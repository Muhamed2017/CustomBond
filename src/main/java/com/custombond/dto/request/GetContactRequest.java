package com.custombond.dto.request;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class GetContactRequest {

    @NotNull
    private CallSequence callSequance; // نفس الاسم اللي في الـ JSON

    @NotNull
    private String vendorId;

    // identifiers
    private String taxId;

    @Pattern(regexp = "^\\d{14}$", message = "nationalId must be 14 digits")
    private String nationalId;

    private Integer contactKey;

    private String vendorRequestId;

    // enums
    public enum CallSequence {
        FIRST_CALL,
        SECOND_CALL
    }

    public static GetContactRequest mapToGetContact(BlackListRequest req) {
        if (req == null)
            return null;

        GetContactRequest result = new GetContactRequest();

        result.setCallSequance(
                GetContactRequest.CallSequence.valueOf(req.getCallSequance().name()));

        result.setVendorId(req.getVendorId());

        result.setTaxId(req.getTaxId());
        result.setNationalId(req.getNationalId());
        result.setContactKey(req.getContactKey());

        return result;
    }

}