package com.custombond.helper;

import com.custombond.repository.CBGeneralLogRepository;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.custombond.auth.DXC_Auth;
import com.custombond.dto.request.BlackListRequest;
import com.custombond.util.JsonUtils;

@Service
public class Validation_Helper {

    @Autowired
    DXC_Auth DXC_auth;

    @Autowired
    JsonUtils JsonConverter;

    Validation_Helper(CBGeneralLogRepository CBGeneralLogRepository) {

    }

    public Map<String, Object> validateId(BlackListRequest Request) {
        Map<String, Object> ret = new HashMap<>();
        String nationalId = Request.getNationalId() != null
                ? Request.getNationalId().toString()
                : null;

        String taxId = Request.getTaxId() != null
                ? Request.getTaxId().toString()
                : null;

        Integer contactKey = Request.getContactKey() != null
                ? Integer.valueOf(Request.getContactKey() .toString())
                : null;

        if (nationalId != null) {
            nationalId = nationalId.replaceAll("\\D", "");
            ret.put("Type", 1);
            if (nationalId.length() < 14 || nationalId.length() > 14) {
                ret.put("Id", nationalId);
                ret.put("isValid", false);
                return ret;
            } else {
                ret.put("Id", nationalId);
                ret.put("isValid", true);
                return ret;
            }

        } else if (taxId != null) {
            taxId = taxId.replaceAll("\\D", "");
            ret.put("Type", 2);
            if (taxId.length() < 9 || taxId.length() > 9) {
                ret.put("Id", taxId);
                ret.put("isValid", false);

                return ret;
            } else {
                ret.put("Id", taxId);
                ret.put("isValid", true);
                return ret;
            }
        } else if (contactKey != null) {
            ret.put("Type", 3);
            if (contactKey.toString().length() < 7 || contactKey.toString().length() > 7) {
                ret.put("Id", contactKey);
                ret.put("isValid", false);

                return ret;
            } else {
                ret.put("Id", contactKey);
                ret.put("isValid", true);
                return ret;
            }
        } else {
            return null;
        }
    }

}
