package com.researchassistant.workspace.dto;

import com.researchassistant.workspace.entity.WorkspaceRole;
import com.researchassistant.workspace.entity.WorkspaceStatus;
import com.researchassistant.workspace.entity.WorkspaceType;

import java.time.OffsetDateTime;
import java.util.UUID;

public record WorkspaceResponse(
        UUID id,
        String name,
        WorkspaceType type,
        WorkspaceStatus status,
        UUID ownerId,
        WorkspaceRole currentUserRole,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}
