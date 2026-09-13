package com.researchassistant.document.dto;

import com.researchassistant.document.entity.DocumentProcessingJobType;
import com.researchassistant.document.entity.DocumentProcessingStatus;

import java.time.OffsetDateTime;
import java.util.UUID;

public record DocumentProcessingJobResponse(
        UUID id,
        UUID documentVersionId,
        DocumentProcessingJobType type,
        DocumentProcessingStatus status,
        int attemptNumber,
        String errorCode,
        String errorMessage,
        OffsetDateTime queuedAt,
        OffsetDateTime startedAt,
        OffsetDateTime completedAt,
        OffsetDateTime failedAt
) {
}
