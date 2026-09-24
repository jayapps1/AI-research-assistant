package com.researchassistant.project.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record CreateResearchProjectRequest(
        UUID workspaceId,

        @NotBlank(message = "Project title is required")
        @Size(max = 255, message = "Project title must not exceed 255 characters")
        String title,

        @Size(max = 5000, message = "Project description must not exceed 5000 characters")
        String description,

        @Size(max = 5000, message = "Research aim must not exceed 5000 characters")
        String researchAim,

        @Size(max = 255, message = "Study area must not exceed 255 characters")
        String studyArea,

        @Size(max = 100, message = "Research type must not exceed 100 characters")
        String researchType,

        @Size(max = 500, message = "Keywords must not exceed 500 characters")
        String keywords,

        UUID reportTemplateId,

        com.researchassistant.analysis.entity.CitationStyle citationStyle,

        Boolean citationStyleLocked,

        com.researchassistant.analysis.entity.CitationPresentation citationPresentation,

        String bibliographySort,

        Boolean includeDoi,

        Boolean includeUrl
) {
    public CreateResearchProjectRequest(String title, String description) {
        this(null, title, description, null, null, null, null, null, null, null, null, null, null, null);
    }

    public CreateResearchProjectRequest(UUID workspaceId, String title, String description) {
        this(workspaceId, title, description, null, null, null, null, null, null, null, null, null, null, null);
    }

    public CreateResearchProjectRequest(
            UUID workspaceId,
            String title,
            String description,
            String researchAim,
            String studyArea,
            String researchType,
            String keywords
    ) {
        this(workspaceId, title, description, researchAim, studyArea, researchType, keywords, null, null, null, null, null, null, null);
    }
}
