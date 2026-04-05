package com.custombond.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

@Service
public class Email_Service {

    @Value("${app.external.email-service-url}")
    private String SendEmailURL;

    @Autowired
    private RestTemplate restTemplate;

    public String sendEmailGeneric(String Title, String content) {
        // Prepare form-data
        MultiValueMap<String, Object> formData = new LinkedMultiValueMap<>();

        // Add fields
        formData.add("htmlBody",
                "<html><body><h2>" + content + "</h2>" +
                        "</body></html>");
        formData.add("subject", Title);
        formData.add("setTo[]", "f.ragab@misrins.com.eg");
        formData.add("setCc[]", "ah.ash.hamza@misrins.com.eg");

        // Headers
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        // Build request
        HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(formData, headers);

        // Send POST request
        ResponseEntity<String> response = restTemplate.postForEntity(SendEmailURL, requestEntity, String.class);

        return "Status: " + response.getStatusCode() + " | Body: " + response.getBody();
    }

}
