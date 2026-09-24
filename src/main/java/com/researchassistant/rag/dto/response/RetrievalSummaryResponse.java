package com.researchassistant.rag.dto.response;

import com.researchassistant.rag.scope.RetrievalScopeType;

public record RetrievalSummaryResponse(
        RetrievalScopeType scopeType,
        int documentCount,
        int evidenceCount,
        String retrievalMode,
        Integer analyzedSourceCount,
        Integer sourcesWithRelevantEvidenceCount,
        Integer candidateChunkCount,
        Integer estimatedInputTokens,
        Integer actualInputTokens,
        Integer actualOutputTokens,
        Integer contextBudgetTokens,
        Integer truncatedEvidenceCount,
        String generationStrategy,
        String configuredModel
) {
}
