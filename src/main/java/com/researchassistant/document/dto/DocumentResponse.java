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
        DocumentType type,
        DocumentStatus status,
        DocumentVersionResponse currentVersion,
        UUID createdBy,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt,
        OffsetDateTime archivedAt
) {
}
