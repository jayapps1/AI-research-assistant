package com.researchassistant.analysis.exception;

public class InsufficientProjectEvidenceException extends RuntimeException {
    private final String sectionTitle;
    private final String requiredEvidence;

    public InsufficientProjectEvidenceException(String message) {
        super(message);
        this.sectionTitle = null;
        this.requiredEvidence = null;
    }

    public InsufficientProjectEvidenceException(String sectionTitle, String requiredEvidence, String message) {
        super(message);
        this.sectionTitle = sectionTitle;
        this.requiredEvidence = requiredEvidence;
    }

    public String getSectionTitle() {
        return sectionTitle;
    }

    public String getRequiredEvidence() {
        return requiredEvidence;
    }
}
