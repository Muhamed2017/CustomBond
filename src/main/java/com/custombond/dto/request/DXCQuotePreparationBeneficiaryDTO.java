package com.custombond.dto.request;


import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DXCQuotePreparationBeneficiaryDTO {

    @NotNull(message = "Beneficiary contact ID is required")
    private Long contactId;

    @NotNull(message = "Beneficiary limit is required")
    @Min(value = 0, message = "Beneficiary limit must be non-negative")
    private Long limit;

    @NotNull(message = "Beneficiary currency is required")
    private Integer currency;

    @NotNull(message = "Share percentage is required")
    @Min(value = 0, message = "Share percentage must be between 0 and 100")
    @Max(value = 100, message = "Share percentage must be between 0 and 100")
    private Integer sharePercentage;

    @NotNull(message = "Print flag is required")
    private Boolean print;
}
