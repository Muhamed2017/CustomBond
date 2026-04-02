package com.custombond.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigInteger;

/**
 * Vendor-supplied metadata for the {@code UPLOAD_DOCUMENT} pipeline step.
 *
 * <p>Fields that are automatically provided by the pipeline ({@code vendorId},
 * {@code vendorRequestId}, {@code parentRequestId}) are absent here.
 *
 * <h2>instnceKey resolution</h2>
 * The {@code instnceKey} field is optional when {@code QUOTE_PREPARATION} precedes
 * {@code UPLOAD_DOCUMENT} in the pipeline – the step will use the {@code policyKey}
 * produced by Quote Preparation.  If Quote Preparation is not included, vendors
 * <strong>must</strong> supply {@code instnceKey} explicitly.
 */
@Data
public class VendorUploadData {

    /**
     * DXC entity key for the document category.
     * Example: the entity type that groups policy documents.
     */
    @NotNull(message = "entityKey is required for UPLOAD_DOCUMENT")
    private Integer entityKey;

    /**
     * DXC document type code.
     * Determines how the document is stored and indexed in the DXC system.
     */
    @NotNull(message = "documentType is required for UPLOAD_DOCUMENT")
    private Integer documentType;

    /**
     * DXC policy instance key.
     *
     * <p><strong>Optional</strong> when {@code QUOTE_PREPARATION} runs before this step –
     * in that case the pipeline uses {@code context.getPolicyKey()} automatically.
     * <strong>Required</strong> when {@code QUOTE_PREPARATION} is not in the pipeline.
     */
    private BigInteger instnceKey;
}
