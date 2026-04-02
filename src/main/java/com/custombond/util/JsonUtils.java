package com.custombond.util;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;

@Component
public class JsonUtils {
    @Autowired
    private ObjectMapper objectMapper;

    public String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            return "JSON_CONVERSION_ERROR: " + e.getMessage();
        }
    }

     public Object extractId(Map<String, String> Request) {
        String nationalId = Request.get("nationalId") != null
                ? Request.get("nationalId").toString()
                : null;

        String taxId = Request.get("taxId") != null
                ? Request.get("taxId").toString()
                : null;

        Integer contactKey = Request.get("contactKey") != null
                ? Integer.valueOf(Request.get("contactKey").toString())
                : null;

        if (nationalId != null) {
            return nationalId;
        } else if (taxId != null) {
            return taxId;
        } else if (contactKey != null) {
            return contactKey;
        } else {
            return null;
        }
    }
}
