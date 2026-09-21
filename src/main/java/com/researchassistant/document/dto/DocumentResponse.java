package com.researchassistant.document.dto;

import com.researchassistant.document.entity.DocumentStatus;
import com.researchassistant.document.entity.DocumentType;

import java.time.OffsetDateTime;
import java.util.UUID;

public record DocumentResponse(
        UUID id,
        UUID projectId,
        long documentNumber,
        String documentCode,
        String title,
        String bibliographicTitle,
        String authors,
        Integer publicationYear,
        String journal,
        String conference,
        String publisher,
        String volume,
        String issue,
        String pages,
        String doi,
        String url,
        String sourceType,
        String keywords,
        DocumentType type,
        DocumentStatus status,
        DocumentVersionResponse currentVersion,
        UUID createdBy,
        Integer pageCount,
        Long chunkCount,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt,
        OffsetDateTime archivedAt
) {
}
