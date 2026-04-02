package com.custombond.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

/**
 * Vendor-supplied input for the {@code CHECK_BLACK_LIST} pipeline step.
 *
 * <p>This is a focused subset of {@link BlackListRequest}: fields that are
 * automatically injected by the pipeline ({@code callSequance}, {@code vendorId},
 * {@code vendorRequestId}) are intentionally absent here so the vendor doesn't
 * need to repeat top-level identifiers inside a nested block.
 *
 * <p>At least one of {@code taxId}, {@code nationalId}, or {@code contactKey}
 * must be present; the DXC service validates this constraint.
 */
@Data
public class VendorBlackListData {

    /** Full legal name of the stakeholder to check. */
    @NotBlank(message = "stakeholderName is required for the black-list check")
    private String stakeholderName;

    /** Whether the stakeholder is a private or public sector entity. */
    @NotNull(message = "sectorType is required for the black-list check")
    private SectorType sectorType;

    // -----------------------------------------------------------------------
    // At least one identifier must be provided
    // -----------------------------------------------------------------------

    /**
     * Tax registration number.
     * Mutually usable with {@code nationalId} and {@code contactKey}.
     */
    private String taxId;

    /**
     * National ID (14 digits exactly).
     * Mutually usable with {@code taxId} and {@code contactKey}.
     */
    @Pattern(regexp = "^\\d{14}$", message = "nationalId must be exactly 14 digits")
    private String nationalId;

    /**
     * DXC contact key.
     * Mutually usable with {@code taxId} and {@code nationalId}.
     */
    private Integer contactKey;

    // -----------------------------------------------------------------------
    // Optional enrichment fields
    // -----------------------------------------------------------------------

    private String commercialRegNo;
    private String address;

    @Pattern(regexp = "^\\+?[1-9]\\d{1,14}$", message = "phone must be in E.164 format")
    private String phone;

    @Email(message = "email must be a valid address")
    private String email;

    private String legalRepName;

    @Pattern(regexp = "^\\d{14}$", message = "legalRepNationalId must be exactly 14 digits")
    private String legalRepNationalId;

    // -----------------------------------------------------------------------
    // Nested enum
    // -----------------------------------------------------------------------

    /** Mirrors {@link BlackListRequest.SectorType}. */
    public enum SectorType {
        PRIVATE,
        PUBLIC
    }
}
