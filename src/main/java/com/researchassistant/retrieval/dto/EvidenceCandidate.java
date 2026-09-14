package com.researchassistant.retrieval.dto;

import java.util.UUID;

public record EvidenceCandidate(
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
        Double lexicalScore,
        Double semanticScore,
        Double fusedScore,
        Double rerankScore,
        String documentTitle
) {
    public EvidenceCandidate(
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
            Double lexicalScore,
            Double semanticScore,
            Double fusedScore,
            String documentTitle
    ) {
        this(
                chunkId,
                workspaceId,
                projectId,
                documentId,
                documentCode,
                documentVersionId,
                versionNumber,
                pageNumber,
                chunkNumber,
                text,
                lexicalScore,
                semanticScore,
                fusedScore,
                null,
                documentTitle
        );
    }

    public EvidenceCandidate withRerankScore(Double newRerankScore) {
        return new EvidenceCandidate(
                chunkId,
                workspaceId,
                projectId,
                documentId,
                documentCode,
                documentVersionId,
                versionNumber,
                pageNumber,
                chunkNumber,
                text,
                lexicalScore,
                semanticScore,
                fusedScore,
                newRerankScore,
                documentTitle
        );
    }
}
