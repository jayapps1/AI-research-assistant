package com.researchassistant.workspace.dto;

import com.researchassistant.workspace.entity.WorkspaceRole;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record AddWorkspaceMemberRequest(
        @NotBlank(message = "Email is required")
        @Email(message = "Email must be valid")
        @Size(max = 255, message = "Email must not exceed 255 characters")
        String email,

        @NotNull(message = "Workspace role is required")
        WorkspaceRole role
) {
}
