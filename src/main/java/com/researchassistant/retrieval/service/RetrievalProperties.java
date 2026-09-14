package com.researchassistant.retrieval.service;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.retrieval")
public record RetrievalProperties(
        int lexicalCandidates,
        int semanticCandidates,
        int fusionCandidates,
        int rerankCandidates,
        int finalLimit,
        int maxLimit,
        int rrfK
) {
}
