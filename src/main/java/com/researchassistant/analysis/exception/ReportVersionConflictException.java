package com.researchassistant.analysis.exception;

public class ReportVersionConflictException extends RuntimeException {
    private final String code;

    public ReportVersionConflictException(String code, String message) {
        super(message);
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
