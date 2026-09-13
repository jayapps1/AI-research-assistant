package com.researchassistant.project.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record CreateResearchProjectRequest(
        @NotNull(message = "Workspace ID is required")
        UUID workspaceId,

        @NotBlank(message = "Project title is required")
        @Size(max = 255, message = "Project title must not exceed 255 characters")
        String title,

        @Size(max = 5000, message = "Project description must not exceed 5000 characters")
        String description
) {
}
