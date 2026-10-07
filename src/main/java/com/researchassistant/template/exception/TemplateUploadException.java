package com.researchassistant.template.exception;

public class TemplateUploadException extends RuntimeException {
    private final String code;

    public TemplateUploadException(String code, String message) {
        super(message);
        this.code = code;
    }

    public TemplateUploadException(String code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
