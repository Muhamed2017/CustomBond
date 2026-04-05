package com.custombond.service;

import java.io.IOException;
import java.math.BigInteger;
import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import com.custombond.auth.DXC_Auth;
import com.custombond.dto.request.UploadRequest;
import com.custombond.dto.response.UploadDocumentResponse;
import com.custombond.dto.response.UploadDocumentResponse.Error;
import com.custombond.entity.CBGeneralLog;
import com.custombond.entity.Enums;
import com.custombond.util.JsonUtils;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class DXC_UploadDocument_Service {
        @Value("${app.external.upload-url}")
        String UploadDocumentURL;

        @Autowired
        private RestTemplate restTemplate;

        @Autowired
        DXC_Auth DXC_auth;

        @Autowired
        Logger logger;

        @Autowired
        JsonUtils JsonConverter;

        public ResponseEntity<?> uploadDocument(MultipartFile file, String data) throws IOException {
                ObjectMapper mapper = new ObjectMapper();
                UploadRequest uploadRequest = mapper.readValue(data, UploadRequest.class);
                Integer entityKey = uploadRequest.getEntityKey();
                BigInteger instnceKey = uploadRequest.getInstnceKey();
                Integer documentType = uploadRequest.getDocumentType();
                String vendorRequestId = uploadRequest.getVendorRequestId();
                String parentRequestId = uploadRequest.getParentRequestId();
                UUID UUIDparentRequestId = parentRequestId != null ? UUID.fromString(parentRequestId) : null;
                String vendorId = uploadRequest.getVendorId();

                MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
                body.add("EntityKey", entityKey);
                body.add("InstnceKey", instnceKey);
                body.add("DocumentType", documentType);
                body.add("File", new ByteArrayResource(file.getBytes()) {
                        @Override
                        public String getFilename() {
                                return file.getOriginalFilename();
                        }
                });

                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.MULTIPART_FORM_DATA);
                headers.setBearerAuth(DXC_auth.getDxcToken());
                HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);
                ResponseEntity<String> response = restTemplate.postForEntity(UploadDocumentURL, requestEntity,
                                String.class);

                logger
                                .logDocument(file,
                                                (CBGeneralLog) logger
                                                                .log("UploadDocument", body.toString(),
                                                                                JsonConverter.toJson(response),
                                                                                UUIDparentRequestId,
                                                                                UploadDocumentURL,
                                                                                Enums.MethodType.POST.toString(),
                                                                                vendorId,
                                                                                Enums.CallType.SECOND_CALL.toString(),
                                                                                body.toString(), vendorRequestId)
                                                                .get("log"),
                                                response.getBody());
                return ResponseEntity.status(response.getStatusCode())
                                .body(response.getBody());
        }

        public ResponseEntity<?> uploadDocuments(List<MultipartFile> files, String data) throws IOException {
                Boolean UploadSuccess = true;
                UploadDocumentResponse FinalResponse = new UploadDocumentResponse();
                
                ObjectMapper mapper = new ObjectMapper();
                UploadRequest uploadRequest = mapper.readValue(data, UploadRequest.class);
                Integer entityKey = uploadRequest.getEntityKey();
                BigInteger instnceKey = uploadRequest.getInstnceKey();
                Integer documentType = uploadRequest.getDocumentType();
                String vendorRequestId = uploadRequest.getVendorRequestId();
                String parentRequestId = uploadRequest.getParentRequestId();
                UUID UUIDparentRequestId = parentRequestId != null ? UUID.fromString(parentRequestId) : null;
                String vendorId = uploadRequest.getVendorId();

                MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
                body.add("EntityKey", entityKey);
                body.add("InstnceKey", instnceKey);
                body.add("DocumentType", documentType);

                for (int i = 0; i < files.size(); i++) {
                        MultipartFile file = files.get(i);
                        body.add("File", new ByteArrayResource(file.getBytes()) {
                                @Override
                                public String getFilename() {
                                        return file.getOriginalFilename();
                                }
                        });

                        HttpHeaders headers = new HttpHeaders();
                        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
                        headers.setBearerAuth(DXC_auth.getDxcToken());
                        HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);
                        ResponseEntity<String> response = restTemplate.postForEntity(UploadDocumentURL, requestEntity,
                                        String.class);
                        try {

                                logger
                                                .logDocument(file,
                                                                (CBGeneralLog) logger
                                                                                .log("UploadDocument", body.toString(),
                                                                                                JsonConverter.toJson(
                                                                                                                response),
                                                                                                UUIDparentRequestId,
                                                                                                UploadDocumentURL,
                                                                                                Enums.MethodType.POST
                                                                                                                .toString(),
                                                                                                vendorId,
                                                                                                Enums.CallType.SECOND_CALL
                                                                                                                .toString(),
                                                                                                body.toString(),
                                                                                                vendorRequestId)
                                                                                .get("log"),
                                                                response.getBody());

                                if (!response.getStatusCode().is2xxSuccessful()) {
                                        UploadSuccess = false;
                                        FinalResponse.setMessage(
                                                        "Failed to upload document: " + file.getOriginalFilename());
                                        Error error = new Error();
                                        error.setCode("001");
                                        error.setMessage("Failed to upload document: " + file.getOriginalFilename());
                                        FinalResponse.getErrors().add(error);
                                        FinalResponse.setUploaded(UploadSuccess);
                                }
                        } catch (Exception e) {
                                UploadSuccess = false;
                                FinalResponse.setMessage(
                                                "Failed to upload document: " + file.getOriginalFilename());
                                Error error = new Error();
                                error.setCode("003");
                                error.setMessage("Exception occurred while logging document: " + e.getMessage());
                                FinalResponse.getErrors().add(error);
                                FinalResponse.setUploaded(UploadSuccess);
                        }

                }
                return ResponseEntity.ok(FinalResponse);
                // return ResponseEntity.status(response.getStatusCode())
                // .body(response.getBody());
        }

}
