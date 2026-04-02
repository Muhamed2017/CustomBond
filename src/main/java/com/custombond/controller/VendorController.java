package com.custombond.controller;

import com.custombond.dto.request.VendorIssuanceRequest;
import com.custombond.dto.response.VendorIssuanceAcknowledgement;
import com.custombond.service.VendorPipelineAsyncService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Controller exposing the vendor-facing issuance pipeline API.
 *
 * <h2>Endpoint</h2>
 * {@code POST /vendor/issue}  (consumes {@code multipart/form-data})
 *
 * <h2>Flow</h2>
 * <ol>
 *   <li>Receive the vendor's request (JSON {@code data} part + optional {@code file} part).</li>
 *   <li>Generate a UUID {@code parentRequestId} as the correlation identifier.</li>
 *   <li>Read the file bytes <em>synchronously</em> (before the request scope closes).</li>
 *   <li>Hand off to {@link VendorPipelineAsyncService#runPipeline} which executes the
 *       configured steps on the {@code externalApiExecutor} thread pool.</li>
 *   <li>Return HTTP {@code 202 Accepted} with a {@link VendorIssuanceAcknowledgement}
 *       containing the {@code parentRequestId} before the pipeline has started.</li>
 * </ol>
 *
 * <h2>Result delivery</h2>
 * The final {@link com.custombond.dto.response.VendorPipelineResult} is:
 * <ul>
 *   <li>Always logged by the async service (structured log with level INFO/ERROR).</li>
 *   <li>POSTed to {@code VendorIssuanceRequest.callbackUrl} if one was provided.</li>
 * </ul>
 *
 * <h2>Multipart request format</h2>
 * <pre>
 * Part name : "data"   – JSON body of {@link VendorIssuanceRequest}
 * Part name : "file"   – document binary (optional; required only when UPLOAD_DOCUMENT is in steps)
 * </pre>
 *
 * <h2>Example curl</h2>
 * <pre>
 * curl -X POST http://localhost:8075/api/vendor/issue \
 *   -F 'data={"vendorId":"V001","steps":["CHECK_BLACK_LIST","QUOTE_PREPARATION","ISSUE_QUOTE","ISSUE_POLICY"],
 *             "blackList":{...},"quotePreparation":{...}};type=application/json' \
 *   -F 'file=@contract.pdf'
 * </pre>
 */
@Slf4j
@RestController
@RequestMapping("/vendor")
@RequiredArgsConstructor
@Tag(name = "Vendor Pipeline API", description = "Single entry-point for vendor-initiated issuance flows")
public class VendorController {

    private final VendorPipelineAsyncService vendorPipelineAsyncService;

    /**
     * Accepts a vendor issuance request, returns an immediate acknowledgement, and
     * processes the configured pipeline steps asynchronously.
     *
     * @param request the vendor issuance request (JSON, sent as the {@code data} part)
     * @param file    optional document file (sent as the {@code file} part);
     *                must be present when {@code UPLOAD_DOCUMENT} is in the steps list
     * @return HTTP 202 with a {@link VendorIssuanceAcknowledgement} containing the correlation ID
     */
    @PostMapping(value = "/issue", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(
            summary = "Submit a vendor issuance request",
            description = "Validates the request synchronously and immediately acknowledges receipt (HTTP 202). "
                    + "The configured pipeline steps are then executed asynchronously. "
                    + "The vendor receives the final outcome via the optional callbackUrl "
                    + "or can correlate log entries using the returned requestId.")
    @ApiResponses({
            @ApiResponse(responseCode = "202",
                    description = "Request accepted – pipeline will execute asynchronously",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = VendorIssuanceAcknowledgement.class))),
            @ApiResponse(responseCode = "400",
                    description = "Validation error in the request payload or unreadable file"),
            @ApiResponse(responseCode = "500",
                    description = "Unexpected server error before pipeline could be started")
    })
    public ResponseEntity<VendorIssuanceAcknowledgement> issue(
            @RequestPart("data") @Valid VendorIssuanceRequest request,
            @RequestPart(value = "file", required = false) MultipartFile file) {

        // Generate correlation ID for the entire pipeline run
        String parentRequestId = UUID.randomUUID().toString();

        log.info("[VendorController] Received issuance request – parentRequestId={}, vendorId={}, steps={}",
                parentRequestId, request.getVendorId(), request.getSteps());

        // Read file bytes synchronously while the multipart request is still available.
        // The async service will run after the HTTP request scope has closed, so file
        // bytes must be extracted here and stored in memory.
        byte[] fileBytes = null;
        String fileName = null;
        String fileContentType = null;

        if (file != null && !file.isEmpty()) {
            try {
                fileBytes = file.getBytes();
                fileName = file.getOriginalFilename();
                fileContentType = file.getContentType();
                log.debug("[VendorController] Received file '{}' ({} bytes, type={})",
                        fileName, fileBytes.length, fileContentType);
            } catch (IOException e) {
                log.error("[VendorController] Failed to read uploaded file – parentRequestId={}: {}",
                        parentRequestId, e.getMessage(), e);
                return ResponseEntity.badRequest().build();
            }
        }

        // Trigger async pipeline – returns immediately
        vendorPipelineAsyncService.runPipeline(
                request, parentRequestId, fileBytes, fileName, fileContentType);

        // Build and return the immediate acknowledgement
        VendorIssuanceAcknowledgement acknowledgement = VendorIssuanceAcknowledgement.builder()
                .requestId(parentRequestId)
                .vendorRequestId(request.getVendorRequestId())
                .message("Request accepted. Processing pipeline asynchronously.")
                .acceptedAt(LocalDateTime.now())
                .steps(request.getSteps())
                .build();

        log.info("[VendorController] Acknowledged – parentRequestId={}", parentRequestId);

        return ResponseEntity.accepted().body(acknowledgement);
    }
}
