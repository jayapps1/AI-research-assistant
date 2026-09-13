package com.researchassistant.project.dto;

import com.researchassistant.project.entity.ProjectRole;

import jakarta.validation.constraints.NotNull;

public record ChangeProjectMemberRoleRequest(
        @NotNull(message = "Project role is required")
        ProjectRole role
) {
}
