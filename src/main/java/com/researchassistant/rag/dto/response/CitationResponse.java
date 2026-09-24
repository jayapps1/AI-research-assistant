package com.researchassistant.rag.dto.response;

import java.util.UUID;

public record CitationResponse(
        int number,
        UUID documentId,
        String documentCode,
        String documentTitle,
        UUID documentVersionId,
        int versionNumber,
        int pageNumber,
        int chunkNumber,
        String supportingExcerpt,
        String formattedCitation,
        boolean metadataComplete,
        String metadataWarning
) {
}
