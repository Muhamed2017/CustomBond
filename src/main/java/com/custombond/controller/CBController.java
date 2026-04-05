package com.custombond.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.custombond.dto.request.CB_IssuePolicyRequest;
import com.custombond.service.PolicyIssuingServiceTest;

import lombok.extern.slf4j.Slf4j;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;

@RestController
@RequestMapping("/cb/services")
@Slf4j
public class CBController {
    @Autowired
    PolicyIssuingServiceTest policyIssuingService;

    @PostMapping(value = "/policies", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> IssuingPolicyFullCycle(
            @RequestPart("attachments") List<MultipartFile> attachments,
            @RequestPart("data") CB_IssuePolicyRequest data) {
        return policyIssuingService.startProcess(attachments, data);
    }

    // @PostMapping(value = "/policies", consumes =
    // MediaType.MULTIPART_FORM_DATA_VALUE)
    // public ResponseEntity<?> IssuingPolicyFullCycle(
    // @RequestPart("attachments") List<MultipartFile> attachments ,
    // @RequestPart("data") CB_IssuePolicyRequest data) {
    // return policyIssuingService.startProcess(attachments, data);
    // }


}
