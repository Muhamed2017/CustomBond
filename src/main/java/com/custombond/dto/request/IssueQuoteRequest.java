package com.custombond.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IssueQuoteRequest {
    @NotNull(message = "policyNo is required")
    private String policyNo;
    @NotNull(message = "parentRequestId is required")
    private String parentRequestId;
    @NotNull(message = "vendorId is required")
    private String vendorId;
    
    private String vendorRequestId;
}
