package com.researchassistant.document.exception;

public class DocumentUploadException extends RuntimeException {

    private final String errorCode;

    public DocumentUploadException(String message) {
        this(null, message);
    }

    public DocumentUploadException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }
}
