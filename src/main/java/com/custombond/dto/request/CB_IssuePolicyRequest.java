package com.custombond.dto.request;

import com.custombond.dto.request.BlackListRequest.CallSequence;
import com.custombond.dto.request.ContactData.Stakeholder;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CB_IssuePolicyRequest {

    @NotNull(message = "Stakeholder information is required")
    private Stakeholder stakeholder;

    @NotNull
    private CallSequence callSequance; // نفس الاسم اللي في الـ JSON
    
    @NotBlank
    private String declarationNo;

    @NotBlank
    private String invoiceNo;

    @NotBlank
    private String sendingCustomsCode;

    @NotBlank
    private String sendingCustomsName;

    @NotBlank
    private String receivingCustomsCode;

    @NotBlank
    private String receivingCustomsName;

    @NotBlank
    private String goodsDescription;

    @NotNull
    private GoodsType goodsType;

    @NotNull
    private Double amanCoverageAmount;

    @NotNull
    private Double totalWeight;

    @NotBlank
    private String weightUnit;

    @NotNull
    private Integer packageCount;

    @NotBlank
    private String packageUnit;

    public enum GoodsType {
        NORMAL,
        HAZARDOUS
    }

    private String vendorRequestId;
    private String vendorId;
    @NotNull
    String nafezaInsuranceRequestId;

}
