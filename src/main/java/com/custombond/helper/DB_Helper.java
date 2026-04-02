package com.custombond.helper;

import com.custombond.repository.CBGeneralLogRepository;
import com.custombond.service.Logger;

import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import com.custombond.auth.DXC_Auth;

import com.custombond.entity.CBDocument;
import com.custombond.entity.CBGeneralLog;
import com.custombond.util.JsonUtils;

/*
Service class used to access database related helper operations.

Responsibilities
- Retrieve stored documents by RequestId
- Retrieve logs stored in CB_General_Log table
*/

@Service
public class DB_Helper {
    private final CBGeneralLogRepository CBGeneralLogRepository; 

    @Autowired
    DXC_Auth DXC_auth;

    @Autowired
    Logger logger;

    @Autowired
    JsonUtils JsonConverter;

    DB_Helper(CBGeneralLogRepository CBGeneralLogRepository) {
        this.CBGeneralLogRepository = CBGeneralLogRepository;
    }

    public ResponseEntity<?> getDocumentByRequestID(UUID requestId) {
        CBDocument document = logger.getDocument(requestId);
        try {
            ByteArrayResource resource = new ByteArrayResource(document.getDocumentData());
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=\"" + document.getDocumentName() + "\"")
                    .contentType(MediaType.parseMediaType(document.getContentType()))
                    .contentLength(document.getDocumentSize())
                    .body(resource);
        } catch (Exception e) {
            // String ApiName, String RequestBody, UUID ParentRequestId, String URL,
            // String MethodType, String VendorId, String parentRequest
            // cb_Logger.logRequest("", LoginURL, requestId, IssueQouteURL, IssuePolicyURL,
            // GetContactURL, BasicURL)
            return ResponseEntity.ok().body("There is No Files");
        }

    }

    public CBGeneralLog getLogByRequestId(UUID requestId) {
        return CBGeneralLogRepository.findByRequestId(requestId).get(0);
    }

}
