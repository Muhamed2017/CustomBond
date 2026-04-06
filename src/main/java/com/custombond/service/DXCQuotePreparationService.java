package com.custombond.service;

import org.springframework.stereotype.Component;

import com.custombond.dto.request.DXCQuotePreparationRequest;
import com.custombond.dto.response.DXCQuotePreparationResponse;

@Component
public interface DXCQuotePreparationService {

    /**
     * Issues a new policy by forwarding the request to the DXC policy engine API.
     * Does not write a DB audit log entry.
     *
     * @param request the validated policy issuance payload
     * @return the issued policy key and number on success
     * @throws com.custombond.exception.DXCQuotePraparationApiBadRequestException if the API rejects the request (400)
     * @throws com.custombond.exception.DXCQuotePraparationApiException            if the API returns any other non-2xx response
     */
    DXCQuotePreparationResponse quotePrepare(DXCQuotePreparationRequest request);

    /**
     * Issues a new policy and writes a {@code CBGeneralLog} audit record (request + response).
     *
     * <p>Follows the same DB-logging pattern used by {@code DXC_IssueQuote_Service} and
     * {@code DXC_IssuePolicy_Service}.
     *
     * @param request         the validated policy issuance payload
     * @param parentRequestId UUID string from the pipeline context (used as correlation key)
     * @param vendorId        calling vendor identifier stored in the audit row
     * @param vendorRequestId vendor's own reference number stored in the audit row
     * @return the issued policy key and number on success
     * @throws com.custombond.exception.DXCQuotePraparationApiBadRequestException if the API rejects the request (400)
     * @throws com.custombond.exception.DXCQuotePraparationApiException            if the API returns any other non-2xx response
     */
    DXCQuotePreparationResponse quotePrepare(DXCQuotePreparationRequest request,
                                             String parentRequestId,
                                             String vendorId,
                                             String vendorRequestId);
}
