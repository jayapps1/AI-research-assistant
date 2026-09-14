package com.researchassistant.rag.citation;

import java.util.List;

public record CitationVerificationResult(
        boolean verified,
        List<String> errors
) {
    public CitationVerificationResult {
        errors = errors == null ? List.of() : List.copyOf(errors);
    }
}
