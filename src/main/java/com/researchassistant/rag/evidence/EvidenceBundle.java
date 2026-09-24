package com.researchassistant.rag.evidence;

import com.researchassistant.rag.scope.RetrievalScope;

import java.time.OffsetDateTime;
import java.util.List;

public record EvidenceBundle(
        String query,
        RetrievalScope scope,
        List<EvidenceItem> items,
        int lexicalCandidatesCount,
        int semanticCandidatesCount,
        int rerankedCandidatesCount,
        int finalEvidenceCount,
        int contextBudgetTokens,
        int selectedEvidenceTokens,
        int truncatedEvidenceCount,
        String retrievalMode,
        String embeddingModel,
        String rerankerName,
        OffsetDateTime createdAt
) {
    public EvidenceBundle {
        items = List.copyOf(items);
    }
}
