package com.custombond.dto.request;

import com.custombond.pipeline.PipelineStepType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Top-level request DTO for the {@code POST /vendor/issue} endpoint.
 *
 * <p>Vendors submit this as the {@code data} part of a {@code multipart/form-data} request
 * (optionally paired with a {@code file} part when {@code UPLOAD_DOCUMENT} is in the pipeline).
 *
 * <h2>Pipeline configuration</h2>
 * The {@link #steps} list drives the entire execution.  Only the step-data blocks
 * relevant to the selected steps need to be populated:
 *
 * <table border="1" cellpadding="4">
 *   <tr><th>Step</th><th>Required block</th><th>Required file</th></tr>
 *   <tr><td>CHECK_BLACK_LIST</td><td>{@code blackList}</td><td>No</td></tr>
 *   <tr><td>QUOTE_PREPARATION</td><td>{@code quotePreparation}</td><td>No</td></tr>
 *   <tr><td>UPLOAD_DOCUMENT</td><td>{@code upload}</td><td>Yes</td></tr>
 *   <tr><td>ISSUE_QUOTE</td><td>none (uses context)</td><td>No</td></tr>
 *   <tr><td>ISSUE_POLICY</td><td>none (uses context)</td><td>No</td></tr>
 * </table>
 *
 * <p>For {@code ISSUE_QUOTE} / {@code ISSUE_POLICY} without a preceding
 * {@code QUOTE_PREPARATION} step, the vendor must provide {@link #policyNo} directly.
 *
 * <h2>Example – full flow</h2>
 * <pre>
 * {
 *   "vendorId": "V001",
 *   "vendorRequestId": "REQ-2024-001",
 *   "callbackUrl": "https://vendor.example.com/webhook/result",
 *   "steps": ["CHECK_BLACK_LIST","QUOTE_PREPARATION","UPLOAD_DOCUMENT","ISSUE_QUOTE","ISSUE_POLICY"],
 *   "blackList": { "stakeholderName": "Acme Corp", "sectorType": "PRIVATE", "taxId": "123456789" },
 *   "quotePreparation": { ... },
 *   "upload": { "entityKey": 10, "documentType": 3 }
 * }
 * </pre>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Top-level request for the vendor issuance pipeline")
public class VendorIssuanceRequest {

    // -----------------------------------------------------------------------
    // Mandatory top-level identifiers
    // -----------------------------------------------------------------------

    /**
     * Unique identifier for the calling vendor.
     * Propagated to every DXC service call as the {@code vendorId} field.
     */
    @NotBlank(message = "vendorId is required")
    @Schema(description = "Unique identifier of the calling vendor", example = "V001")
    private String vendorId;

    /**
     * Vendor's own request reference number.
     * Stored in all DXC log entries for cross-system tracing.
     * Optional but strongly recommended.
     */
    @Schema(description = "Vendor's own request reference (optional but recommended)", example = "REQ-2024-001")
    private String vendorRequestId;

    /**
     * Optional webhook URL.
     * When provided, the pipeline posts the {@link com.custombond.dto.response.VendorPipelineResult}
     * (success or failure) to this URL as an HTTP POST with a JSON body once all steps complete.
     */
    @Schema(description = "Optional URL to receive the pipeline result as a webhook POST")
    private String callbackUrl;

    // -----------------------------------------------------------------------
    // Pipeline definition
    // -----------------------------------------------------------------------

    /**
     * Ordered list of pipeline steps to execute.
     *
     * <p>Steps are executed strictly in the order listed here.
     * Each vendor may include any subset of the available steps.
     * Duplicate entries are not recommended and will be executed multiple times.
     */
    @NotEmpty(message = "steps list must not be empty")
    @Schema(description = "Ordered list of pipeline steps to execute for this vendor",
            example = "[\"CHECK_BLACK_LIST\",\"QUOTE_PREPARATION\",\"UPLOAD_DOCUMENT\",\"ISSUE_QUOTE\",\"ISSUE_POLICY\"]")
    private List<PipelineStepType> steps;

    // -----------------------------------------------------------------------
    // Per-step input blocks (only the blocks needed for the selected steps
    // must be populated; all others may be null)
    // -----------------------------------------------------------------------

    /**
     * Input data for the {@link PipelineStepType#CHECK_BLACK_LIST} step.
     * Required when {@code CHECK_BLACK_LIST} is included in {@link #steps}.
     */
    @Valid
    @Schema(description = "Input data for CHECK_BLACK_LIST step")
    private VendorBlackListData blackList;

    /**
     * Input data for the {@link PipelineStepType#QUOTE_PREPARATION} step.
     * Required when {@code QUOTE_PREPARATION} is included in {@link #steps}.
     */
    @Valid
    @Schema(description = "Input data for QUOTE_PREPARATION step")
    private DXCQuotePreparationRequest quotePreparation;

    /**
     * Metadata for the {@link PipelineStepType#UPLOAD_DOCUMENT} step.
     * Required when {@code UPLOAD_DOCUMENT} is included in {@link #steps}.
     * The actual file bytes are supplied as the {@code file} multipart part.
     */
    @Valid
    @Schema(description = "Metadata for UPLOAD_DOCUMENT step (file is sent as the 'file' multipart part)")
    private VendorUploadData upload;

    /**
     * Pre-known policy number.
     *
     * <p>Used as a fallback for {@code ISSUE_QUOTE} and {@code ISSUE_POLICY} steps
     * when {@code QUOTE_PREPARATION} is <em>not</em> included in the pipeline.
     * When {@code QUOTE_PREPARATION} runs first, its output supersedes this value.
     */
    @Schema(description = "Policy number – required for ISSUE_QUOTE / ISSUE_POLICY when QUOTE_PREPARATION is not in the pipeline")
    private String policyNo;
}
