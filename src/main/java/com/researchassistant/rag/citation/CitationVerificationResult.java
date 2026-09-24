package com.researchassistant.rag.citation;

import java.util.List;

public record CitationVerificationResult(
        boolean verified,
        List<String> errors,
        List<Integer> citedEvidenceOrdinals,
        List<String> citationMarkers,
        List<String> invalidCitationMarkers,
        List<Integer> missingCitationOrdinals
) {
    public CitationVerificationResult {
        errors = errors == null ? List.of() : List.copyOf(errors);
        citedEvidenceOrdinals = citedEvidenceOrdinals == null ? List.of() : List.copyOf(citedEvidenceOrdinals);
        citationMarkers = citationMarkers == null ? List.of() : List.copyOf(citationMarkers);
        invalidCitationMarkers = invalidCitationMarkers == null ? List.of() : List.copyOf(invalidCitationMarkers);
        missingCitationOrdinals = missingCitationOrdinals == null ? List.of() : List.copyOf(missingCitationOrdinals);
    }

    public CitationVerificationResult(boolean verified, List<String> errors) {
        this(verified, errors, List.of(), List.of(), List.of(), List.of());
    }
}
