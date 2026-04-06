package com.custombond.pipeline;

/**
 * Enumeration of all available pipeline steps.
 *
 * <p>Vendors specify an ordered list of these values in {@code VendorIssuanceRequest.steps}
 * to control which services are executed and in what sequence for their issuance flow.
 *
 * <p>Typical full-flow order (standard vendor):
 * <ol>
 *   <li>{@link #CHECK_BLACK_LIST} – verify the contact is not blacklisted</li>
 *   <li>{@link #QUOTE_PREPARATION} – prepare quote; produces {@code policyNo} and {@code policyKey}</li>
 *   <li>{@link #UPLOAD_DOCUMENT} – attach supporting document to the policy</li>
 *   <li>{@link #ISSUE_QUOTE} – issue the quote against the prepared policy</li>
 *   <li>{@link #ISSUE_POLICY} – finalise and issue the policy</li>
 * </ol>
 *
 * <p>Typical full-flow order (NAFEZA):
 * <ol>
 *   <li>{@link #CHECK_BLACK_LIST} – verify and resolve the stakeholder contact key</li>
 *   <li>{@link #NAFEZA_QUOTE_PREPARATION} – prepare quote using the resolved insured contact key</li>
 *   <li>{@link #NAFEZA_UPLOAD_DOCUMENTS} – upload documents for the NAFEZA flow</li>
 *   <li>{@link #ISSUE_QUOTE} – issue the quote</li>
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
     * Also resolves and stores the DXC contact key for use by downstream NAFEZA steps.
     */
    CHECK_BLACK_LIST,

    /**
     * Calls the DXC Quote Preparation service using a fully pre-built
     * {@link com.custombond.dto.request.DXCQuotePreparationRequest} supplied by the vendor.
     * On success it populates {@code IssuancePipelineContext.policyNo} and {@code .policyKey}.
     */
    QUOTE_PREPARATION,

    /**
     * NAFEZA variant of quote preparation.
     * Builds the {@link com.custombond.dto.request.DXCQuotePreparationRequest} from the
     * {@link com.custombond.dto.request.NafezaQuotePreparationData} in the NAFEZA request,
     * automatically filling {@code insured} from the contact key resolved by the preceding
     * {@link #CHECK_BLACK_LIST} step.
     * On success it populates {@code IssuancePipelineContext.policyNo}, {@code .policyKey},
     * and {@code .resolvedInsured}.
     */
    NAFEZA_QUOTE_PREPARATION,

    /**
     * Uploads the vendor-supplied document to the DXC Document service.
     * Requires file bytes to be present in the pipeline context.
     * {@code instnceKey} is taken from the context's {@code policyKey} if not supplied explicitly.
     */
    UPLOAD_DOCUMENT,

    /**
     * NAFEZA variant of document upload.
     * Functionally equivalent to {@link #UPLOAD_DOCUMENT} but registered under its own type
     * to allow NAFEZA pipelines to use a distinct step name in the {@code steps} list.
     */
    NAFEZA_UPLOAD_DOCUMENTS,

    /**
     * Issues the quote against the prepared policy (step 2 in the DXC flow).
     * Requires {@code policyNo} – either from a preceding {@code QUOTE_PREPARATION} /
     * {@code NAFEZA_QUOTE_PREPARATION} step or supplied directly in the request.
     */
    ISSUE_QUOTE,

    /**
     * Finalises and issues the policy (step 3 in the DXC flow).
     * Requires {@code policyNo} – same sourcing rules as {@link #ISSUE_QUOTE}.
     */
    ISSUE_POLICY
}
