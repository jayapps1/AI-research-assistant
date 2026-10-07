package com.researchassistant.project.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.UUID;

public record CreateResearchProjectRequest(
        UUID workspaceId,

        @NotBlank(message = "Project title is required")
        @Size(max = 255, message = "Project title must not exceed 255 characters")
        String title,

        @Size(max = 5000, message = "Project description must not exceed 5000 characters")
        String description,

        com.researchassistant.project.entity.AcademicWorkspaceType workspaceType,

        com.researchassistant.project.entity.AcademicProjectType projectType,

        @Size(max = 255, message = "Institution must not exceed 255 characters")
        String institution,

        @Size(max = 255, message = "Department must not exceed 255 characters")
        String department,

        @Size(max = 255, message = "Programme must not exceed 255 characters")
        String programme,

        @Size(max = 40, message = "Academic year must not exceed 40 characters")
        String academicYear,

        @Size(max = 255, message = "Supervisor must not exceed 255 characters")
        String supervisor,

        @Size(max = 255, message = "Course name must not exceed 255 characters")
        String courseName,

        @Size(max = 80, message = "Course code must not exceed 80 characters")
        String courseCode,

        @Size(max = 255, message = "Lecturer must not exceed 255 characters")
        String lecturer,

        LocalDate deadline,

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
        this(
                null,
                title,
                description,
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
                null,
                null,
                null,
                null,
                null,
                null,
                null
        );
    }

    public CreateResearchProjectRequest(UUID workspaceId, String title, String description) {
        this(
                workspaceId,
                title,
                description,
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
                null,
                null,
                null,
                null,
                null,
                null,
                null
        );
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
        this(
                workspaceId,
                title,
                description,
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
                researchAim,
                studyArea,
                researchType,
                keywords,
                null,
                null,
                null,
                null,
                null,
                null,
                null
        );
    }
}
