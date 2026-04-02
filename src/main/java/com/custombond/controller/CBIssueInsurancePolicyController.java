package com.custombond.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.custombond.dto.request.CB_IssuePolicyRequest;
import com.custombond.service.PolicyIssuingServiceTest;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;

@RestController
@RequestMapping("/cb/services")
@Slf4j
public class CBIssueInsurancePolicyController {
    @Autowired
    PolicyIssuingServiceTest policyIssuingService;

    @PostMapping(value = "/policies", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> IssuingPolicyFullCycle(
            @RequestPart("file") MultipartFile file,
            @RequestPart("data") CB_IssuePolicyRequest data) {
        return policyIssuingService.startProcess(file, data);
    }

}
