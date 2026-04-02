package com.custombond.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.custombond.auth.DXC_Auth;
import com.custombond.dto.request.DXCQuotePreparationRequest;
import com.custombond.dto.response.DXCQuotePreparationApiErrorResponse;
import com.custombond.dto.response.DXCQuotePreparationResponse;
import com.custombond.exception.DXCQuotePraparationApiBadRequestException;
import com.custombond.exception.DXCQuotePraparationApiException;
import com.custombond.service.DXCQuotePreparationService;
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

@Slf4j
@Service
@RequiredArgsConstructor
public class DXCQuotePreparationServiceImpl implements DXCQuotePreparationService {

    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    @Autowired
    DXC_Auth dxc_Auth;

    @Value("${app.external.quoteprepare-url}")
    private String quotePreparationPath;

    @Override
    public DXCQuotePreparationResponse quotePrepare(DXCQuotePreparationRequest request) {

        String Token = dxc_Auth.getDxcToken();

        // log.info("Sending policy issuance request to external API: insured={}, product={}, division={}",
        //         request.getInsured(), request.getProduct(), request.getDivision());

        System.out.println("Token is :" +  Token);

        DXCQuotePreparationResponse response = restClient.post()
                .uri(quotePreparationPath)
                .contentType(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + Token)
                .body(request)
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, (httpRequest, httpResponse) -> {
                    try {
                        DXCQuotePreparationApiErrorResponse errorResponse = objectMapper.readValue(
                                httpResponse.getBody(), DXCQuotePreparationApiErrorResponse.class);

                        log.warn("External API returned 4xx [status={}]: {}",
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
                    log.error("External API returned 5xx [status={}]", httpResponse.getStatusCode());
                    throw new DXCQuotePraparationApiException(
                            "External policy API is currently unavailable",
                            httpResponse.getStatusCode().value());
                })
                .body(DXCQuotePreparationResponse.class);

        log.info("Policy issued successfully: policyKey={}, policyNo={}",
                response.getPolicyKey(), response.getPolicyNo());
        return response;
    }
}
