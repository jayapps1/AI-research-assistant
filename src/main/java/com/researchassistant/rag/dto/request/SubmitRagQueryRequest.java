package com.researchassistant.rag.dto.request;

import com.researchassistant.rag.scope.RetrievalScopeType;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.Set;
import java.util.UUID;

public record SubmitRagQueryRequest(
        @NotBlank
        @Size(max = 8000)
        String question,
        RetrievalScopeType scopeType,
        Set<UUID> documentIds,
        Integer evidenceLimit,
        @Size(max = 2000)
        String retrievalQuery
) {
}
