package com.custombond.dto.request;

import com.fasterxml.jackson.annotation.JsonAlias;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CancelPolicyRequest {
    @NotNull(message = "policyNo is required")
    private String policyNo;
    @NotNull(message = "parentRequestId is required")
    private String parentRequestId;
    @JsonAlias({"nafezaInsuranceRequestId"})
    private String vendorRequestId;
    
    private String vendorId;

}
