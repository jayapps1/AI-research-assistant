package com.researchassistant.workspace.dto;

import com.researchassistant.workspace.entity.WorkspaceRole;

import jakarta.validation.constraints.NotNull;

public record ChangeWorkspaceMemberRoleRequest(
        @NotNull(message = "Workspace role is required")
        WorkspaceRole role
) {
}
