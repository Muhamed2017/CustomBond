package com.custombond.pipeline;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Orchestrates the vendor issuance pipeline by executing each configured
 * {@link PipelineStep} in the order specified by
 * {@link com.custombond.dto.request.VendorIssuanceRequest#getSteps()}.
 *
 * <h2>Registration</h2>
 * All Spring beans that implement {@link PipelineStep} are injected automatically
 * via the {@code List<PipelineStep>} constructor argument. The pipeline builds an
 * internal registry keyed by {@link PipelineStepType} at startup, so adding a new
 * step only requires creating a new {@code @Component} that implements {@link PipelineStep}.
 *
 * <h2>Execution contract</h2>
 * <ul>
 *   <li>Steps execute sequentially in the order defined by the vendor's {@code steps} list.</li>
 *   <li>If a step throws {@link PipelineStepException} or any unchecked exception, the
 *       pipeline records the failure on the {@link IssuancePipelineContext} and returns
 *       immediately – no further steps are executed.</li>
 *   <li>On full success, {@code context.isSuccessful()} is set to {@code true}.</li>
 * </ul>
 *
 * <h2>Thread safety</h2>
 * Each invocation uses its own {@link IssuancePipelineContext} instance, so concurrent
 * executions on the thread pool defined in {@code AsyncConfig} are fully isolated.
 */
@Slf4j
@Component
public class IssuancePipeline {

    /** Registry of all available step implementations, keyed by step type. */
    private final Map<PipelineStepType, PipelineStep> stepRegistry;

    /**
     * Spring injects every {@link PipelineStep} bean in the application context.
     *
     * @param allSteps all discovered {@link PipelineStep} implementations
     * @throws IllegalStateException if two beans report the same {@link PipelineStepType}
     */
    public IssuancePipeline(List<PipelineStep> allSteps) {
        this.stepRegistry = allSteps.stream()
                .collect(Collectors.toMap(
                        PipelineStep::getType,
                        Function.identity(),
                        (a, b) -> {
                            throw new IllegalStateException(
                                    "Duplicate PipelineStep beans for type: " + a.getType());
                        }
                ));
        log.info("[Pipeline] Registered {} step(s): {}", stepRegistry.size(), stepRegistry.keySet());
    }

    /**
     * Executes the pipeline defined by {@code context.getRequest().getSteps()}.
     *
     * <p>The context is mutated in place: each step may read and write shared fields
     * (e.g. {@code policyNo}, {@code policyKey}). Callers should inspect
     * {@link IssuancePipelineContext#isSuccessful()} and
     * {@link IssuancePipelineContext#getFailedStep()} after this method returns.
     *
     * @param context the fully initialised pipeline context for this execution
     * @return the same {@code context} instance, with status fields populated
     */
    public IssuancePipelineContext execute(IssuancePipelineContext context) {
        List<PipelineStepType> requestedSteps = context.getRequest().getSteps();
        String parentId = context.getParentRequestId();

        log.info("[Pipeline] parentRequestId={} | executing {} step(s): {}",
                parentId, requestedSteps.size(), requestedSteps);

        for (PipelineStepType stepType : requestedSteps) {
            PipelineStep step = stepRegistry.get(stepType);

            if (step == null) {
                String msg = "No PipelineStep implementation registered for type: " + stepType;
                log.error("[Pipeline] parentRequestId={} | {}", parentId, msg);
                context.markFailed(stepType.name(), msg);
                return context;
            }

            log.info("[Pipeline] parentRequestId={} | starting step '{}'", parentId, step.getName());

            try {
                step.execute(context);
                log.info("[Pipeline] parentRequestId={} | step '{}' completed successfully",
                        parentId, step.getName());

            } catch (PipelineStepException e) {
                log.warn("[Pipeline] parentRequestId={} | step '{}' failed: {}",
                        parentId, step.getName(), e.getMessage());
                context.markFailed(step.getName(), e.getMessage());
                return context;

            } catch (Exception e) {
                String msg = "Unexpected error in step '" + step.getName() + "': " + e.getMessage();
                log.error("[Pipeline] parentRequestId={} | {}", parentId, msg, e);
                context.markFailed(step.getName(), msg);
                return context;
            }
        }

        context.setSuccessful(true);
        log.info("[Pipeline] parentRequestId={} | all steps completed successfully", parentId);
        return context;
    }
}
