package com.researchassistant.project.dto;

import com.researchassistant.project.entity.ProjectRole;
import com.researchassistant.project.entity.ResearchProjectStatus;

import java.time.OffsetDateTime;
import java.util.UUID;

public record ResearchProjectResponse(
        UUID id,
        UUID workspaceId,
        String title,
        String description,
        ResearchProjectStatus status,
        UUID createdByUserId,
        ProjectRole currentUserRole,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}
