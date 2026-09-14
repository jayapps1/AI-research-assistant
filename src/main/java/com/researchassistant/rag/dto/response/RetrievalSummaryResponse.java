package com.researchassistant.rag.dto.response;

import com.researchassistant.rag.scope.RetrievalScopeType;

public record RetrievalSummaryResponse(
        RetrievalScopeType scopeType,
        int documentCount,
        int evidenceCount,
        String retrievalMode
) {
}
