package com.researchassistant.analysis.exception;

import com.researchassistant.analysis.dto.AnalysisDtos.ReportValidationResponse;

public class ReportValidationException extends RuntimeException {
    private final ReportValidationResponse validation;

    public ReportValidationException(ReportValidationResponse validation) {
        super("The report has items that must be corrected before finalization.");
        this.validation = validation;
    }

    public ReportValidationResponse getValidation() {
        return validation;
    }
}
