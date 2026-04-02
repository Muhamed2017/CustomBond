package com.custombond.dto.request.ContactData;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class Stakeholder {

    @NotBlank
    private String stakeholderName;

    @NotNull
    private SectorType sectorType;

    // identifiers
    private String taxId;


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

    public enum SectorType {
        PRIVATE,
        PUBLIC
    }
}
