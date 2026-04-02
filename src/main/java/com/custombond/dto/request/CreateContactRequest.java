package com.custombond.dto.request;

import com.custombond.dto.request.BlackListRequest.CallSequence;
import com.custombond.dto.request.ContactData.IndividualContactData;
import com.custombond.dto.request.ContactData.OrganizationContactData;
import com.fasterxml.jackson.annotation.JsonSetter;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CreateContactRequest {
    public IndividualContactData individualContactData;
    public OrganizationContactData organizationContactData;
    @NotNull
    private String vendorId;
    @NotNull
    private CallSequence callSequance; // نفس الاسم اللي في الـ JSON

    @NotNull(message = "vendorRequestId is required")
    private String vendorRequestId;

    private String NafazaRequestId;

    private String parentRequestId;

    @JsonSetter("NafazaRequestId")
    public void setNRequestId(String NafazaRequestId) {
        this.NafazaRequestId = NafazaRequestId;

        if (NafazaRequestId != null && !NafazaRequestId.trim().isEmpty()) {
            this.vendorRequestId = NafazaRequestId;
        }
    }

}