package com.custombond.pipeline.steps;

import com.custombond.dto.request.IssueQuoteRequest;
import com.custombond.pipeline.IssuancePipelineContext;
import com.custombond.pipeline.PipelineStep;
import com.custombond.pipeline.PipelineStepException;
import com.custombond.pipeline.PipelineStepType;
import com.custombond.service.DXC_IssueQuote_Service;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Pipeline step that issues the quote against the prepared policy.
 *
 * <p>This is the second step in the DXC policy flow (after Quote Preparation).
 *
 * <h2>policyNo resolution</h2>
 * <ol>
 *   <li>{@code context.getPolicyNo()} – written by a preceding {@code QuotePreparationStep}.</li>
 *   <li>{@code context.getRequest().getPolicyNo()} – vendor-supplied fallback for flows
 *       that skip Quote Preparation.</li>
 * </ol>
 * If neither source provides a value, the step throws {@link PipelineStepException}.
 *
 * <h2>Abort conditions</h2>
 * Throws {@link PipelineStepException} when:
 * <ul>
 *   <li>{@code policyNo} cannot be resolved from any source.</li>
 *   <li>The DXC API returns a non-2xx HTTP status.</li>
 * </ul>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class IssueQuoteStep implements PipelineStep {

    private final DXC_IssueQuote_Service issueQuoteService;

    @Override
    public PipelineStepType getType() {
        return PipelineStepType.ISSUE_QUOTE;
    }

    @Override
    public String getName() {
        return "IssueQuote";
    }

    /**
     * {@inheritDoc}
     *
     * <p>Resolves {@code policyNo} from the context (set by QuotePreparation) or from
     * the raw vendor request, then delegates to {@link DXC_IssueQuote_Service}.
     */
    @Override
    public void execute(IssuancePipelineContext context) throws PipelineStepException {
        String policyNo = context.resolvedPolicyNo();
        if (policyNo == null || policyNo.isBlank()) {
            throw new PipelineStepException(
                    getName() + ": 'policyNo' is required – include QUOTE_PREPARATION before "
                            + "ISSUE_QUOTE or supply 'policyNo' directly in the request");
        }

        IssueQuoteRequest request = IssueQuoteRequest.builder()
                .policyNo(policyNo)
                .parentRequestId(context.getParentRequestId())
                .vendorId(context.getRequest().getVendorId())
                .vendorRequestId(context.getRequest().getVendorRequestId())
                .build();

        log.debug("[{}] Issuing quote for policyNo='{}', parentRequestId='{}'",
                getName(), policyNo, context.getParentRequestId());

        ResponseEntity<Map<String, Object>> response = issueQuoteService.issueQoute(request);

        if (response == null || !response.getStatusCode().is2xxSuccessful()) {
            String status = response != null ? response.getStatusCode().toString() : "null";
            String body = response != null && response.getBody() != null
                    ? response.getBody().toString() : "empty";
            throw new PipelineStepException(
                    getName() + ": DXC returned HTTP " + status + " – " + body);
        }

        log.info("[{}] Quote issued successfully for policyNo='{}' – HTTP {}",
                getName(), policyNo, response.getStatusCode());
    }
}
