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
 * Top-level request DTO for the NAFEZA issuance pipeline endpoint
 * ({@code POST /vendor/issue/nafeza}).
 *
 * <h2>Difference from {@link VendorIssuanceRequest}</h2>
 * The standard vendor endpoint requires a fully pre-built
 * {@link DXCQuotePreparationRequest} (including the {@code insured} field).
 * In the NAFEZA flow the {@code insured} (DXC contact key) is resolved
 * automatically from the contact key returned by the preceding
 * {@link PipelineStepType#CHECK_BLACK_LIST} step, so vendors supply a
 * {@link NafezaQuotePreparationData} block instead (which omits {@code insured}).
 *
 * <h2>Typical NAFEZA step sequence</h2>
 * <pre>
 * ["CHECK_BLACK_LIST", "NAFEZA_QUOTE_PREPARATION", "NAFEZA_UPLOAD_DOCUMENTS",
 *  "ISSUE_QUOTE", "ISSUE_POLICY"]
 * </pre>
 *
 * <h2>Multipart request format</h2>
 * <pre>
 * Part name : "data"   – JSON body of {@link NafezaIssuanceRequest}
 * Part name : "file"   – document binary (required when NAFEZA_UPLOAD_DOCUMENTS is in steps)
 * </pre>
 *
 * <h2>Example – full NAFEZA flow</h2>
 * <pre>
 * {
 *   "vendorId": "V001",
 *   "vendorRequestId": "NAFEZA-2024-001",
 *   "callbackUrl": "https://vendor.example.com/webhook/result",
 *   "steps": ["CHECK_BLACK_LIST","NAFEZA_QUOTE_PREPARATION","NAFEZA_UPLOAD_DOCUMENTS","ISSUE_QUOTE","ISSUE_POLICY"],
 *   "blackList": { "stakeholderName": "Acme Corp", "sectorType": "PRIVATE", "taxId": "123456789" },
 *   "nafezaQuoteData": {
 *     "effectiveDate": "2024-01-01", "expiryDate": "2025-01-01",
 *     "currency": 1, "division": 100, "subDivision": 200,
 *     ...
 *   },
 *   "upload": { "entityKey": 10, "documentType": 3 }
 * }
 * </pre>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Top-level request for the NAFEZA vendor issuance pipeline")
public class NafezaIssuanceRequest {

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
    @Schema(description = "Vendor's own request reference (optional but recommended)", example = "NAFEZA-2024-001")
    private String vendorRequestId;

    /**
     * Optional webhook URL.
     * When provided, the pipeline posts the pipeline result (success or failure)
     * to this URL as an HTTP POST with a JSON body once all steps complete.
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
     * Typical NAFEZA sequence:
     * {@code CHECK_BLACK_LIST → NAFEZA_QUOTE_PREPARATION → NAFEZA_UPLOAD_DOCUMENTS →
     * ISSUE_QUOTE → ISSUE_POLICY}.
     */
    @NotEmpty(message = "steps list must not be empty")
    @Schema(description = "Ordered list of pipeline steps",
            example = "[\"CHECK_BLACK_LIST\",\"NAFEZA_QUOTE_PREPARATION\",\"NAFEZA_UPLOAD_DOCUMENTS\",\"ISSUE_QUOTE\",\"ISSUE_POLICY\"]")
    private List<PipelineStepType> steps;

    // -----------------------------------------------------------------------
    // Per-step input blocks
    // -----------------------------------------------------------------------

    /**
     * Input data for the {@link PipelineStepType#CHECK_BLACK_LIST} step.
     * Required when {@code CHECK_BLACK_LIST} is in {@link #steps}.
     */
    @Valid
    @Schema(description = "Input data for CHECK_BLACK_LIST step")
    private VendorBlackListData blackList;

    /**
     * Input data for the {@link PipelineStepType#NAFEZA_QUOTE_PREPARATION} step.
     * Required when {@code NAFEZA_QUOTE_PREPARATION} is in {@link #steps}.
     *
     * <p>The {@code insured} field is intentionally absent – it is resolved at runtime
     * from the contact key returned by the {@code CHECK_BLACK_LIST} step.
     */
    @Valid
    @Schema(description = "Input data for NAFEZA_QUOTE_PREPARATION step (insured resolved from black-list)")
    private NafezaQuotePreparationData nafezaQuoteData;

    /**
     * Metadata for the {@link PipelineStepType#NAFEZA_UPLOAD_DOCUMENTS} step.
     * Required when {@code NAFEZA_UPLOAD_DOCUMENTS} is in {@link #steps}.
     * The actual file bytes are supplied as the {@code file} multipart part.
     */
    @Valid
    @Schema(description = "Metadata for NAFEZA_UPLOAD_DOCUMENTS step (file is sent as the 'file' multipart part)")
    private VendorUploadData upload;

    /**
     * Pre-known policy number.
     *
     * <p>Used as a fallback for {@code ISSUE_QUOTE} and {@code ISSUE_POLICY} steps
     * when {@code NAFEZA_QUOTE_PREPARATION} is <em>not</em> included in the pipeline.
     * When {@code NAFEZA_QUOTE_PREPARATION} runs first, its output supersedes this value.
     */
    @Schema(description = "Policy number – required for ISSUE_QUOTE / ISSUE_POLICY when NAFEZA_QUOTE_PREPARATION is not in the pipeline")
    private String policyNo;
}
