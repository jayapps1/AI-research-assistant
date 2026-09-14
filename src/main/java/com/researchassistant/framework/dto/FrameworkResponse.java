package com.researchassistant.framework.dto;

import com.researchassistant.common.enums.ContentOrigin;

import java.time.OffsetDateTime;
import java.util.UUID;

public record FrameworkResponse(
        UUID id,
        UUID projectId,
        String title,
        String description,
        String status,
        ContentOrigin origin,
        int revisionNumber,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}
