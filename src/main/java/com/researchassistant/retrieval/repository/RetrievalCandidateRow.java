package com.researchassistant.retrieval.repository;

import java.util.UUID;

public record RetrievalCandidateRow(
        UUID chunkId,
        UUID workspaceId,
        UUID projectId,
        UUID documentId,
        String documentCode,
        UUID documentVersionId,
        int versionNumber,
        int pageNumber,
        int chunkNumber,
        String text,
        double score,
        String documentTitle
) {
}
