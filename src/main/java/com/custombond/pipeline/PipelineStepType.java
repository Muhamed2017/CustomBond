package com.custombond.pipeline;

/**
 * Enumeration of all available pipeline steps.
 *
 * <p>Vendors specify an ordered list of these values in {@code VendorIssuanceRequest.steps}
 * to control which services are executed and in what sequence for their issuance flow.
 *
 * <p>Typical full-flow order:
 * <ol>
 *   <li>{@link #CHECK_BLACK_LIST} – verify the contact is not blacklisted</li>
 *   <li>{@link #QUOTE_PREPARATION} – prepare quote; produces {@code policyNo} and {@code policyKey}</li>
 *   <li>{@link #UPLOAD_DOCUMENT} – attach supporting document to the policy</li>
 *   <li>{@link #ISSUE_QUOTE} – issue the quote against the prepared policy</li>
 *   <li>{@link #ISSUE_POLICY} – finalise and issue the policy</li>
 * </ol>
 *
 * <p>Vendors are not required to include all steps; each vendor's configuration may omit
 * steps or reorder them as appropriate for their workflow.
 */
public enum PipelineStepType {

    /**
     * Calls the DXC black-list check service to verify the stakeholder is neither
     * blacklisted nor unknown. The pipeline aborts if the status is {@code REJECTED}.
     */
    CHECK_BLACK_LIST,

    /**
     * Calls the DXC Quote Preparation service. On success it populates
     * {@code IssuancePipelineContext.policyNo} and {@code .policyKey} for downstream steps.
     */
    QUOTE_PREPARATION,

    /**
     * Uploads the vendor-supplied document to the DXC Document service.
     * Requires file bytes to be present in the pipeline context.
     * {@code instnceKey} is taken from the context's {@code policyKey} if not supplied explicitly.
     */
    UPLOAD_DOCUMENT,

    /**
     * Issues the quote against the prepared policy (step 2 in the DXC flow).
     * Requires {@code policyNo} – either from a preceding {@code QUOTE_PREPARATION} step
     * or supplied directly in {@code VendorIssuanceRequest.policyNo}.
     */
    ISSUE_QUOTE,

    /**
     * Finalises and issues the policy (step 3 in the DXC flow).
     * Requires {@code policyNo} – same sourcing rules as {@link #ISSUE_QUOTE}.
     */
    ISSUE_POLICY
}
