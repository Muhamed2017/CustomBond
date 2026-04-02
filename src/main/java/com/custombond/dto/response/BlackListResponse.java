package com.custombond.dto.response;

import java.time.LocalDateTime;

import com.custombond.entity.Enums;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import lombok.Data;

@Data
@JsonPropertyOrder({
        "taxId",
        "id",
        "contactKey",
        "status",
        "stakeholderName",
        "maskedStakeholderName",
        "isExisted",
        "checkedAt",
        "cachedResultTtlSeconds",
        "rejectionCode",
        "rejectionReasonAr",
        "errorMessage"
})
public class BlackListResponse {

    private Enums.Status status;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String taxId;

    private Object id;

    private String stakeholderName;

    private String maskedStakeholderName;

    private boolean isExisted;

    private LocalDateTime checkedAt;

    private Integer cachedResultTtlSeconds;

    private String rejectionCode;

    private String rejectionReasonAr;

    private String errorMessage;
    
    @JsonIgnore
    private Integer contactKey;

}