package com.custombond.pipeline;

import com.custombond.dto.request.VendorIssuanceRequest;
import com.custombond.pipeline.steps.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link IssuancePipeline}.
 *
 * <p>All {@link PipelineStep} implementations are replaced with Mockito mocks so
 * no external services are called.  Tests verify the pipeline's orchestration logic:
 * correct step ordering, early-abort on failure, and context mutation.
 *
 * <p><strong>How to run:</strong>
 * <pre>
 *   mvn test -Dtest=IssuancePipelineTest
 * </pre>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("IssuancePipeline – orchestration unit tests")
class IssuancePipelineTest {

    // -----------------------------------------------------------------------
    // Mock steps – each behaves as a no-op (success) by default
    // -----------------------------------------------------------------------

    @Mock private CheckBlackListStep checkBlackListStep;
    @Mock private QuotePreparationStep quotePreparationStep;
    @Mock private UploadDocumentStep uploadDocumentStep;
    @Mock private IssueQuoteStep issueQuoteStep;
    @Mock private IssuePolicyStep issuePolicyStep;

    private IssuancePipeline pipeline;

    @BeforeEach
    void setUp() {
        // Wire up type -> bean mapping
        when(checkBlackListStep.getType()).thenReturn(PipelineStepType.CHECK_BLACK_LIST);
        when(quotePreparationStep.getType()).thenReturn(PipelineStepType.QUOTE_PREPARATION);
        when(uploadDocumentStep.getType()).thenReturn(PipelineStepType.UPLOAD_DOCUMENT);
        when(issueQuoteStep.getType()).thenReturn(PipelineStepType.ISSUE_QUOTE);
        when(issuePolicyStep.getType()).thenReturn(PipelineStepType.ISSUE_POLICY);

        // Wire up display names (used in failure messages)
        when(checkBlackListStep.getName()).thenReturn("CheckBlackList");
        when(quotePreparationStep.getName()).thenReturn("QuotePreparation");
        when(uploadDocumentStep.getName()).thenReturn("UploadDocument");
        when(issueQuoteStep.getName()).thenReturn("IssueQuote");
        when(issuePolicyStep.getName()).thenReturn("IssuePolicy");

        pipeline = new IssuancePipeline(List.of(
                checkBlackListStep,
                quotePreparationStep,
                uploadDocumentStep,
                issueQuoteStep,
                issuePolicyStep));
    }

    // -----------------------------------------------------------------------
    // Happy-path tests
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("Full pipeline with all steps – all steps executed and context marked successful")
    void execute_allSteps_allExecutedAndSuccessful() throws Exception {
        IssuancePipelineContext ctx = buildContext(
                PipelineStepType.CHECK_BLACK_LIST,
                PipelineStepType.QUOTE_PREPARATION,
                PipelineStepType.UPLOAD_DOCUMENT,
                PipelineStepType.ISSUE_QUOTE,
                PipelineStepType.ISSUE_POLICY);

        pipeline.execute(ctx);

        assertThat(ctx.isSuccessful()).isTrue();
        assertThat(ctx.getFailedStep()).isNull();

        // Verify each step was called exactly once
        verify(checkBlackListStep).execute(ctx);
        verify(quotePreparationStep).execute(ctx);
        verify(uploadDocumentStep).execute(ctx);
        verify(issueQuoteStep).execute(ctx);
        verify(issuePolicyStep).execute(ctx);
    }

    @Test
    @DisplayName("Subset pipeline (CHECK_BLACK_LIST + ISSUE_POLICY only) – only selected steps run")
    void execute_subsetSteps_onlySelectedStepsRun() throws Exception {
        IssuancePipelineContext ctx = buildContext(
                PipelineStepType.CHECK_BLACK_LIST,
                PipelineStepType.ISSUE_POLICY);

        pipeline.execute(ctx);

        assertThat(ctx.isSuccessful()).isTrue();
        verify(checkBlackListStep).execute(ctx);
        verify(issuePolicyStep).execute(ctx);

        // Steps not in the list must never be called
        verify(quotePreparationStep, never()).execute(any());
        verify(uploadDocumentStep, never()).execute(any());
        verify(issueQuoteStep, never()).execute(any());
    }

    @Test
    @DisplayName("Single step pipeline (ISSUE_POLICY) – only that step runs")
    void execute_singleStep_onlyThatStepRuns() throws Exception {
        IssuancePipelineContext ctx = buildContext(PipelineStepType.ISSUE_POLICY);

        pipeline.execute(ctx);

        assertThat(ctx.isSuccessful()).isTrue();
        verify(issuePolicyStep).execute(ctx);
        verify(checkBlackListStep, never()).execute(any());
        verify(quotePreparationStep, never()).execute(any());
        verify(uploadDocumentStep, never()).execute(any());
        verify(issueQuoteStep, never()).execute(any());
    }

    // -----------------------------------------------------------------------
    // Failure / abort tests
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("First step fails – pipeline aborts and remaining steps are skipped")
    void execute_firstStepFails_pipelineAbortsImmediately() throws Exception {
        doThrow(new PipelineStepException("Stakeholder is REJECTED"))
                .when(checkBlackListStep).execute(any());

        IssuancePipelineContext ctx = buildContext(
                PipelineStepType.CHECK_BLACK_LIST,
                PipelineStepType.QUOTE_PREPARATION,
                PipelineStepType.ISSUE_POLICY);

        pipeline.execute(ctx);

        assertThat(ctx.isSuccessful()).isFalse();
        assertThat(ctx.getFailedStep()).isEqualTo("CheckBlackList");
        assertThat(ctx.getFailureMessage()).isEqualTo("Stakeholder is REJECTED");

        // Steps after the failure must never be called
        verify(quotePreparationStep, never()).execute(any());
        verify(issuePolicyStep, never()).execute(any());
    }

    @Test
    @DisplayName("Middle step fails – steps before are called, steps after are skipped")
    void execute_middleStepFails_onlyPrecedingStepsRun() throws Exception {
        doThrow(new PipelineStepException("DXC upload API error"))
                .when(uploadDocumentStep).execute(any());

        IssuancePipelineContext ctx = buildContext(
                PipelineStepType.CHECK_BLACK_LIST,
                PipelineStepType.QUOTE_PREPARATION,
                PipelineStepType.UPLOAD_DOCUMENT,
                PipelineStepType.ISSUE_QUOTE,
                PipelineStepType.ISSUE_POLICY);

        pipeline.execute(ctx);

        assertThat(ctx.isSuccessful()).isFalse();
        assertThat(ctx.getFailedStep()).isEqualTo("UploadDocument");
        assertThat(ctx.getFailureMessage()).contains("DXC upload API error");

        verify(checkBlackListStep).execute(ctx);
        verify(quotePreparationStep).execute(ctx);
        verify(uploadDocumentStep).execute(ctx);
        verify(issueQuoteStep, never()).execute(any());
        verify(issuePolicyStep, never()).execute(any());
    }

    @Test
    @DisplayName("Unchecked exception in step – caught and treated as step failure")
    void execute_stepThrowsRuntimeException_treatedAsFailure() throws Exception {
        doThrow(new RuntimeException("Unexpected NullPointerException"))
                .when(quotePreparationStep).execute(any());

        IssuancePipelineContext ctx = buildContext(
                PipelineStepType.QUOTE_PREPARATION,
                PipelineStepType.ISSUE_QUOTE);

        pipeline.execute(ctx);

        assertThat(ctx.isSuccessful()).isFalse();
        assertThat(ctx.getFailedStep()).isEqualTo("QuotePreparation");
        assertThat(ctx.getFailureMessage()).contains("Unexpected error");
        verify(issueQuoteStep, never()).execute(any());
    }

    @Test
    @DisplayName("Unknown step type – pipeline records failure immediately")
    void execute_unknownStepType_pipelineFailsWithMeaningfulMessage() {
        // Build a request referencing a step type that has no registered bean
        // We achieve this by creating a pipeline with only 2 registered steps,
        // then requesting a third one that is not registered.
        IssuancePipeline smallPipeline = new IssuancePipeline(
                List.of(issueQuoteStep, issuePolicyStep));

        IssuancePipelineContext ctx = buildContext(
                PipelineStepType.ISSUE_QUOTE,
                PipelineStepType.CHECK_BLACK_LIST);   // not registered in smallPipeline

        smallPipeline.execute(ctx);

        assertThat(ctx.isSuccessful()).isFalse();
        assertThat(ctx.getFailedStep()).isEqualTo(PipelineStepType.CHECK_BLACK_LIST.name());
        assertThat(ctx.getFailureMessage()).contains("CHECK_BLACK_LIST");
    }

    // -----------------------------------------------------------------------
    // Context mutation tests
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("QuotePreparation step sets policyNo/policyKey – downstream step reads them")
    void execute_quotePreparationWritesToContext_downstreamStepReadsIt() throws Exception {
        // Simulate QuotePreparationStep writing to context
        doAnswer(invocation -> {
            IssuancePipelineContext c = invocation.getArgument(0);
            c.setPolicyNo("POL-2024-001");
            c.setPolicyKey(98765L);
            return null;
        }).when(quotePreparationStep).execute(any());

        IssuancePipelineContext ctx = buildContext(
                PipelineStepType.QUOTE_PREPARATION,
                PipelineStepType.ISSUE_QUOTE);

        pipeline.execute(ctx);

        assertThat(ctx.isSuccessful()).isTrue();
        assertThat(ctx.getPolicyNo()).isEqualTo("POL-2024-001");
        assertThat(ctx.getPolicyKey()).isEqualTo(98765L);
        assertThat(ctx.resolvedPolicyNo()).isEqualTo("POL-2024-001");
    }

    @Test
    @DisplayName("resolvedPolicyNo falls back to request.policyNo when context has no policyNo")
    void execute_resolvedPolicyNo_fallbackToRequestPolicyNo() {
        VendorIssuanceRequest req = VendorIssuanceRequest.builder()
                .vendorId("V001")
                .steps(List.of(PipelineStepType.ISSUE_QUOTE))
                .policyNo("POL-FALLBACK")
                .build();

        IssuancePipelineContext ctx = IssuancePipelineContext.builder()
                .request(req)
                .parentRequestId("parent-uuid")
                .build();

        // context.policyNo is null here (QuotePrep did not run)
        assertThat(ctx.resolvedPolicyNo()).isEqualTo("POL-FALLBACK");
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    /**
     * Builds a pipeline context with the given ordered step types and a stub vendor request.
     */
    private IssuancePipelineContext buildContext(PipelineStepType... stepTypes) {
        VendorIssuanceRequest request = VendorIssuanceRequest.builder()
                .vendorId("V001")
                .vendorRequestId("REQ-TEST")
                .steps(List.of(stepTypes))
                .policyNo("POL-FALLBACK")
                .build();

        return IssuancePipelineContext.builder()
                .request(request)
                .parentRequestId("test-parent-uuid-001")
                .build();
    }
}
