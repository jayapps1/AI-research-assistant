package com.researchassistant.rag.scope;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record RetrievalScope(
        UUID workspaceId,
        UUID projectId,
        RetrievalScopeType scopeType,
        RetrievalVersionPolicy versionPolicy,
        List<UUID> authorizedDocumentIds,
        List<UUID> authorizedDocumentVersionIds,
        Map<UUID, UUID> currentVersionByDocumentId,
        int documentCount,
        OffsetDateTime resolvedAt
) {
    public RetrievalScope {
        authorizedDocumentIds = List.copyOf(authorizedDocumentIds);
        authorizedDocumentVersionIds = List.copyOf(authorizedDocumentVersionIds);
        currentVersionByDocumentId = Map.copyOf(currentVersionByDocumentId);
    }
}
