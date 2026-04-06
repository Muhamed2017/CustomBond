package com.custombond.pipeline.steps;

import com.custombond.dto.request.BlackListRequest;
import com.custombond.dto.request.VendorBlackListData;
import com.custombond.dto.response.BlackListResponse;
import com.custombond.entity.Enums;
import com.custombond.pipeline.IssuancePipelineContext;
import com.custombond.pipeline.PipelineStep;
import com.custombond.pipeline.PipelineStepException;
import com.custombond.pipeline.PipelineStepType;
import com.custombond.service.DXC_BlackList_Service;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

/**
 * Pipeline step that checks whether the vendor's stakeholder is blacklisted.
 *
 * <h2>Input</h2>
 * Reads {@link VendorBlackListData} from
 * {@code context.getRequest().getBlackList()}.  The top-level {@code vendorId}
 * and {@code vendorRequestId} are injected automatically so the vendor does not
 * need to repeat them inside the nested block.
 *
 * <h2>Output</h2>
 * Writes the full {@link BlackListResponse} to {@code context.setBlackListResult(…)}.
 * The contact key inside the response is available to downstream steps via
 * {@code context.getBlackListResult().getContactKey()}.
 *
 * <h2>Abort condition</h2>
 * Throws {@link PipelineStepException} (aborting the pipeline) when:
 * <ul>
 *   <li>The {@code blackList} block is missing from the request.</li>
 *   <li>The DXC response status is {@link Enums.Status#REJECTED}.</li>
 *   <li>The DXC response indicates an error ({@code errorMessage} is non-null).</li>
 *   <li>The HTTP response is not 2xx.</li>
 * </ul>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CheckBlackListStep implements PipelineStep {

    private final DXC_BlackList_Service blackListService;

    @Override
    public PipelineStepType getType() {
        return PipelineStepType.CHECK_BLACK_LIST;
    }

    @Override
    public String getName() {
        return "CheckBlackList";
    }

    /**
     * {@inheritDoc}
     *
     * <p>Builds a {@link BlackListRequest} from the vendor-supplied
     * {@link VendorBlackListData}, injects the shared {@code vendorId} /
     * {@code vendorRequestId}, then delegates to {@link DXC_BlackList_Service}.
     */
    @Override
    public void execute(IssuancePipelineContext context) throws PipelineStepException {
        VendorBlackListData data = context.getRequest().getBlackList();
        if (data == null) {
            throw new PipelineStepException(
                    "Missing required 'blackList' input data for step " + getName());
        }

        BlackListRequest request = buildBlackListRequest(data, context);
        log.debug("[{}] Calling DXC black-list service for stakeholder='{}', vendorId='{}'",
                getName(), data.getStakeholderName(), context.getRequest().getVendorId());

        ResponseEntity<BlackListResponse> response = blackListService.checkBlackList(request);

        if (response == null || response.getBody() == null) {
            throw new PipelineStepException(getName() + ": received null response from DXC black-list service");
        }

        BlackListResponse body = response.getBody();

        // Abort if the stakeholder is explicitly rejected – this is the only hard stop
        if (Enums.Status.REJECTED.equals(body.getStatus())) {
            String reason = body.getRejectionReasonAr() != null
                    ? body.getRejectionReasonAr()
                    : body.getRejectionCode();
            throw new PipelineStepException(
                    getName() + ": stakeholder is REJECTED – " + reason);
        }

        // HTTP 404 + APPROVED means the contact does not yet exist in DXC but is not
        // blacklisted. The downstream NAFEZA_QUOTE_PREPARATION step will call GetContact
        // to obtain the contact key once DXC has created it. Treat this as a pass.
        if (response.getStatusCode().value() == 404
                && Enums.Status.APPROVED.equals(body.getStatus())) {
            context.setBlackListResult(body);
            log.info("[{}] Contact not found in DXC (will be resolved by downstream step) – APPROVED",
                    getName());
            return;
        }

        // Any other non-2xx response is an unexpected error
        if (!response.getStatusCode().is2xxSuccessful()) {
            throw new PipelineStepException(
                    getName() + ": DXC returned HTTP " + response.getStatusCode());
        }

        // Surface actual service-level errors (non-blank message that is not the benign
        // "TaxId is not found" which is covered by the 404 branch above)
        if (body.getErrorMessage() != null
                && !body.getErrorMessage().isEmpty()
                && !"TaxId is not found".equals(body.getErrorMessage())) {
            throw new PipelineStepException(
                    getName() + ": DXC error – " + body.getErrorMessage());
        }

        // Persist result for downstream steps
        context.setBlackListResult(body);
        log.info("[{}] Stakeholder '{}' passed black-list check – status={}, existed={}",
                getName(), data.getStakeholderName(), body.getStatus(), body.isExisted());
    }

    // -----------------------------------------------------------------------
    // Private helpers
    // -----------------------------------------------------------------------

    /**
     * Maps {@link VendorBlackListData} + pipeline context to the full {@link BlackListRequest}
     * expected by {@link DXC_BlackList_Service}.
     */
    private BlackListRequest buildBlackListRequest(VendorBlackListData data,
                                                   IssuancePipelineContext context) {
        BlackListRequest req = new BlackListRequest();
        // callSequance is always FIRST_CALL when initiated via the vendor pipeline
        req.setCallSequance(BlackListRequest.CallSequence.FIRST_CALL);
        req.setStakeholderName(data.getStakeholderName());
        req.setSectorType(BlackListRequest.SectorType.valueOf(data.getSectorType().name()));
        // vendorId / vendorRequestId come from the top-level vendor request
        req.setVendorId(context.getRequest().getVendorId());
        req.setVendorRequestId(context.getRequest().getVendorRequestId());
        req.setTaxId(data.getTaxId());
        req.setNationalId(data.getNationalId());
        req.setContactKey(data.getContactKey());
        req.setCommercialRegNo(data.getCommercialRegNo());
        req.setAddress(data.getAddress());
        req.setPhone(data.getPhone());
        req.setEmail(data.getEmail());
        req.setLegalRepName(data.getLegalRepName());
        req.setLegalRepNationalId(data.getLegalRepNationalId());
        return req;
    }
}
