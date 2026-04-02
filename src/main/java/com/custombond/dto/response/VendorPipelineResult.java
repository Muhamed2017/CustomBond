package com.custombond.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Final outcome of an asynchronous vendor issuance pipeline run.
 *
 * <p>This payload is:
 * <ul>
 *   <li>Logged by {@link com.custombond.service.VendorPipelineAsyncService} (always).</li>
 *   <li>Posted to the vendor's {@code callbackUrl} as an HTTP POST body, if one was supplied.</li>
 * </ul>
 *
 * <p>Fields irrelevant to the outcome (e.g. {@code failedStep} on success) are omitted
 * from serialisation via {@code @JsonInclude(NON_NULL)}.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Final outcome of the vendor issuance pipeline (delivered via callback or logged)")
public class VendorPipelineResult {

    /** The correlation ID that was returned in the initial {@link VendorIssuanceAcknowledgement}. */
    @Schema(description = "Correlation ID matching the initial acknowledgement requestId")
    private String requestId;

    /** The vendor identifier from the original request. */
    @Schema(description = "Vendor ID from the original request")
    private String vendorId;

    /** {@code true} if all configured steps completed without error. */
    @Schema(description = "true if all steps succeeded, false otherwise")
    private boolean success;

    /** Issued policy number on success; {@code null} on failure. */
    @Schema(description = "Policy number – populated on successful issuance", example = "POL-2024-00001")
    private String policyNo;

    /**
     * Name of the step that caused the pipeline to abort.
     * {@code null} on success.
     */
    @Schema(description = "Name of the step that failed – null on success")
    private String failedStep;

    /**
     * Human-readable description of why the pipeline failed.
     * {@code null} on success.
     */
    @Schema(description = "Failure reason – null on success")
    private String failureMessage;

    /** UTC timestamp when the pipeline finished (success or failure). */
    @Schema(description = "UTC timestamp when the pipeline completed")
    private LocalDateTime completedAt;

    // -----------------------------------------------------------------------
    // Factory methods
    // -----------------------------------------------------------------------

    /**
     * Creates a successful result.
     *
     * @param requestId correlation ID from the acknowledgement
     * @param vendorId  the vendor's identifier
     * @param policyNo  the issued policy number
     * @return populated success result
     */
    public static VendorPipelineResult success(String requestId, String vendorId, String policyNo) {
        return VendorPipelineResult.builder()
                .requestId(requestId)
                .vendorId(vendorId)
                .success(true)
                .policyNo(policyNo)
                .completedAt(LocalDateTime.now())
                .build();
    }

    /**
     * Creates a failure result.
     *
     * @param requestId      correlation ID from the acknowledgement
     * @param vendorId       the vendor's identifier
     * @param failedStep     display name of the step that aborted the pipeline
     * @param failureMessage human-readable reason for failure
     * @return populated failure result
     */
    public static VendorPipelineResult failure(String requestId, String vendorId,
                                               String failedStep, String failureMessage) {
        return VendorPipelineResult.builder()
                .requestId(requestId)
                .vendorId(vendorId)
                .success(false)
                .failedStep(failedStep)
                .failureMessage(failureMessage)
                .completedAt(LocalDateTime.now())
                .build();
    }
}
