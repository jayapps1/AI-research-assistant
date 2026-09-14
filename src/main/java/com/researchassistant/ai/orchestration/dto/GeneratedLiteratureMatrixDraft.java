package com.researchassistant.ai.orchestration.dto;

import java.util.List;

public record GeneratedLiteratureMatrixDraft(
        String studyTitle,
        String authors,
        Integer publicationYear,
        String methodologyUsed,
        String sampleSizeAndPopulation,
        String keyFindings,
        String limitations,
        List<Integer> citedEvidenceOrdinals
) {
}
