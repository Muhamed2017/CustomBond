package com.custombond.dto.response;

import java.util.List;

import lombok.Data;

@Data
public class UploadDocumentResponse {

    Boolean Uploaded;

    String Message;

    List<Error> errors;

    @Data
    public static class Error {
        String code;
        String message;
    }

    public UploadDocumentResponse() {
        this.Uploaded = true;
        this.Message = "All Files Are Uploaded";
        this.errors = List.of();
    }

}