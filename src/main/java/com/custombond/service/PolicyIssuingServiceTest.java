package com.custombond.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.custombond.dto.request.CB_IssuePolicyRequest;
import com.custombond.dto.response.CB_AsyncResponse;

@Service
public class PolicyIssuingServiceTest {

    @Autowired
    AsyncPolicyIssuingServiceTest asyncPolicyIssuingServiceTest;
    public ResponseEntity<?> startProcess(List<MultipartFile> attachments, CB_IssuePolicyRequest data) {
        asyncPolicyIssuingServiceTest.AsyncFullCycle_CB(data);

        return ResponseEntity.ok().body(new CB_AsyncResponse(
                data.getNafezaInsuranceRequestId(),
                "test-request-id",
                LocalDateTime.parse("2024-06-01T12:00:00")));
    }
    
}
