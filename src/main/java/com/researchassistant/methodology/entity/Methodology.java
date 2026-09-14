package com.researchassistant.methodology.entity;

import com.researchassistant.common.enums.ContentOrigin;
import com.researchassistant.identity.entity.User;
import com.researchassistant.project.entity.ResearchProject;
import com.researchassistant.researchdesign.entity.ResearchProblem;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "methodologies", indexes = @Index(name = "idx_methodologies_project", columnList = "project_id"))
@Getter @Setter @NoArgsConstructor
public class Methodology {
    @Id @Column(nullable = false, updatable = false)
    private UUID id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "project_id", nullable = false)
    private ResearchProject project;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "research_problem_id")
    private ResearchProblem researchProblem;
    @Column(length = 255)
    private String title;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 40)
    private ResearchApproach approach = ResearchApproach.UNSPECIFIED;
    @Enumerated(EnumType.STRING) @Column(name = "design_type", nullable = false, length = 80)
    private ResearchDesignType designType = ResearchDesignType.UNSPECIFIED;
    @Column(name = "design_description", columnDefinition = "TEXT")
    private String designDescription;
    @Column(name = "study_setting", columnDefinition = "TEXT")
    private String studySetting;
    @Column(name = "study_period", length = 255)
    private String studyPeriod;
    @Column(columnDefinition = "TEXT")
    private String rationale;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 30)
    private MethodologyStatus status = MethodologyStatus.DRAFT;
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
    @PrePersist void onCreate() { if (id == null) id = UUID.randomUUID(); OffsetDateTime now = OffsetDateTime.now(); if (createdAt == null) createdAt = now; updatedAt = now; if (approach == null) approach = ResearchApproach.UNSPECIFIED; if (designType == null) designType = ResearchDesignType.UNSPECIFIED; if (status == null) status = MethodologyStatus.DRAFT; if (origin == null) origin = ContentOrigin.USER; if (revisionNumber < 1) revisionNumber = 1; }
    @PreUpdate void onUpdate() { updatedAt = OffsetDateTime.now(); }
}
