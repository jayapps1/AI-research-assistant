package com.researchassistant.project.dto;

import com.researchassistant.project.entity.ProjectRole;
import com.researchassistant.project.entity.AcademicProjectType;
import com.researchassistant.project.entity.AcademicWorkspaceType;
import com.researchassistant.project.entity.ResearchProjectStatus;
import com.researchassistant.analysis.entity.ResearchReportType;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

public record ResearchProjectResponse(
        UUID id,
        UUID workspaceId,
        String title,
        String description,
        AcademicWorkspaceType workspaceType,
        String workspaceTypeLabel,
        AcademicProjectType projectType,
        ResearchReportType reportType,
        String finalDocumentLabel,
        String workAreaLabel,
        String institution,
        String department,
        String programme,
        String academicYear,
        String supervisor,
        String courseName,
        String courseCode,
        String lecturer,
        LocalDate deadline,
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
        this(
                id,
                workspaceId,
                title,
                description,
                AcademicWorkspaceType.ACADEMIC_RESEARCH,
                "Academic Research",
                null,
                ResearchReportType.RESEARCH_REPORT,
                "Research Report",
                "Study Design",
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                com.researchassistant.analysis.entity.CitationStyle.APA_7,
                false,
                com.researchassistant.analysis.entity.CitationPresentation.PARENTHETICAL,
                "STYLE_DEFAULT",
                true,
                true,
                status,
                createdByUserId,
                currentUserRole,
                createdAt,
                updatedAt
        );
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
        this(
                id,
                workspaceId,
                title,
                description,
                AcademicWorkspaceType.ACADEMIC_RESEARCH,
                "Academic Research",
                null,
                ResearchReportType.RESEARCH_REPORT,
                "Research Report",
                "Study Design",
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                researchAim,
                studyArea,
                researchType,
                keywords,
                null,
                null,
                com.researchassistant.analysis.entity.CitationStyle.APA_7,
                false,
                com.researchassistant.analysis.entity.CitationPresentation.PARENTHETICAL,
                "STYLE_DEFAULT",
                true,
                true,
                status,
                createdByUserId,
                currentUserRole,
                createdAt,
                updatedAt
        );
    }
}
