package com.custombond.service;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import org.springframework.web.client.RestTemplate;
import com.custombond.auth.DXC_Auth;
import com.custombond.dto.request.IssuePolicyRequest;
import com.custombond.entity.Enums;
import com.custombond.util.JsonUtils;

@Service
public class DXC_IssuePolicy_Service {

    @Value("${app.external.issuepolicy-url}")
    String IssuePolicyURL;

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    DXC_Auth DXC_auth;

    @Autowired
    JsonUtils JsonConverter;

    @Autowired
    Logger logger;

    public ResponseEntity<Map<String, Object>> issuePolicy(IssuePolicyRequest Request) {
        Map<String, Object> Response = new HashMap<>();

        HttpEntity<Void> entity = new HttpEntity<>(prepareRequestHeaders());

        Map<String, Object> log = logger.logRequest("IssuePolicy",
                logger.prepareGETRequestBody((IssuePolicyURL + Request.getPolicyNo()), entity),
                UUID.fromString(Request.getParentRequestId()), (IssuePolicyURL + Request.getPolicyNo()),
                Enums.MethodType.POST.toString(), Request.getVendorId(), Enums.CallType.SECOND_CALL.toString(),JsonConverter.toJson(Request),Request.getVendorRequestId());

        ResponseEntity<Map<String, Object>> DXC_Response = restTemplate.exchange(
                IssuePolicyURL + Request.getPolicyNo(),
                HttpMethod.POST,
                entity,
                new ParameterizedTypeReference<Map<String, Object>>() {
                });

        logger.logResponse(JsonConverter.toJson(DXC_Response), log.get("requestId").toString());
        Response.putAll(DXC_Response.getBody());
        if (DXC_Response.getStatusCode() == HttpStatus.NOT_FOUND) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Response);
        } else if (DXC_Response.getStatusCode() == HttpStatus.BAD_REQUEST) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Response);
        } else {
            return ResponseEntity.ok(Response);
        }
    }

    public HttpHeaders prepareRequestHeaders()
    {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(DXC_auth.getDxcToken());
        return headers;
    }

}
