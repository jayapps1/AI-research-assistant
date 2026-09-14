package com.researchassistant.retrieval.dto;

import java.util.Set;
import java.util.UUID;

public record DocumentRetrievalRequest(
        UUID workspaceId,
        UUID projectId,
        Set<UUID> documentIds,
        String query,
        int limit,
        boolean lexicalEnabled,
        boolean semanticEnabled,
        DocumentRetrievalMode mode
) {
}
