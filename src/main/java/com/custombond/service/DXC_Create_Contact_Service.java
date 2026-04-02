package com.custombond.service;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

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
import com.custombond.dto.request.CreateContactRequest;
import com.custombond.dto.request.ContactData.IndividualContactData;
import com.custombond.dto.request.ContactData.OrganizationContactData;
import com.custombond.entity.Enums;
import com.custombond.util.JsonUtils;

@Service
public class DXC_Create_Contact_Service {

        @Value("${app.external.create_contact-individual-url}")
        String CreateIndividualContactURL;
        @Value("${app.external.create_contact-organizaion-url}")
        String CreateOrganizaionContactURL;

        @Autowired
        private RestTemplate restTemplate;

        @Autowired
        DXC_Auth DXC_auth;

        @Autowired
        Logger logger;

        @Autowired
        JsonUtils JsonConverter;

        private ResponseEntity<Map<String, Object>> createIndividualContact(String Token,
                        CreateContactRequest Request) {
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);
                headers.setBearerAuth(Token);
                HttpEntity<IndividualContactData> entity = new HttpEntity<>(Request.getIndividualContactData(),
                                headers);
                String requestId = Optional.ofNullable(
                                logger.logRequest(
                                                "CreateContact-Individual",
                                               JsonConverter.toJson(entity),
                                                  UUID.fromString(Request.getParentRequestId()),
                                                CreateIndividualContactURL,
                                                Enums.MethodType.POST.toString(),
                                                Request.getVendorId(),
                                                Request.getCallSequance().toString(),JsonConverter.toJson(Request),Request.getVendorRequestId()).get("requestId"))
                                .map(Object::toString).orElse(null);
                ResponseEntity<Map<String, Object>> DXC_Response = restTemplate.exchange(
                                CreateIndividualContactURL,
                                HttpMethod.POST,
                                entity,
                                new ParameterizedTypeReference<Map<String, Object>>() {
                                });
                DXC_auth.Logout(Token);
                logger.logResponse(JsonConverter.toJson(DXC_Response), requestId);
                DXC_Response.getBody().put("requestId", requestId);
                return DXC_Response;
        }

        private ResponseEntity<Map<String, Object>> createOrganizationContact(String Token,
                        CreateContactRequest Request) {
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);
                headers.setBearerAuth(Token);
                HttpEntity<OrganizationContactData> entity = new HttpEntity<>(Request.getOrganizationContactData(),
                                headers);
                String requestId = Optional.ofNullable(
                                logger.logRequest(
                                                "CreateContact-Organization",
                                               JsonConverter.toJson(entity),
                                                UUID.fromString(Request.getParentRequestId()),
                                                CreateOrganizaionContactURL,
                                                Enums.MethodType.POST.toString(),
                                                Request.getVendorId(),
                                                Request.getCallSequance().toString(),JsonConverter.toJson(Request),Request.getVendorRequestId()).get("requestId"))
                                .map(Object::toString).orElse(null);
                ResponseEntity<Map<String, Object>> DXC_Response = restTemplate.exchange(
                                CreateOrganizaionContactURL,
                                HttpMethod.POST,
                                entity,
                                new ParameterizedTypeReference<Map<String, Object>>() {
                                });
                DXC_auth.Logout(Token);
                logger.logResponse(JsonConverter.toJson(DXC_Response), requestId);
                DXC_Response.getBody().put("requestId", requestId);
                return DXC_Response;
        }

        public ResponseEntity<Map<String, Object>> createContact(CreateContactRequest Request) {

                if (Request.getIndividualContactData() != null) {
                        return createIndividualContact(DXC_auth.getDxcToken(), Request);
                } else {
                        return createOrganizationContact(DXC_auth.getDxcToken(), Request);
                }
        }

}
