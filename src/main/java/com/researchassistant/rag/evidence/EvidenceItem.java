package com.researchassistant.rag.evidence;

import java.util.UUID;

public record EvidenceItem(
        UUID evidenceId,
        int evidenceOrdinal,
        UUID chunkId,
        UUID workspaceId,
        UUID projectId,
        UUID documentId,
        String documentCode,
        String documentTitle,
        UUID documentVersionId,
        int versionNumber,
        int pageNumber,
        int chunkNumber,
        String text,
        Double lexicalScore,
        Double semanticScore,
        Double fusedScore,
        Double rerankScore,
        int finalRank
) {
}
