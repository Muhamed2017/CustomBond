package com.custombond.pipeline.steps;

import com.custombond.dto.request.NafezaIssuanceRequest;
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
 * NAFEZA variant of the document-upload pipeline step.
 *
 * <h2>Difference from {@link UploadDocumentStep}</h2>
 * This step reads the upload metadata ({@link VendorUploadData}) from
 * {@code context.getNafezaRequest().getUpload()} rather than from
 * {@code context.getRequest().getUpload()}, and derives the vendor correlation
 * identifiers from the {@link NafezaIssuanceRequest}.
 * All other logic (instnceKey resolution, file wrapping, DXC call) is identical.
 *
 * <h2>instnceKey resolution</h2>
 * <ol>
 *   <li>{@link VendorUploadData#getInstnceKey()} – explicit value from the vendor.</li>
 *   <li>{@code context.getPolicyKey()} – set by a preceding
 *       {@link PipelineStepType#NAFEZA_QUOTE_PREPARATION} step.</li>
 * </ol>
 *
 * <h2>Abort conditions</h2>
 * Throws {@link PipelineStepException} when:
 * <ul>
 *   <li>{@code nafezaRequest} is not set in the context.</li>
 *   <li>The {@code upload} metadata block is missing from the NAFEZA request.</li>
 *   <li>No file bytes are present in the context.</li>
 *   <li>{@code instnceKey} cannot be resolved from any source.</li>
 *   <li>The DXC API returns a non-2xx HTTP status.</li>
 * </ul>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NafezaUploadDocumentsStep implements PipelineStep {

    private final DXC_UploadDocument_Service uploadDocumentService;
    private final ObjectMapper objectMapper;

    @Override
    public PipelineStepType getType() {
        return PipelineStepType.NAFEZA_UPLOAD_DOCUMENTS;
    }

    @Override
    public String getName() {
        return "NafezaUploadDocuments";
    }

    @Override
    public void execute(IssuancePipelineContext context) throws PipelineStepException {
        NafezaIssuanceRequest nafezaRequest = context.getNafezaRequest();
        if (nafezaRequest == null) {
            throw new PipelineStepException(
                    getName() + ": nafezaRequest is not set in context – "
                            + "use the /vendor/issue/nafeza endpoint for NAFEZA flows");
        }

        VendorUploadData uploadData = nafezaRequest.getUpload();
        if (uploadData == null) {
            throw new PipelineStepException(
                    "Missing required 'upload' input data for step " + getName());
        }

        byte[] fileBytes = context.getDocumentBytes();
        if (fileBytes == null || fileBytes.length == 0) {
            throw new PipelineStepException(
                    getName() + ": no file bytes found in context – "
                            + "ensure a file is attached when NAFEZA_UPLOAD_DOCUMENTS is in the pipeline");
        }

        BigInteger instnceKey = resolveInstnceKey(uploadData, context);

        UploadRequest uploadRequest = new UploadRequest();
        uploadRequest.setEntityKey(uploadData.getEntityKey());
        uploadRequest.setInstnceKey(instnceKey);
        uploadRequest.setDocumentType(uploadData.getDocumentType());
        uploadRequest.setVendorId(nafezaRequest.getVendorId());
        uploadRequest.setVendorRequestId(nafezaRequest.getVendorRequestId());
        uploadRequest.setParentRequestId(context.getParentRequestId());

        String uploadRequestJson;
        try {
            uploadRequestJson = objectMapper.writeValueAsString(uploadRequest);
        } catch (JsonProcessingException e) {
            throw new PipelineStepException(getName() + ": failed to serialise UploadRequest to JSON", e);
        }

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
                        + "or include NAFEZA_QUOTE_PREPARATION before NAFEZA_UPLOAD_DOCUMENTS");
    }
}
