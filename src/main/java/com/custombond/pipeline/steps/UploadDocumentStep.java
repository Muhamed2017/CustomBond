package com.custombond.pipeline.steps;

import com.custombond.dto.request.UploadRequest;
import com.custombond.dto.request.VendorUploadData;
import com.custombond.pipeline.IssuancePipelineContext;
import com.custombond.pipeline.PipelineStep;
import com.custombond.pipeline.PipelineStepException;
import com.custombond.pipeline.PipelineStepType;
import com.custombond.service.DXC_UploadDocument_Service;
import com.custombond.util.BytesMultipartFile;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigInteger;

/**
 * Pipeline step that uploads a vendor-supplied document to the DXC Document service.
 *
 * <h2>Input</h2>
 * <ul>
 *   <li>{@code context.getDocumentBytes()} – raw file bytes (must not be null/empty).</li>
 *   <li>{@code context.getRequest().getUpload()} – upload metadata ({@link VendorUploadData}).</li>
 * </ul>
 *
 * <h2>instnceKey resolution</h2>
 * The DXC upload API requires an {@code InstnceKey} (the policy instance key).
 * This step resolves it in priority order:
 * <ol>
 *   <li>{@link VendorUploadData#getInstnceKey()} – explicit value supplied by the vendor.</li>
 *   <li>{@code context.getPolicyKey()} – set by a preceding {@code QuotePreparationStep}.</li>
 * </ol>
 * If neither source provides a value, the step throws {@link PipelineStepException}.
 *
 * <h2>parentRequestId</h2>
 * Automatically sourced from {@link IssuancePipelineContext#getParentRequestId()}.
 *
 * <h2>Abort conditions</h2>
 * Throws {@link PipelineStepException} when:
 * <ul>
 *   <li>The {@code upload} metadata block is missing.</li>
 *   <li>No file bytes are present in the context.</li>
 *   <li>{@code instnceKey} cannot be resolved from any source.</li>
 *   <li>The DXC API returns a non-2xx HTTP status.</li>
 * </ul>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UploadDocumentStep implements PipelineStep {

    private final DXC_UploadDocument_Service uploadDocumentService;
    private final ObjectMapper objectMapper;

    @Override
    public PipelineStepType getType() {
        return PipelineStepType.UPLOAD_DOCUMENT;
    }

    @Override
    public String getName() {
        return "UploadDocument";
    }

    /**
     * {@inheritDoc}
     *
     * <p>Reconstructs the {@link UploadRequest} from the vendor's {@link VendorUploadData}
     * plus shared context fields, serialises it to JSON (as required by
     * {@link DXC_UploadDocument_Service#uploadDocument(MultipartFile, String)}),
     * and wraps the raw file bytes in a {@link BytesMultipartFile}.
     */
    @Override
    public void execute(IssuancePipelineContext context) throws PipelineStepException {
        VendorUploadData uploadData = context.getRequest().getUpload();
        if (uploadData == null) {
            throw new PipelineStepException(
                    "Missing required 'upload' input data for step " + getName());
        }

        byte[] fileBytes = context.getDocumentBytes();
        if (fileBytes == null || fileBytes.length == 0) {
            throw new PipelineStepException(
                    getName() + ": no file bytes found in context – "
                            + "ensure a file is attached when UPLOAD_DOCUMENT is in the pipeline");
        }

        // Resolve instnceKey: explicit value wins, otherwise fall back to policyKey from QuotePrep
        BigInteger instnceKey = resolveInstnceKey(uploadData, context);

        // Build UploadRequest for the DXC service
        UploadRequest uploadRequest = new UploadRequest();
        uploadRequest.setEntityKey(uploadData.getEntityKey());
        uploadRequest.setInstnceKey(instnceKey);
        uploadRequest.setDocumentType(uploadData.getDocumentType());
        uploadRequest.setVendorId(context.getRequest().getVendorId());
        uploadRequest.setVendorRequestId(context.getRequest().getVendorRequestId());
        uploadRequest.setParentRequestId(context.getParentRequestId());

        String uploadRequestJson;
        try {
            uploadRequestJson = objectMapper.writeValueAsString(uploadRequest);
        } catch (JsonProcessingException e) {
            throw new PipelineStepException(getName() + ": failed to serialise UploadRequest to JSON", e);
        }

        // Wrap raw bytes as a MultipartFile
        String fileName = context.getDocumentFileName() != null
                ? context.getDocumentFileName()
                : "document";
        String contentType = context.getDocumentContentType() != null
                ? context.getDocumentContentType()
                : "application/octet-stream";

        MultipartFile multipartFile = new BytesMultipartFile(
                "File", fileName, contentType, fileBytes);

        log.debug("[{}] Uploading document '{}' ({} bytes) – instnceKey={}",
                getName(), fileName, fileBytes.length, instnceKey);

        ResponseEntity<?> response;
        try {
            response = uploadDocumentService.uploadDocument(multipartFile, uploadRequestJson);
        } catch (IOException e) {
            throw new PipelineStepException(getName() + ": I/O error during document upload", e);
        }

        if (response == null || !response.getStatusCode().is2xxSuccessful()) {
            String status = response != null ? response.getStatusCode().toString() : "null";
            throw new PipelineStepException(
                    getName() + ": DXC document upload returned non-success status – HTTP " + status);
        }

        log.info("[{}] Document uploaded successfully – HTTP {}", getName(), response.getStatusCode());
    }

    // -----------------------------------------------------------------------
    // Private helpers
    // -----------------------------------------------------------------------

    /**
     * Resolves the {@code instnceKey} for the upload request.
     *
     * @param uploadData vendor-supplied upload metadata
     * @param context    pipeline context (may contain {@code policyKey} from QuotePrep)
     * @return resolved instnceKey
     * @throws PipelineStepException if no value can be determined
     */
    private BigInteger resolveInstnceKey(VendorUploadData uploadData,
                                         IssuancePipelineContext context) throws PipelineStepException {
        if (uploadData.getInstnceKey() != null) {
            return uploadData.getInstnceKey();
        }
        if (context.getPolicyKey() != null) {
            return BigInteger.valueOf(context.getPolicyKey());
        }
        throw new PipelineStepException(
                getName() + ": 'instnceKey' is required – provide it in 'upload.instnceKey' "
                        + "or include QUOTE_PREPARATION before UPLOAD_DOCUMENT");
    }
}
