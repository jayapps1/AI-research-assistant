package com.researchassistant.project.dto;

import jakarta.validation.constraints.Size;

public record UpdateResearchProjectRequest(
        @Size(max = 255, message = "Project title must not exceed 255 characters")
        String title,

        @Size(max = 5000, message = "Project description must not exceed 5000 characters")
        String description
) {
}
