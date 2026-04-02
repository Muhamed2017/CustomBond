package com.custombond.pipeline.steps;

import com.custombond.dto.request.IssuePolicyRequest;
import com.custombond.pipeline.IssuancePipelineContext;
import com.custombond.pipeline.PipelineStep;
import com.custombond.pipeline.PipelineStepException;
import com.custombond.pipeline.PipelineStepType;
import com.custombond.service.DXC_IssuePolicy_Service;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Pipeline step that finalises and issues the insurance policy.
 *
 * <p>This is the third and final step in the DXC policy flow (after Issue Quote).
 *
 * <h2>policyNo resolution</h2>
 * Same as {@code IssueQuoteStep}: context value from Quote Preparation wins,
 * falling back to the vendor-supplied {@code policyNo} in the top-level request.
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
public class IssuePolicyStep implements PipelineStep {

    private final DXC_IssuePolicy_Service issuePolicyService;

    @Override
    public PipelineStepType getType() {
        return PipelineStepType.ISSUE_POLICY;
    }

    @Override
    public String getName() {
        return "IssuePolicy";
    }

    /**
     * {@inheritDoc}
     *
     * <p>Resolves {@code policyNo} from the context or raw request,
     * then delegates to {@link DXC_IssuePolicy_Service}.
     */
    @Override
    public void execute(IssuancePipelineContext context) throws PipelineStepException {
        String policyNo = context.resolvedPolicyNo();
        if (policyNo == null || policyNo.isBlank()) {
            throw new PipelineStepException(
                    getName() + ": 'policyNo' is required – include QUOTE_PREPARATION before "
                            + "ISSUE_POLICY or supply 'policyNo' directly in the request");
        }

        IssuePolicyRequest request = IssuePolicyRequest.builder()
                .policyNo(policyNo)
                .parentRequestId(context.getParentRequestId())
                .vendorId(context.getRequest().getVendorId())
                .vendorRequestId(context.getRequest().getVendorRequestId())
                .build();

        log.debug("[{}] Issuing policy for policyNo='{}', parentRequestId='{}'",
                getName(), policyNo, context.getParentRequestId());

        ResponseEntity<Map<String, Object>> response = issuePolicyService.issuePolicy(request);

        if (response == null || !response.getStatusCode().is2xxSuccessful()) {
            String status = response != null ? response.getStatusCode().toString() : "null";
            String body = response != null && response.getBody() != null
                    ? response.getBody().toString() : "empty";
            throw new PipelineStepException(
                    getName() + ": DXC returned HTTP " + status + " – " + body);
        }

        log.info("[{}] Policy issued successfully for policyNo='{}' – HTTP {}",
                getName(), policyNo, response.getStatusCode());
    }
}
