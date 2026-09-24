package com.researchassistant.analysis.entity;

import com.researchassistant.common.enums.ContentOrigin;
import com.researchassistant.identity.entity.User;
import com.researchassistant.project.entity.ResearchProject;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "research_reports", indexes = @Index(name = "idx_research_reports_project", columnList = "project_id"))
@Getter @Setter @NoArgsConstructor
public class ResearchReport {
    @Id @Column(nullable = false, updatable = false)
    private UUID id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "project_id", nullable = false)
    private ResearchProject project;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "template_id")
    private ResearchReportTemplate template;
    @Column(nullable = false, length = 255)
    private String title;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 60)
    private ResearchReportType type = ResearchReportType.FINAL_YEAR_PROJECT;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 40)
    private ResearchReportStatus status = ResearchReportStatus.DRAFT;
    @Column(name = "institution_name", length = 255)
    private String institutionName;
    @Column(name = "department_name", length = 255)
    private String departmentName;
    @Column(name = "author_name", length = 255)
    private String authorName;
    @Column(name = "supervisor_name", length = 255)
    private String supervisorName;
    @Column(name = "degree_program", length = 255)
    private String degreeProgram;
    @Column(name = "submission_year")
    private Integer submissionYear;
    @Enumerated(EnumType.STRING) @Column(name = "citation_style", nullable = false, length = 60)
    private CitationStyle citationStyle = CitationStyle.APA_7;
    @Column(name = "include_uncited_references", nullable = false)
    private boolean includeUncitedReferences = false;
    @Column(name = "literature_matrix_inclusion", nullable = false, length = 40)
    private String literatureMatrixInclusion = "NONE";
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 30)
    private ContentOrigin origin = ContentOrigin.USER;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "created_by", nullable = false)
    private User createdBy;
    @Column(name = "revision_number", nullable = false)
    private int revisionNumber = 1;
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;
    @PrePersist void onCreate() { if (id == null) id = UUID.randomUUID(); OffsetDateTime now = OffsetDateTime.now(); if (createdAt == null) createdAt = now; updatedAt = now; if (type == null) type = ResearchReportType.FINAL_YEAR_PROJECT; if (status == null) status = ResearchReportStatus.DRAFT; if (citationStyle == null) citationStyle = CitationStyle.APA_7; if (origin == null) origin = ContentOrigin.USER; if (revisionNumber < 1) revisionNumber = 1; }
    @PreUpdate void onUpdate() { updatedAt = OffsetDateTime.now(); }
}
