package com.custombond.pipeline;

/**
 * Signals that a pipeline step has failed and the pipeline should be aborted.
 *
 * <p>Throw this from {@link PipelineStep#execute} whenever the step cannot continue.
 * The message is recorded in {@link IssuancePipelineContext#getFailureMessage()} and
 * ultimately returned to the vendor (either in the callback payload or in the logs).
 *
 * <p>Examples of situations that warrant this exception:
 * <ul>
 *   <li>Black-list check returned {@code REJECTED}</li>
 *   <li>Required input data is missing from the request (e.g. no {@code quotePreparation} block)</li>
 *   <li>External DXC API returned a non-success HTTP status</li>
 * </ul>
 */
public class PipelineStepException extends Exception {

    /**
     * Creates a new exception with the given failure message.
     *
     * @param message human-readable description of why the step failed
     */
    public PipelineStepException(String message) {
        super(message);
    }

    /**
     * Creates a new exception wrapping an underlying cause.
     *
     * @param message human-readable description of why the step failed
     * @param cause   the original exception that triggered this failure
     */
    public PipelineStepException(String message, Throwable cause) {
        super(message, cause);
    }
}
