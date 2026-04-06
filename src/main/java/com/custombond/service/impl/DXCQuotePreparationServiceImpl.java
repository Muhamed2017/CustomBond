package com.custombond.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.custombond.auth.DXC_Auth;
import com.custombond.dto.request.DXCQuotePreparationRequest;
import com.custombond.dto.response.DXCQuotePreparationApiErrorResponse;
import com.custombond.dto.response.DXCQuotePreparationResponse;
import com.custombond.entity.Enums;
import com.custombond.exception.DXCQuotePraparationApiBadRequestException;
import com.custombond.exception.DXCQuotePraparationApiException;
import com.custombond.service.DXCQuotePreparationService;
import com.custombond.service.Logger;
import com.custombond.util.JsonUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class DXCQuotePreparationServiceImpl implements DXCQuotePreparationService {

    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    @Autowired
    DXC_Auth dxc_Auth;

    @Autowired
    Logger logger;

    @Autowired
    JsonUtils jsonConverter;

    @Value("${app.external.quoteprepare-url}")
    private String quotePreparationPath;

    @Override
    public DXCQuotePreparationResponse quotePrepare(DXCQuotePreparationRequest request) {
        return doQuotePrepare(request, null, null, null);
    }

    @Override
    public DXCQuotePreparationResponse quotePrepare(DXCQuotePreparationRequest request,
                                                     String parentRequestId,
                                                     String vendorId,
                                                     String vendorRequestId) {
        return doQuotePrepare(request, parentRequestId, vendorId, vendorRequestId);
    }

    // -----------------------------------------------------------------------
    // Core implementation
    // -----------------------------------------------------------------------

    private DXCQuotePreparationResponse doQuotePrepare(DXCQuotePreparationRequest request,
                                                        String parentRequestId,
                                                        String vendorId,
                                                        String vendorRequestId) {
        String token = dxc_Auth.getDxcToken();
        String requestJson = jsonConverter.toJson(request);

        // Log the outbound request to DB when correlation params are available
        UUID parentUuid = null;
        String dbRequestId = null;
        if (parentRequestId != null) {
            try {
                parentUuid = UUID.fromString(parentRequestId);
            } catch (IllegalArgumentException e) {
                log.warn("[QuotePreparation] parentRequestId '{}' is not a valid UUID – skipping DB log", parentRequestId);
            }
            if (parentUuid != null) {
                Object rid = logger.logRequest(
                        "QuotePreparation",
                        requestJson,
                        parentUuid,
                        quotePreparationPath,
                        Enums.MethodType.POST.toString(),
                        vendorId,
                        Enums.CallType.FIRST_CALL.toString(),
                        requestJson,
                        vendorRequestId
                ).get("requestId");
                dbRequestId = rid != null ? rid.toString() : null;
            }
        }

        DXCQuotePreparationResponse response = restClient.post()
                .uri(quotePreparationPath)
                .contentType(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .body(request)
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, (httpRequest, httpResponse) -> {
                    try {
                        DXCQuotePreparationApiErrorResponse errorResponse = objectMapper.readValue(
                                httpResponse.getBody(), DXCQuotePreparationApiErrorResponse.class);

                        log.warn("[QuotePreparation] External API returned 4xx [status={}]: {}",
                                httpResponse.getStatusCode(), errorResponse);

                        if (httpResponse.getStatusCode() == HttpStatus.BAD_REQUEST) {
                            throw new DXCQuotePraparationApiBadRequestException(errorResponse);
                        }

                        throw new DXCQuotePraparationApiException(
                                "Client error from external API: " + errorResponse.getTitle(),
                                httpResponse.getStatusCode().value());

                    } catch (DXCQuotePraparationApiBadRequestException | DXCQuotePraparationApiException ex) {
                        throw ex;
                    } catch (Exception ex) {
                        throw new DXCQuotePraparationApiException(
                                "Failed to parse error response from external API",
                                httpResponse.getStatusCode().value());
                    }
                })
                .onStatus(HttpStatusCode::is5xxServerError, (httpRequest, httpResponse) -> {
                    log.error("[QuotePreparation] External API returned 5xx [status={}]", httpResponse.getStatusCode());
                    throw new DXCQuotePraparationApiException(
                            "External policy API is currently unavailable",
                            httpResponse.getStatusCode().value());
                })
                .body(DXCQuotePreparationResponse.class);

        // Log the response to DB
        if (dbRequestId != null) {
            logger.logResponse(jsonConverter.toJson(response), dbRequestId);
        }

        log.info("[QuotePreparation] Policy quote prepared: policyKey={}, policyNo={}",
                response.getPolicyKey(), response.getPolicyNo());
        return response;
    }
}
