package com.researchassistant.workspace.dto;

import jakarta.validation.constraints.Size;

public record UpdateWorkspaceRequest(
        @Size(max = 255, message = "Workspace name must not exceed 255 characters")
        String name
) {
}
