package com.researchassistant.analysis.entity;

import com.researchassistant.common.enums.ContentOrigin;
import com.researchassistant.identity.entity.User;
import com.researchassistant.project.entity.ResearchProject;
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
@Table(name = "research_conclusions")
@Getter @Setter @NoArgsConstructor
public class ResearchConclusion {
    @Id @Column(nullable = false, updatable = false)
    private UUID id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "project_id", nullable = false)
    private ResearchProject project;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "discussion_id")
    private FindingDiscussion discussion;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "objective_id")
    private ResearchObjective objective;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "question_id")
    private ResearchQuestion question;
    @ManyToMany
    @JoinTable(name = "research_conclusion_findings",
            joinColumns = @JoinColumn(name = "conclusion_id"),
            inverseJoinColumns = @JoinColumn(name = "finding_id"))
    private Set<ResearchFinding> findings = new LinkedHashSet<>();
    @Column(nullable = false, length = 255)
    private String title;
    @Column(name = "conclusion_text", nullable = false, columnDefinition = "TEXT")
    private String conclusionText;
    @Column(nullable = false, columnDefinition = "TEXT")
    private String statement;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 40)
    private ResearchConclusionStatus status = ResearchConclusionStatus.DRAFT;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 40)
    private ResearchConclusionType type = ResearchConclusionType.OBJECTIVE_SPECIFIC;
    @Column(name = "scope_note", columnDefinition = "TEXT")
    private String scopeNote;
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
    @PrePersist void onCreate() { if (id == null) id = UUID.randomUUID(); OffsetDateTime now = OffsetDateTime.now(); if (createdAt == null) createdAt = now; updatedAt = now; if (origin == null) origin = ContentOrigin.USER; if (status == null) status = ResearchConclusionStatus.DRAFT; if (type == null) type = ResearchConclusionType.OBJECTIVE_SPECIFIC; if (revisionNumber < 1) revisionNumber = 1; if (displayOrder < 1) displayOrder = 1; if (conclusionText == null) conclusionText = statement; if (statement == null) statement = conclusionText; }
    @PreUpdate void onUpdate() { updatedAt = OffsetDateTime.now(); }
}
