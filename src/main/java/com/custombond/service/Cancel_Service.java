package com.custombond.service;


import java.time.LocalDateTime;

import java.util.Map;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import com.custombond.auth.DXC_Auth;
import com.custombond.dto.request.CancelPolicyRequest;

import com.custombond.dto.response.CB_AsyncResponse;
import com.custombond.entity.Enums;
import com.custombond.util.JsonUtils;

@Service
public class Cancel_Service {

    @Autowired
    DXC_Auth DXC_auth;

    @Autowired
    Logger logger;

    @Autowired
    JsonUtils JsonConverter;

    @Autowired
    Email_Service emailService;

    public ResponseEntity<CB_AsyncResponse> cancelPolicy(CancelPolicyRequest Request) {

        Map<String, Object> log = logger.logRequest("CancelPolicy", JsonConverter.toJson(Request),
                UUID.fromString(Request.getParentRequestId()), "/CancelPolicy", Enums.MethodType.POST.toString(),
                Request.getVendorId(), Enums.CallType.FIRST_CALL.toString(), JsonConverter.toJson(Request),
                Request.getVendorRequestId());

        logger.logResponse(
                emailService
                        .sendEmailGeneric("Cancellation Request Received for PolicyNo: " + Request.getPolicyNo(),
                                "Cancellation request received for PolicyNo: " + Request.getPolicyNo()
                                        + " with requestId: " + Request.getVendorRequestId()),
                log.get("requestId").toString());

        CB_AsyncResponse response = new CB_AsyncResponse(Request.getVendorRequestId(), log.get("requestId").toString(),
                LocalDateTime.now());

        return ResponseEntity.ok(response);
    }
}
