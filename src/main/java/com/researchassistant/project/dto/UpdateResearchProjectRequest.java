package com.researchassistant.project.dto;

import jakarta.validation.constraints.Size;

public record UpdateResearchProjectRequest(
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

        java.util.UUID reportTemplateId,

        com.researchassistant.analysis.entity.CitationStyle citationStyle,

        Boolean citationStyleLocked,

        com.researchassistant.analysis.entity.CitationPresentation citationPresentation,

        String bibliographySort,

        Boolean includeDoi,

        Boolean includeUrl,

        com.researchassistant.project.entity.ResearchProjectStatus status
) {
    public UpdateResearchProjectRequest(String title, String description) {
        this(title, description, null, null, null, null, null, null, null, null, null, null, null, null);
    }
}
