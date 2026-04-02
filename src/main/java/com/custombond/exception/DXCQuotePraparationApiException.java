package com.custombond.exception;


import lombok.Getter;

@Getter
public class DXCQuotePraparationApiException extends RuntimeException {

    private final int statusCode;

    public DXCQuotePraparationApiException(String message, int statusCode) {
        super(message);
        this.statusCode = statusCode;
    }
}
