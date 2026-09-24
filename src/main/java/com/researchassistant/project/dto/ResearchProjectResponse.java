package com.researchassistant.project.dto;

import com.researchassistant.project.entity.ProjectRole;
import com.researchassistant.project.entity.ResearchProjectStatus;

import java.time.OffsetDateTime;
import java.util.UUID;

public record ResearchProjectResponse(
        UUID id,
        UUID workspaceId,
        String title,
        String description,
        String researchAim,
        String studyArea,
        String researchType,
        String keywords,
        UUID reportTemplateId,
        String reportTemplateName,
        com.researchassistant.analysis.entity.CitationStyle citationStyle,
        boolean citationStyleLocked,
        com.researchassistant.analysis.entity.CitationPresentation citationPresentation,
        String bibliographySort,
        boolean includeDoi,
        boolean includeUrl,
        ResearchProjectStatus status,
        UUID createdByUserId,
        ProjectRole currentUserRole,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
    public ResearchProjectResponse(
            UUID id,
            UUID workspaceId,
            String title,
            String description,
            ResearchProjectStatus status,
            UUID createdByUserId,
            ProjectRole currentUserRole,
            OffsetDateTime createdAt,
            OffsetDateTime updatedAt
    ) {
        this(id, workspaceId, title, description, null, null, null, null, null, null, com.researchassistant.analysis.entity.CitationStyle.APA_7, false, com.researchassistant.analysis.entity.CitationPresentation.PARENTHETICAL, "STYLE_DEFAULT", true, true, status, createdByUserId, currentUserRole, createdAt, updatedAt);
    }

    public ResearchProjectResponse(
            UUID id,
            UUID workspaceId,
            String title,
            String description,
            String researchAim,
            String studyArea,
            String researchType,
            String keywords,
            ResearchProjectStatus status,
            UUID createdByUserId,
            ProjectRole currentUserRole,
            OffsetDateTime createdAt,
            OffsetDateTime updatedAt
    ) {
        this(id, workspaceId, title, description, researchAim, studyArea, researchType, keywords, null, null, com.researchassistant.analysis.entity.CitationStyle.APA_7, false, com.researchassistant.analysis.entity.CitationPresentation.PARENTHETICAL, "STYLE_DEFAULT", true, true, status, createdByUserId, currentUserRole, createdAt, updatedAt);
    }
}
