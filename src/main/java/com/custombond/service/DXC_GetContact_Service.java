package com.custombond.service;

import java.util.Map;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import org.springframework.web.client.RestTemplate;


import com.custombond.auth.DXC_Auth;
import com.custombond.dto.request.GetContactRequest;
import com.custombond.entity.Enums;
import com.custombond.util.JsonUtils;

@Service
public class DXC_GetContact_Service {
    @Value("${app.external.getcontact-taxid-url}")
    String GetContactByTaxIdURL;
    @Value("${app.external.getcontact-nationalid-url}")
    String GetContactByNationalIdURL;
    @Value("${app.external.getcontact-contact-url}")
    String GetContactByContactURL;


    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    DXC_Auth DXC_auth;

    @Autowired
    Logger logger;

    @Autowired
    JsonUtils JsonConverter;

    private ResponseEntity<Map<String, Object>> getContactByTaxId(String Token, GetContactRequest Request) {
        String Id = Request.getTaxId();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(Token);
        HttpEntity<Void> entity = new HttpEntity<>(headers);
        String requestId = Optional.ofNullable(
                logger.logRequest(
                        "GetContactByTaxId",
                        logger.prepareGETRequestBody((GetContactByTaxIdURL + Id), entity),
                        null,
                        (GetContactByTaxIdURL + Id),
                        Enums.MethodType.GET.toString(),
                        Request.getVendorId(),
                        Request.getCallSequance().toString(),JsonConverter.toJson(Request),
                        Request.getVendorRequestId()
                ).get("requestId"))
                .map(Object::toString).orElse(null);
        ResponseEntity<Map<String, Object>> DXC_Response = restTemplate.exchange(
                GetContactByTaxIdURL + Id,
                HttpMethod.GET,
                entity,
                new ParameterizedTypeReference<Map<String, Object>>() {
                });
        DXC_auth.Logout(Token);
        logger.logResponse(JsonConverter.toJson(DXC_Response), requestId);
        DXC_Response.getBody().put("requestId", requestId);
        return DXC_Response;
    }

    private ResponseEntity<Map<String, Object>> getContactByNationalId(String Token, GetContactRequest Request) {
        String Id = Request.getNationalId();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(Token);
        HttpEntity<Void> entity = new HttpEntity<>(headers);
        String requestId = Optional.ofNullable(
                logger.logRequest(
                        "GetContactByNationalId",
                        logger.prepareGETRequestBody((GetContactByNationalIdURL + Id), entity),
                        null,
                        (GetContactByNationalIdURL + Id),
                        Enums.MethodType.GET.toString(),
                        Request.getVendorId(),
                        Request.getCallSequance().toString(),JsonConverter.toJson(Request),
                        Request.getVendorRequestId()
                ).get("requestId"))
                .map(Object::toString).orElse(null);
        ResponseEntity<Map<String, Object>> DXC_Response = restTemplate.exchange(
                GetContactByNationalIdURL + Id,
                HttpMethod.GET,
                entity,
                new ParameterizedTypeReference<Map<String, Object>>() {
                });
        DXC_auth.Logout(Token);
        logger.logResponse(JsonConverter.toJson(DXC_Response), requestId);
        DXC_Response.getBody().put("requestId", requestId);
        return DXC_Response;
    }

    private ResponseEntity<Map<String, Object>> getContactByContackKey(String Token, GetContactRequest Request) {
        String Id = Request.getContactKey().toString();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(Token);
        HttpEntity<Void> entity = new HttpEntity<>(headers);
        String requestId = Optional.ofNullable(
                logger.logRequest(
                        "GetContactByContactKey",
                        logger.prepareGETRequestBody((GetContactByContactURL + Id), entity),
                        null,
                        (GetContactByContactURL + Id),
                        Enums.MethodType.GET.toString(),
                        Request.getVendorId(),
                        Request.getCallSequance().toString(),
                        JsonConverter.toJson(Request),
                        Request.getVendorRequestId()
                ).get("requestId"))
                .map(Object::toString).orElse(null);
        ResponseEntity<Map<String, Object>> DXC_Response = restTemplate.exchange(
                GetContactByContactURL + Id,
                HttpMethod.GET,
                entity,
                new ParameterizedTypeReference<Map<String, Object>>() {
                });
        DXC_auth.Logout(Token);
        logger.logResponse(JsonConverter.toJson(DXC_Response), requestId);
        DXC_Response.getBody().put("requestId", requestId);
        return DXC_Response;
    }

    public ResponseEntity<Map<String, Object>> getContact(GetContactRequest Request) {

        String nationalId = Request.getNationalId() != null
                ? Request.getNationalId().toString()
                : null;

        String taxId = Request.getTaxId() != null
                ? Request.getTaxId().toString()
                : null;

        Integer contactKey = Request.getContactKey() != null
                ? Integer.valueOf(Request.getContactKey().toString())
                : null;

        if (nationalId != null) {
            return getContactByNationalId(DXC_auth.getDxcToken(), Request);
        } else if (taxId != null) {
            return getContactByTaxId(DXC_auth.getDxcToken(), Request);
        } else if (contactKey != null) {
            return getContactByContackKey(DXC_auth.getDxcToken(), Request);
        } else {
            return null;
        }

    }

}
