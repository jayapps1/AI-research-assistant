package com.researchassistant.analysis.entity;

import com.researchassistant.common.enums.ContentOrigin;
import com.researchassistant.identity.entity.User;
import com.researchassistant.project.entity.ResearchProject;
import com.researchassistant.researchdesign.entity.ResearchHypothesis;
import com.researchassistant.researchdesign.entity.ResearchObjective;
import com.researchassistant.researchdesign.entity.ResearchQuestion;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "research_findings", indexes = @Index(name = "idx_research_findings_project", columnList = "project_id"))
@Getter @Setter @NoArgsConstructor
public class ResearchFinding {
    public enum Strength { LOW, MODERATE, HIGH, INCONCLUSIVE }
    @Id @Column(nullable = false, updatable = false)
    private UUID id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "project_id", nullable = false)
    private ResearchProject project;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "analysis_result_id")
    private AnalysisResult analysisResult;
    @ManyToMany
    @JoinTable(name = "research_finding_analysis_results",
            joinColumns = @JoinColumn(name = "finding_id"),
            inverseJoinColumns = @JoinColumn(name = "analysis_result_id"))
    private Set<AnalysisResult> analysisResults = new LinkedHashSet<>();
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "objective_id")
    private ResearchObjective objective;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "question_id")
    private ResearchQuestion question;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "hypothesis_id")
    private ResearchHypothesis hypothesis;
    @Column(nullable = false, length = 255)
    private String title;
    @Column(name = "finding_text", nullable = false, columnDefinition = "TEXT")
    private String findingText;
    @Column(name = "statement", nullable = false, columnDefinition = "TEXT")
    private String statement;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 40)
    private ResearchFindingType type = ResearchFindingType.OTHER;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 40)
    private ResearchFindingStatus status = ResearchFindingStatus.DRAFT;
    @Column(name = "evidence_summary", columnDefinition = "TEXT")
    private String evidenceSummary;
    @Column(name = "result_value_snapshot", columnDefinition = "TEXT")
    private String resultValueSnapshot;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 30)
    private Strength strength = Strength.INCONCLUSIVE;
    @Column(name = "display_order", nullable = false)
    private int displayOrder = 1;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 30)
    private ContentOrigin origin = ContentOrigin.USER;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "created_by", nullable = false)
    private User createdBy;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "updated_by")
    private User updatedBy;
    @Column(name = "revision_number", nullable = false)
    private int revisionNumber = 1;
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;
    @PrePersist void onCreate() { if (id == null) id = UUID.randomUUID(); OffsetDateTime now = OffsetDateTime.now(); if (createdAt == null) createdAt = now; updatedAt = now; if (origin == null) origin = ContentOrigin.USER; if (strength == null) strength = Strength.INCONCLUSIVE; if (type == null) type = ResearchFindingType.OTHER; if (status == null) status = ResearchFindingStatus.DRAFT; if (revisionNumber < 1) revisionNumber = 1; if (displayOrder < 1) displayOrder = 1; if (findingText == null) findingText = statement; if (statement == null) statement = findingText; if (analysisResult != null) analysisResults.add(analysisResult); }
    @PreUpdate void onUpdate() { updatedAt = OffsetDateTime.now(); }
}
