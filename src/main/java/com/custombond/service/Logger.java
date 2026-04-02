package com.custombond.service;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.custombond.dto.request.BlackListRequest;
import com.custombond.dto.response.BlackListResponse;
import com.custombond.entity.CBDocument;
import com.custombond.entity.CBGeneralLog;
import com.custombond.entity.Enums;
import com.custombond.helper.Validation_Helper;
import com.custombond.repository.CBDocumentRepository;
import com.custombond.repository.CBGeneralLogRepository;
import com.custombond.util.AppStringUtils;
import com.custombond.util.JsonUtils;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class Logger {

    @Autowired
    CBGeneralLogRepository cGeneralLogRepository;

    @Autowired
    CBDocumentRepository cbDocumentRepository;

    @Autowired
    JsonUtils JsonConverter;

    @Autowired
    Validation_Helper validation_Helper;

    public Map<String, Object> log(String ApiName, String RequestBody, String ResponseBody, UUID ParentRequestId,
            String URL,
            String MethodType, String VendorId, String callType,String vendorRequestBody,String vendorRequestId) {

        Map<String, Object> ret = new HashMap<>();
        try {
            ;
            ret.put("isSaved", true);
            ret.put("log", cGeneralLogRepository
                    .save(buildLog(ApiName, RequestBody, ResponseBody, ParentRequestId, URL, MethodType, VendorId,
                            callType,vendorRequestBody,vendorRequestId)));
            return ret;
        } catch (Exception e) {
            System.out.println(e.toString());
            ret.put("isSaved", false);
            ret.put("log", null);
            return ret;
        }

    }

    public CBGeneralLog buildLog(String ApiName, String RequestBody, String ResponseBody, UUID ParentRequestId,
            String URL, String MethodType, String VendorId, String callType,String vendorRequestBody,String vendorRequestId) {
        CBGeneralLog cbGeneralLog = new CBGeneralLog();
        cbGeneralLog.setApiName(ApiName);
        cbGeneralLog.setRequestBody(RequestBody);
        cbGeneralLog.setParentRequestId(ParentRequestId);
        cbGeneralLog.setResponseBody(ResponseBody);
        cbGeneralLog.setUrl(URL);
        cbGeneralLog.setMethodType(MethodType);
        if (callType != null) {
            cbGeneralLog.setCallType(Enums.CallType.valueOf(callType.toUpperCase()));
        }
        cbGeneralLog.setVendorId(VendorId);
        cbGeneralLog.setVendorRequestBody(vendorRequestBody);
        cbGeneralLog.setVendorRequestId(vendorRequestId);
        return cbGeneralLog;
    }

    public CBGeneralLog buildRequestLog(String ApiName, String RequestBody, String ResponseBody, UUID ParentRequestId,
            String URL, String MethodType, String VendorId, String callType,String vendorRequestBody,String vendorRequestId) {
        CBGeneralLog cbGeneralLog = new CBGeneralLog();
        cbGeneralLog.setApiName(ApiName);
        cbGeneralLog.setRequestBody(RequestBody);
        cbGeneralLog.setParentRequestId(ParentRequestId);
        cbGeneralLog.setResponseBody(ResponseBody);
        cbGeneralLog.setUrl(URL);
        cbGeneralLog.setMethodType(MethodType);
        cbGeneralLog.setVendorId(VendorId);
        cbGeneralLog.setResponseDateTime(null);
        if (callType != null) {
            cbGeneralLog.setCallType(Enums.CallType.valueOf(callType.toUpperCase()));
        }
        cbGeneralLog.setVendorRequestBody(vendorRequestBody);
        cbGeneralLog.setVendorRequestId(vendorRequestId);
        return cbGeneralLog;
    }

    public CBGeneralLog buildResponseLog(String ResponseBody, String RequestId) {
        CBGeneralLog cbGeneralLog = cGeneralLogRepository.findByRequestId(UUID.fromString(RequestId)).get(0);
        cbGeneralLog.setResponseBody(ResponseBody);
        return cbGeneralLog;
    }

    public Map<String, Object> logRequest(String ApiName, String RequestBody, UUID ParentRequestId, String URL,
            String MethodType, String VendorId, String callType,String vendorRequestBody,String vendorRequestId) {

        Map<String, Object> DBResponse = new HashMap<>();
        try {

            DBResponse.put("isAdded", true);
            DBResponse.put("requestId",
                    cGeneralLogRepository
                            .save(buildRequestLog(ApiName, RequestBody, null, ParentRequestId, URL, MethodType,
                                    VendorId, callType,vendorRequestBody,vendorRequestId))
                            .getRequestId());

            return DBResponse;
        } catch (Exception e) {
            DBResponse.put("isAdded", false);
            DBResponse.put("requestId", null);
            System.out.println(e.toString());
            return DBResponse;
        }

    }

    public boolean logResponse(String ResponseBody, String RequestId) {
        try {

            cGeneralLogRepository.save(buildResponseLog(ResponseBody, RequestId));
            return true;
        } catch (Exception e) {
            System.out.println(e.toString());
            return false;
        }

    }

    public String prepareGETRequestBody(String url, HttpEntity<?> entity) {
        return "URL=" + url +
                " | METHOD=GET" +
                " | HEADERS=" + entity.getHeaders().toString();
    }

    public ResponseEntity<BlackListResponse> blackListResponse(
            ResponseEntity<Map<String, Object>> DXC_Response,
            String requestId,
            BlackListRequest Request) {
        BlackListResponse Response = new BlackListResponse();
        Map<String, Object> Body = DXC_Response.getBody();

        Map<String, Object> validationResult = validation_Helper.validateId(Request);

        if ((Integer) validationResult.get("Type") == 2) {
            Response.setTaxId(validationResult.get("Id").toString());
        }

        Response.setId(validationResult.get("Id"));
        Response.setStakeholderName(Request.getStakeholderName());
        Response.setCheckedAt(LocalDateTime.now());
        Response.setCachedResultTtlSeconds(3600);
        Response.setErrorMessage("");
        ResponseEntity<BlackListResponse> FinalResponse;

        if (DXC_Response.getStatusCode() == HttpStatus.NOT_FOUND) {
            Response.setMaskedStakeholderName(null);
            Response.setExisted(false);
            Response.setStatus(Enums.Status.APPROVED);
            Response.setErrorMessage("TaxId is not found");
            FinalResponse = ResponseEntity.status(HttpStatus.NOT_FOUND).body(Response);

        } else {
            boolean blacklisted = Boolean.TRUE.equals(Body != null ? Body.get("blacklisted") : null);
            Response.setMaskedStakeholderName(
                    Body != null
                            ? AppStringUtils.maskNameWithFirstLetter(
                                    Body.get("companyName") != null
                                            ? (String) Body.get("companyName")
                                            : (String) Body.get("contactReference"))
                            : null);

            Response.setExisted(true);
            if (!blacklisted) {
                Response.setStatus(Enums.Status.APPROVED);
                Response.setContactKey((Integer)DXC_Response.getBody().get("key"));
                Response.setRejectionReasonAr(null);
                Response.setRejectionCode(null);

            } else {

                Response.setStatus(Enums.Status.REJECTED);
                Response.setRejectionReasonAr("");
                Response.setRejectionCode(null);

            }

            FinalResponse = ResponseEntity.ok(Response);
        }

        logResponse(JsonConverter.toJson(DXC_Response), requestId);

        return FinalResponse;
    }

    public Boolean logDocument(MultipartFile File, CBGeneralLog log, String response) {
        CBDocument document = new CBDocument();
        ObjectMapper mapper = new ObjectMapper();
        Integer documentKey = null;
        try {
            documentKey = Integer.valueOf(mapper.readValue(response, Map.class).get("documentKey").toString());
        } catch (JsonProcessingException e) {
            documentKey = null;
            e.printStackTrace();
        }
        try {
            document.setRequestId(log.getRequestId());
            document.setDocumentData(File.getBytes());
            document.setContentType(File.getContentType());
            document.setDocumentName(File.getOriginalFilename());
            document.setDocumentSize(File.getSize());
            document.setUploadDate(LocalDateTime.now());
            document.setDocumentKey(documentKey);
            cbDocumentRepository.save(document);
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }

    }

    public CBDocument getDocument(UUID RequestId) {

        try {
            CBDocument document = cbDocumentRepository.findByRequestId(RequestId).get(0);
            return document;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }

    }

}
