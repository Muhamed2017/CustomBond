package com.custombond.controller;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.custombond.auth.DXC_Auth;
import com.custombond.dto.request.BlackListRequest;
import com.custombond.dto.request.CreateContactRequest;
import com.custombond.dto.request.DXCQuotePreparationRequest;
import com.custombond.dto.request.GetContactRequest;
import com.custombond.dto.request.IssuePolicyRequest;
import com.custombond.dto.request.IssueQuoteRequest;
import com.custombond.dto.response.ApiResponse;
import com.custombond.dto.response.DXCQuotePreparationResponse;
import com.custombond.helper.DB_Helper;
import com.custombond.service.DXCQuotePreparationService;
import com.custombond.service.DXC_BlackList_Service;
import com.custombond.service.DXC_Create_Contact_Service;
import com.custombond.service.DXC_GetContact_Service;
import com.custombond.service.DXC_IssuePolicy_Service;
import com.custombond.service.DXC_IssueQuote_Service;
import com.custombond.service.DXC_UploadDocument_Service;


import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;

@RestController
@RequestMapping("/dxc/services")
@Slf4j
public class DxcController {

    @Autowired
    DXC_Auth DXC_auth;

    @Autowired
    DXC_BlackList_Service DXC_BlackList_service;

    @Autowired
    DXC_IssueQuote_Service DXC_IssueQuote_service;

    @Autowired
    DXC_IssuePolicy_Service DXC_IssuePolicy_service;

    @Autowired
    DXC_UploadDocument_Service DXC_UploadDocument_service;

    @Autowired
    DXC_GetContact_Service DXC_GetContact_service;

    @Autowired
    DXC_Create_Contact_Service DXC_Create_Contact_service;

    @Autowired
    DB_Helper db_Helper;

    @Autowired
    DXCQuotePreparationService policyIssuanceService;

    // check user in blacklist or not by searching by TaxId
    @PostMapping("/checkBlackListed")
    public Object checkBlackListed(@RequestBody BlackListRequest Request) {
        return DXC_BlackList_service.checkBlackList(Request);
    }

    // call IssueQuote from DXC (the second step after Quote Preparation)
    @PostMapping("/issueQuote")
    public ResponseEntity<Map<String, Object>> issueQuote(@RequestBody IssueQuoteRequest Request) {
        return DXC_IssueQuote_service.issueQoute(Request);
    }

    // call IssuePolicy from DXC (the third step after Issue Quote)
    @PostMapping("/issuePolicy")
    public ResponseEntity<Map<String, Object>> issuePolicy(@RequestBody IssuePolicyRequest Request) {
        return DXC_IssuePolicy_service.issuePolicy(Request);
    }

    // upload one document to specefic policy by policy key
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> upload(@RequestPart("File") MultipartFile file, @RequestPart("Data") String data)
            throws IOException {
        return DXC_UploadDocument_service.uploadDocument(file, data);
    }

    // get document by request id
    @PostMapping("/getDocument")
    public ResponseEntity<?> getDocument(@RequestBody Map<String, String> Request) {
        return db_Helper.getDocumentByRequestID(UUID.fromString(Request.get("requestId")));
    }

    @PostMapping("/quotePrepration")
    public ResponseEntity<ApiResponse<DXCQuotePreparationResponse>> quotePreparation(
            @Valid @RequestBody DXCQuotePreparationRequest request) {

        // log.info("Received policy issuance request: insured={}, product={}, division={}",
        //         request.getInsured(), request.getProduct(), request.getDivision());

        DXCQuotePreparationResponse response = policyIssuanceService.quotePrepare(request);

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    // get contact by taxid or nationalid or contact key
    @PostMapping("/getContact")
    public ResponseEntity<?> getContact(@RequestBody GetContactRequest Request) {
        return DXC_GetContact_service.getContact(Request);
    }

    // create individual or organization contact
    @PostMapping("/createContact")
    public ResponseEntity<?> createContact(@RequestBody CreateContactRequest Request) {
        return DXC_Create_Contact_service.createContact(Request);
    }

}
