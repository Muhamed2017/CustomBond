package com.custombond.dto.request;


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
public class DXCQuotePreparationAdditionalInsuredDTO {

    @NotNull(message = "Additional insured contact ID is required")
    private Long contactId;

    @NotNull(message = "Additional insured limit is required")
    @Min(value = 0, message = "Additional insured limit must be non-negative")
    private Long limit;

    @NotNull(message = "Additional insured currency is required")
    private Integer currency;
}