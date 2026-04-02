package com.custombond.pipeline;

/**
 * Contract for a single step in the vendor issuance pipeline.
 *
 * <p>Each implementation wraps exactly one existing DXC service and is registered
 * as a Spring bean so that {@link IssuancePipeline} can auto-discover all steps.
 *
 * <p>Steps communicate through the shared {@link IssuancePipelineContext}: they read
 * the vendor request data they need and write any outputs (e.g. {@code policyNo})
 * back to the context for downstream steps to consume.
 *
 * <p><strong>Error contract:</strong> Throw {@link PipelineStepException} when the step
 * cannot proceed (e.g. blacklist rejected, external API error). Any unchecked exception
 * that escapes {@code execute} is caught by the pipeline and treated as a fatal step
 * failure, aborting the remaining steps.
 */
public interface PipelineStep {

    /**
     * Returns the {@link PipelineStepType} that this implementation handles.
     * Used by {@link IssuancePipeline} to build its step registry at startup.
     *
     * @return the step type; must be unique across all registered {@code PipelineStep} beans
     */
    PipelineStepType getType();

    /**
     * Human-readable name shown in failure/success log messages and in the
     * {@link com.custombond.dto.response.VendorPipelineResult}.
     *
     * @return short display name, e.g. {@code "CheckBlackList"}
     */
    String getName();

    /**
     * Executes the step using data from {@code context}.
     *
     * <p>Implementations should:
     * <ul>
     *   <li>Read their input from {@code context.getRequest()} and/or earlier step outputs</li>
     *   <li>Call the corresponding service</li>
     *   <li>Write any output values back to {@code context} (e.g. {@code setPolicyNo})</li>
     *   <li>Throw {@link PipelineStepException} on any logical or HTTP failure</li>
     * </ul>
     *
     * @param context the mutable pipeline context shared by all steps in this execution
     * @throws PipelineStepException if the step fails in a recoverable or expected way
     */
    void execute(IssuancePipelineContext context) throws PipelineStepException;
}
