package com.custombond.pipeline.steps;

import com.custombond.dto.request.DXCQuotePreparationRequest;
import com.custombond.dto.request.NafezaIssuanceRequest;
import com.custombond.dto.request.NafezaQuotePreparationData;
import com.custombond.dto.response.BlackListResponse;
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
 * NAFEZA variant of the quote-preparation pipeline step.
 *
 * <h2>Key difference from {@link QuotePreparationStep}</h2>
 * The standard {@link QuotePreparationStep} expects the vendor to supply a fully
 * populated {@link DXCQuotePreparationRequest} (including the {@code insured} field).
 * This step instead reads a {@link NafezaQuotePreparationData} block (which omits
 * {@code insured}) and automatically injects the DXC contact key obtained from the
 * preceding {@link PipelineStepType#CHECK_BLACK_LIST} step as the {@code insured} value.
 * This removes the need for NAFEZA vendors to look up the contact key before calling the API.
 *
 * <h2>Input</h2>
 * <ul>
 *   <li>{@code context.getNafezaRequest().getNafezaQuoteData()} – quote preparation data
 *       (all fields except {@code insured}).</li>
 *   <li>{@code context.getBlackListResult().getContactKey()} – DXC contact key resolved by
 *       the preceding {@code CHECK_BLACK_LIST} step; used as {@code insured}.</li>
 * </ul>
 *
 * <h2>Output</h2>
 * On success, populates:
 * <ul>
 *   <li>{@code context.policyNo} – generated policy number</li>
 *   <li>{@code context.policyKey} – generated policy key (used as {@code instnceKey} by
 *       the {@code NafezaUploadDocumentsStep})</li>
 *   <li>{@code context.resolvedInsured} – the contact key used as {@code insured}</li>
 * </ul>
 *
 * <h2>Abort conditions</h2>
 * Throws {@link PipelineStepException} when:
 * <ul>
 *   <li>The {@code nafezaRequest} or its {@code nafezaQuoteData} block is missing.</li>
 *   <li>No black-list result is available, or the contact key is null (stakeholder must
 *       be verified first via {@code CHECK_BLACK_LIST}).</li>
 *   <li>The DXC API returns a 4xx or 5xx response.</li>
 *   <li>The response body is null or missing required fields.</li>
 * </ul>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NafezaQuotePreparationStep implements PipelineStep {

    private final DXCQuotePreparationService quotePreparationService;

    @Override
    public PipelineStepType getType() {
        return PipelineStepType.NAFEZA_QUOTE_PREPARATION;
    }

    @Override
    public String getName() {
        return "NafezaQuotePreparation";
    }

    /**
     * {@inheritDoc}
     *
     * <p>Resolves {@code insured} from the black-list contact key, builds a
     * {@link DXCQuotePreparationRequest}, and delegates to
     * {@link DXCQuotePreparationService#quotePrepare(DXCQuotePreparationRequest, String, String, String)}.
     */
    @Override
    public void execute(IssuancePipelineContext context) throws PipelineStepException {
        // --- Validate NAFEZA request presence ---
        NafezaIssuanceRequest nafezaRequest = context.getNafezaRequest();
        if (nafezaRequest == null) {
            throw new PipelineStepException(
                    getName() + ": nafezaRequest is not set in context – "
                            + "use the /vendor/issue/nafeza endpoint for NAFEZA flows");
        }

        NafezaQuotePreparationData data = nafezaRequest.getNafezaQuoteData();
        if (data == null) {
            throw new PipelineStepException(
                    "Missing required 'nafezaQuoteData' input data for step " + getName());
        }

        // --- Resolve insured from black-list contact key ---
        BlackListResponse blackListResult = context.getBlackListResult();
        if (blackListResult == null) {
            throw new PipelineStepException(
                    getName() + ": black-list result is not available – "
                            + "ensure CHECK_BLACK_LIST runs before NAFEZA_QUOTE_PREPARATION");
        }
        if (blackListResult.getContactKey() == null) {
            throw new PipelineStepException(
                    getName() + ": contactKey is null in black-list result – "
                            + "stakeholder must exist in DXC (status=" + blackListResult.getStatus() + ")");
        }

        Long insured = blackListResult.getContactKey().longValue();
        context.setResolvedInsured(insured);

        log.debug("[{}] Resolved insured from black-list contactKey={}", getName(), insured);

        // --- Build DXCQuotePreparationRequest ---
        DXCQuotePreparationRequest request = DXCQuotePreparationRequest.builder()
                .effectiveDate(data.getEffectiveDate())
                .expiryDate(data.getExpiryDate())
                .currency(data.getCurrency())
                .division(data.getDivision())
                .subDivision(data.getSubDivision())
                .insured(insured)
                .underWriterContactKey(data.getUnderWriterContactKey())
                .mop(data.getMop())
                .producer(data.getProducer())
                .maximumExposure(data.getMaximumExposure())
                .product(data.getProduct())
                .hazardLevel(data.getHazardLevel())
                .policyName(data.getPolicyName())
                .contactTPA(data.getContactTPA())
                .gracePeriodDays(data.getGracePeriodDays())
                .marketSourceKey(data.getMarketSourceKey())
                .usePerecntageInBeneficiary(data.getUsePerecntageInBeneficiary())
                .benficiaries(data.getBenficiaries())
                .additionalInsureds(data.getAdditionalInsureds())
                .build();

        log.debug("[{}] Calling DXC Quote Preparation – insured={}, product={}, division={}",
                getName(), insured, data.getProduct(), data.getDivision());

        // --- Call DXC service with DB logging ---
        DXCQuotePreparationResponse response;
        try {
            response = quotePreparationService.quotePrepare(
                    request,
                    context.getParentRequestId(),
                    nafezaRequest.getVendorId(),
                    nafezaRequest.getVendorRequestId());
        } catch (DXCQuotePraparationApiBadRequestException e) {
            throw new PipelineStepException(
                    getName() + ": DXC rejected the quote request (400) – "
                            + e.getErrorResponse().getTitle(), e);
        } catch (DXCQuotePraparationApiException e) {
            throw new PipelineStepException(
                    getName() + ": DXC quote API error (HTTP " + e.getStatusCode() + ")", e);
        }

        if (response == null) {
            throw new PipelineStepException(
                    getName() + ": received null response from DXC Quote Preparation service");
        }
        if (response.getPolicyNo() == null || response.getPolicyKey() == null) {
            throw new PipelineStepException(
                    getName() + ": DXC returned incomplete response – policyNo or policyKey is null");
        }

        // --- Publish outputs for downstream steps ---
        context.setPolicyNo(response.getPolicyNo());
        context.setPolicyKey(response.getPolicyKey());

        log.info("[{}] NAFEZA quote prepared – policyNo='{}', policyKey={}, insured={}",
                getName(), response.getPolicyNo(), response.getPolicyKey(), insured);
    }
}
