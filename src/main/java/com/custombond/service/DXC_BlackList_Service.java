package com.custombond.service;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;


import com.custombond.auth.DXC_Auth;
import com.custombond.dto.request.BlackListRequest;
import com.custombond.dto.request.GetContactRequest;
import com.custombond.dto.response.BlackListResponse;
import com.custombond.entity.CBGeneralLog;
import com.custombond.entity.Enums;
import com.custombond.util.JsonUtils;
import com.custombond.helper.*;

@Service
public class DXC_BlackList_Service{

    @Autowired
    DXC_GetContact_Service dxc_GetContact_Service;

    @Autowired
    DXC_Auth DXC_auth;

    @Autowired
    Logger logger;

    @Autowired
    DB_Helper db_Helper;

    @Autowired
    Validation_Helper validation_Helper;

    @Autowired
    JsonUtils JsonConverter;

    public ResponseEntity<BlackListResponse> checkBlackList(BlackListRequest Request) {
        Object Id = "";
        String Token = "";
        BlackListResponse Response = new BlackListResponse();
        Map<String, Object> validationResult = validation_Helper.validateId(Request);
        if ((Boolean) validationResult.get("isValid") == true) {
            Id = validationResult.get("Id").toString();
        }
        try {
            Token = DXC_auth.getDxcToken();
            if (Token != null) {
                if ((Boolean) validationResult.get("isValid") == false) {
                    if ((Integer) validationResult.get("Type") == 2) {
                        Response.setTaxId(Id.toString());
                    }
                    Response.setId(Id);
                    Response.setErrorMessage("Id is not valid");
                    Response.setStatus(null);
                    logger.log("CheckBlackList", JsonConverter.toJson(Request), "Id is not valid", null, "/api/dxc/services/checkBlackListed", Enums.MethodType.GET.toString(),
                            Request.getVendorId(), Request.getCallSequance().toString(),JsonConverter.toJson(Request),Request.getVendorRequestId());
                    return ResponseEntity.badRequest().body(Response);
                } else {
                    return callCheckBlackList(Token, Request);
                }
            }
            return null;

        } catch (Exception e) {
            Response.setTaxId(Id.toString());
            Response.setErrorMessage("Internal Server Error");
            Response.setStatus(null);
            logger.log("CheckBlackList", JsonConverter.toJson(Request), e.toString(), null,
                    "/api/dxc/services/checkBlackListed", Enums.MethodType.GET.toString(), Request.getVendorId(), Request.getCallSequance().toString(),JsonConverter.toJson(Request),Request.getVendorRequestId());
            return ResponseEntity.internalServerError().body(Response);
        }

    }

    public ResponseEntity<BlackListResponse> checkBlackList(BlackListRequest Request, String Token) {
        String Id = Request.getTaxId();
        BlackListResponse Response = new BlackListResponse();
        if (Token != null) {
            Id = Id.replaceAll("\\D", "");
            if (Id.length() < 9 || Id.length() > 9) {
                Response.setTaxId(Id);
                Response.setErrorMessage("TaxId length is not equal 9");
                Response.setStatus(null);
                logger.log("CheckBlackList", JsonConverter.toJson(Request), "TaxId length is not equal 9", null,
                        "/api/dxc/services/checkBlackListed", Enums.MethodType.GET.toString(), Request.getVendorId(),
                        Request.getCallSequance().toString(),JsonConverter.toJson(Request),Request.getVendorRequestId());
                return ResponseEntity.badRequest().body(Response);
            } else {
                return callCheckBlackList(Token, Request);
            }
        } else {
            Response.setTaxId(Id);
            Response.setErrorMessage("Error in DXC");
            Response.setStatus(null);
            return ResponseEntity.internalServerError().body(Response);
        }

    }

    private ResponseEntity<BlackListResponse> callCheckBlackList(String Token, BlackListRequest Request) {
        ResponseEntity<Map<String, Object>> Contact = dxc_GetContact_Service.getContact(GetContactRequest.mapToGetContact(Request));
        CBGeneralLog log = db_Helper.getLogByRequestId(UUID.fromString(Contact.getBody().get("requestId").toString()));
        String reqBody = log.getRequestBody();
        String url =log .getUrl();
        String requestId = Optional.ofNullable(
                logger.logRequest(
                        "CheckBlackList",
                        reqBody,
                        null,
                        url,
                        Enums.MethodType.GET.toString(),
                        Request.getVendorId(),
                        Request.getCallSequance().toString(),JsonConverter.toJson(Request),Request.getVendorRequestId()).get("requestId"))
                .map(Object::toString).orElse(null);
        return logger.blackListResponse(Contact, requestId, Request);

    }

}
