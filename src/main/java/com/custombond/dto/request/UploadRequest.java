package com.custombond.dto.request;

import java.math.BigInteger;

import lombok.Data;

@Data
public class UploadRequest {
    private Integer entityKey;
    private BigInteger instnceKey;
    private Integer documentType;
    private String vendorRequestId;
    private String vendorId;
    private String parentRequestId;
    
}