package com.custombond.dto.response;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DXCQuotePreparationResponse {

    private Long policyKey;
    private String policyNo;
}

