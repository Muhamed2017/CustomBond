package com.custombond.dto.request;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

/**
 * Quote preparation data for the NAFEZA issuance flow.
 *
 * <p>This is structurally identical to {@link DXCQuotePreparationRequest} except that the
 * {@code insured} field is <strong>absent</strong>. The {@code insured} (DXC contact key)
 * is resolved at runtime by the {@code NafezaQuotePreparationStep} from the contact key
 * returned by the preceding {@code CHECK_BLACK_LIST} step, eliminating the need for
 * vendors to look up the contact key themselves before calling the API.
 *
 * @see DXCQuotePreparationRequest
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Quote preparation data for NAFEZA flows – 'insured' is resolved automatically from the black-list step")
public class NafezaQuotePreparationData {

    @NotNull(message = "effectiveDate is required")
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Schema(description = "Policy effective date", example = "2024-01-01")
    private LocalDate effectiveDate;

    @NotNull(message = "expiryDate is required")
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Schema(description = "Policy expiry date", example = "2025-01-01")
    private LocalDate expiryDate;

    @NotNull(message = "currency is required")
    @Schema(description = "Currency code", example = "1")
    private Integer currency;

    @NotNull(message = "division is required")
    @Schema(description = "Division key", example = "100")
    private Long division;

    @NotNull(message = "subDivision is required")
    @Schema(description = "Sub-division key", example = "200")
    private Long subDivision;

    /**
     * NOTE: {@code insured} is intentionally absent here.
     * It is resolved from {@code BlackListResponse.contactKey} by the
     * {@code NafezaQuotePreparationStep} and injected into the
     * {@link DXCQuotePreparationRequest} at runtime.
     */

    @NotNull(message = "underWriterContactKey is required")
    @Schema(description = "Underwriter contact key", example = "500")
    private Long underWriterContactKey;

    @NotNull(message = "mop is required")
    @Schema(description = "Method of payment", example = "1")
    private Integer mop;

    @NotNull(message = "producer is required")
    @Schema(description = "Producer key", example = "10")
    private Integer producer;

    @NotNull(message = "maximumExposure is required")
    @Schema(description = "Maximum exposure amount", example = "1000000")
    private Long maximumExposure;

    @NotNull(message = "product is required")
    @Schema(description = "Product key", example = "300")
    private Long product;

    @NotNull(message = "hazardLevel is required")
    @Schema(description = "Hazard level", example = "1")
    private Integer hazardLevel;

    @NotNull(message = "policyName is required")
    @Schema(description = "Policy name code", example = "1")
    private Integer policyName;

    @NotNull(message = "contactTPA is required")
    @Schema(description = "Contact TPA key", example = "1")
    private Integer contactTPA;

    @NotNull(message = "gracePeriodDays is required")
    @Min(value = 0, message = "gracePeriodDays must be non-negative")
    @Schema(description = "Grace period in days", example = "30")
    private Integer gracePeriodDays;

    @NotNull(message = "marketSourceKey is required")
    @Schema(description = "Market source key", example = "5")
    private Integer marketSourceKey;

    @NotNull(message = "usePerecntageInBeneficiary is required")
    @Schema(description = "Whether beneficiary allocations are expressed as percentages")
    private Boolean usePerecntageInBeneficiary;

    @NotNull(message = "benficiaries list is required")
    @Valid
    @Schema(description = "List of beneficiaries")
    private List<DXCQuotePreparationBeneficiaryDTO> benficiaries;

    @NotNull(message = "additionalInsureds list is required")
    @Valid
    @Schema(description = "List of additional insureds")
    private List<DXCQuotePreparationAdditionalInsuredDTO> additionalInsureds;
}
