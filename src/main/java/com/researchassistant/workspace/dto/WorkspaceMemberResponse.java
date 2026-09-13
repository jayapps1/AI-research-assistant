package com.researchassistant.workspace.dto;

import com.researchassistant.workspace.entity.WorkspaceMembershipStatus;
import com.researchassistant.workspace.entity.WorkspaceRole;

import java.time.OffsetDateTime;
import java.util.UUID;

public record WorkspaceMemberResponse(
        UUID userId,
        String email,
        String firstName,
        String lastName,
        WorkspaceRole role,
        WorkspaceMembershipStatus status,
        OffsetDateTime joinedAt
) {
}
