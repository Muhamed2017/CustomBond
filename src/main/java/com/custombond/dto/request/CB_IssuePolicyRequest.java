package com.custombond.dto.request;

import com.custombond.dto.request.ContactData.Stakeholder;
import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CB_IssuePolicyRequest {


    @NotNull(message = "Stakeholder information is required")
    private Stakeholder stakeholder;


    @NotNull(message = "Effective date is required")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate effectiveDate;

    @NotNull(message = "Expiry date is required")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate expiryDate;

    @NotNull(message = "Currency is required")
    private Integer currency;

    @NotNull(message = "Division is required")
    private Long division;

    @NotNull(message = "Sub-division is required")
    private Long subDivision;

    @NotNull(message = "Insured is required")
    private Long insured;

    @NotNull(message = "Underwriter contact key is required")
    private Long underWriterContactKey;

    @NotNull(message = "MOP is required")
    private Integer mop;

    @NotNull(message = "Producer is required")
    private Integer producer;

    @NotNull(message = "Product is required")
    private Long product;

    @NotNull(message = "Hazard level is required")
    private Integer hazardLevel;

    @NotNull(message = "Grace period days is required")
    @Min(value = 0, message = "Grace period days must be non-negative")
    private Integer gracePeriodDays;

    @NotNull(message = "Market source key is required")
    private Integer marketSourceKey;

    @NotNull(message = "usePercentageInBeneficiary flag is required")
    private Boolean usePerecntageInBeneficiary;

    @NotNull(message = "Beneficiaries list is required")
    @Valid
    private List<DXCQuotePreparationBeneficiaryDTO> benficiaries;

    @NotNull(message = "Additional insureds list is required")
    @Valid
    private List<DXCQuotePreparationAdditionalInsuredDTO> additionalInsureds;
}

