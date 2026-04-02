package com.custombond.exception;

import com.custombond.dto.response.DXCQuotePreparationApiErrorResponse;

import lombok.Getter;

@Getter
public class DXCQuotePraparationApiBadRequestException extends RuntimeException {

    private final DXCQuotePreparationApiErrorResponse errorResponse;

    public DXCQuotePraparationApiBadRequestException(DXCQuotePreparationApiErrorResponse errorResponse) {
        super(errorResponse.getTitle());
        this.errorResponse = errorResponse;
    }
}
