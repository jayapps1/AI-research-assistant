package com.researchassistant.analysis.exception;

public class ReportExportSelectionException extends RuntimeException {
    private final String code;

    public ReportExportSelectionException(String code, String message) {
        super(message);
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
