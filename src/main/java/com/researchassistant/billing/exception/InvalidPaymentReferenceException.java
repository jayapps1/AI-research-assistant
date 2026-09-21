package com.researchassistant.billing.exception;

public class InvalidPaymentReferenceException extends RuntimeException {

    private final String errorCode;

    public InvalidPaymentReferenceException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }
}
