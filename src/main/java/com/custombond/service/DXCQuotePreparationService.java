package com.custombond.service;

import org.springframework.stereotype.Component;

import com.custombond.dto.request.DXCQuotePreparationRequest;
import com.custombond.dto.response.DXCQuotePreparationResponse;

@Component
public interface DXCQuotePreparationService {

    /**
     * Issues a new policy by forwarding the request to the dxc policy engine API.
     *
     * @param request the validated policy issuance payload
     * @return the issued policy key and number on success
     * @throws com.custombond.exception.DXCApiBadRequestException if the dxc API rejects the request (400)
     * @throws com.custombond.exception.DXCApiException            if the dxc API returns any other non-2xx response
     */
    DXCQuotePreparationResponse quotePrepare(DXCQuotePreparationRequest request);
}
