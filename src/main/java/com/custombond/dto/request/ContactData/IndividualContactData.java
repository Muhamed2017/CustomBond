package com.custombond.dto.request.ContactData;

import java.util.List;

import lombok.Data;

@Data
public class IndividualContactData {

    private String surname;

    private String forename;

    private Integer gender;

    private Integer title;

    private Integer maritalStatus;

    private Integer jobTitle;

    private String nationalID;

    private Integer typeOfTax;

    private Integer sector;

    private String mobilePhone;

    private String email;

    private String address1;

    private String address2;

    private String city;

    private Integer state;

    private Integer country;

    private String drivingLicenseNumber;

    private List<RoleDTO> roles;

    @Data
    public static class RoleDTO {
        private Integer role;
    }
}
