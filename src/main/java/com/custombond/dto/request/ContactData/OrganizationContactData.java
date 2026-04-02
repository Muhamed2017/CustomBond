package com.custombond.dto.request.ContactData;



import lombok.Data;
import java.time.LocalDate;
import java.util.List;

@Data
public class OrganizationContactData {

    // required
    private String companyName;

    private String taxCardId;

    private String commercialRegID;

    private Integer typeOfTax;

    private Integer sector;

    private Integer country;

    private List<RoleDTO> roles;

    // optional
    private String mobilePhone;

    private String email;

    private String address1;

    private String address2;

    private String city;

    private Integer state;

    private String drivingLicenseNumber;

    private LocalDate niprExpirationDate;

    private String niprNumber;

    @Data
    public static class RoleDTO {
        private Integer role;
    }
}