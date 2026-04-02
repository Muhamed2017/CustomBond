package com.custombond.pipeline.steps;

import com.custombond.dto.request.DXCQuotePreparationRequest;
import com.custombond.dto.response.DXCQuotePreparationResponse;
import com.custombond.exception.DXCQuotePraparationApiBadRequestException;
import com.custombond.exception.DXCQuotePraparationApiException;
import com.custombond.pipeline.IssuancePipelineContext;
import com.custombond.pipeline.PipelineStep;
import com.custombond.pipeline.PipelineStepException;
import com.custombond.pipeline.PipelineStepType;
import com.custombond.service.DXCQuotePreparationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Pipeline step that calls the DXC Quote Preparation service to create a new policy quote.
 *
 * <h2>Input</h2>
 * Reads a fully populated {@link DXCQuotePreparationRequest} from
 * {@code context.getRequest().getQuotePreparation()}.
 *
 * <h2>Output</h2>
 * On success, populates:
 * <ul>
 *   <li>{@code context.policyNo} – the generated policy number</li>
 *   <li>{@code context.policyKey} – the generated policy key (used as {@code instnceKey}
 *       for the {@code UploadDocumentStep})</li>
 * </ul>
 *
 * <h2>Abort conditions</h2>
 * Throws {@link PipelineStepException} when:
 * <ul>
 *   <li>The {@code quotePreparation} block is missing from the request.</li>
 *   <li>The DXC API returns a 4xx or 5xx response (mapped from the service's own exceptions).</li>
 *   <li>The response body is null or missing required fields.</li>
 * </ul>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class QuotePreparationStep implements PipelineStep {

    private final DXCQuotePreparationService quotePreparationService;

    @Override
    public PipelineStepType getType() {
        return PipelineStepType.QUOTE_PREPARATION;
    }

    @Override
    public String getName() {
        return "QuotePreparation";
    }

    /**
     * {@inheritDoc}
     *
     * <p>Delegates to {@link DXCQuotePreparationService#quotePrepare(DXCQuotePreparationRequest)}.
     * On success, writes {@code policyNo} and {@code policyKey} into the context
     * so that downstream steps can use them without repeating the look-up.
     */
    @Override
    public void execute(IssuancePipelineContext context) throws PipelineStepException {
        DXCQuotePreparationRequest request = context.getRequest().getQuotePreparation();
        if (request == null) {
            throw new PipelineStepException(
                    "Missing required 'quotePreparation' input data for step " + getName());
        }

        log.debug("[{}] Calling DXC Quote Preparation service – insured={}, product={}",
                getName(), request.getInsured(), request.getProduct());

        DXCQuotePreparationResponse response;
        try {
            response = quotePreparationService.quotePrepare(request);
        } catch (DXCQuotePraparationApiBadRequestException e) {
            throw new PipelineStepException(
                    getName() + ": DXC rejected the quote request (400) – "
                            + e.getErrorResponse().getTitle(), e);
        } catch (DXCQuotePraparationApiException e) {
            throw new PipelineStepException(
                    getName() + ": DXC quote API error (HTTP " + e.getStatusCode() + ")", e);
        }

        if (response == null) {
            throw new PipelineStepException(getName() + ": received null response from DXC Quote Preparation service");
        }
        if (response.getPolicyNo() == null || response.getPolicyKey() == null) {
            throw new PipelineStepException(
                    getName() + ": DXC returned incomplete response – policyNo or policyKey is null");
        }

        // Publish outputs for downstream steps
        context.setPolicyNo(response.getPolicyNo());
        context.setPolicyKey(response.getPolicyKey());

        log.info("[{}] Quote prepared successfully – policyNo='{}', policyKey={}",
                getName(), response.getPolicyNo(), response.getPolicyKey());
    }
}
