package com.custombond.dto.request;

import com.custombond.dto.request.ContactData.Stakeholder;
import com.fasterxml.jackson.annotation.JsonAlias;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class BlackListRequest {

    @NotNull
    private CallSequence callSequance; // نفس الاسم اللي في الـ JSON

    @NotBlank
    private String stakeholderName;

    @NotNull
    private SectorType sectorType;

    @NotNull
    private String vendorId;

    // identifiers
    private String taxId;

    @Pattern(regexp = "^\\d{14}$", message = "nationalId must be 14 digits")
    private String nationalId;

    private Integer contactKey;

    // optional fields
    private String commercialRegNo;

    private String address;

    @Pattern(regexp = "^\\+?[1-9]\\d{1,14}$", message = "phone must be E.164 format")
    private String phone;

    @Email
    private String email;

    private String legalRepName;

    @Pattern(regexp = "^\\d{14}$", message = "legalRepNationalId must be 14 digits")
    private String legalRepNationalId;

    @JsonAlias({"nafezaInsuranceRequestId"})
    private String vendorRequestId;

    // enums
    public enum CallSequence {
        FIRST_CALL,
        SECOND_CALL
    }

    public enum SectorType {
        PRIVATE,
        PUBLIC
    }

    public static BlackListRequest toEntity(CB_IssuePolicyRequest policyRequest,String vendorId) {
        Stakeholder stakeholder = policyRequest.getStakeholder();
        BlackListRequest entity = new BlackListRequest();
        entity.setCallSequance(BlackListRequest.CallSequence.valueOf(policyRequest.getCallSequance().name()));
        entity.setStakeholderName(stakeholder.getStakeholderName());
        entity.setSectorType(BlackListRequest.SectorType.valueOf(stakeholder.getSectorType().name()));
        entity.setVendorId(vendorId);
        entity.setTaxId(stakeholder.getTaxId());
        entity.setNationalId(stakeholder.getLegalRepNationalId());
        entity.setContactKey(stakeholder.getContactKey());
        entity.setCommercialRegNo(stakeholder.getCommercialRegNo());
        entity.setAddress(stakeholder.getAddress());
        entity.setPhone(stakeholder.getPhone());
        entity.setEmail(stakeholder.getEmail());
        entity.setLegalRepName(stakeholder.getLegalRepName());
        entity.setLegalRepNationalId(stakeholder.getLegalRepNationalId());
        entity.setVendorRequestId(policyRequest.getNafezaInsuranceRequestId());
        return entity;
    }
}