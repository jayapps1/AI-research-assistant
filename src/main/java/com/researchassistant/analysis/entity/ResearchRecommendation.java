package com.researchassistant.analysis.entity;

import com.researchassistant.common.enums.ContentOrigin;
import com.researchassistant.identity.entity.User;
import com.researchassistant.project.entity.ResearchProject;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "research_recommendations")
@Getter @Setter @NoArgsConstructor
public class ResearchRecommendation {
    public enum Priority { LOW, MEDIUM, HIGH, CRITICAL }
    @Id @Column(nullable = false, updatable = false)
    private UUID id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "project_id", nullable = false)
    private ResearchProject project;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "conclusion_id")
    private ResearchConclusion conclusion;
    @ManyToMany
    @JoinTable(name = "research_recommendation_conclusions",
            joinColumns = @JoinColumn(name = "recommendation_id"),
            inverseJoinColumns = @JoinColumn(name = "conclusion_id"))
    private Set<ResearchConclusion> conclusions = new LinkedHashSet<>();
    @ManyToMany
    @JoinTable(name = "research_recommendation_findings",
            joinColumns = @JoinColumn(name = "recommendation_id"),
            inverseJoinColumns = @JoinColumn(name = "finding_id"))
    private Set<ResearchFinding> findings = new LinkedHashSet<>();
    @Column(nullable = false, length = 255)
    private String title;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 40)
    private ResearchRecommendationType type = ResearchRecommendationType.OTHER;
    @Column(name = "recommendation_text", nullable = false, columnDefinition = "TEXT")
    private String recommendationText;
    @Column(nullable = false, columnDefinition = "TEXT")
    private String recommendation;
    @Column(name = "target_audience", length = 255)
    private String targetAudience;
    @Column(length = 120)
    private String audience;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 30)
    private RecommendationPriority priority = RecommendationPriority.MEDIUM;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 40)
    private ResearchRecommendationStatus status = ResearchRecommendationStatus.DRAFT;
    @Column(columnDefinition = "TEXT")
    private String rationale;
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
    @PrePersist void onCreate() { if (id == null) id = UUID.randomUUID(); OffsetDateTime now = OffsetDateTime.now(); if (createdAt == null) createdAt = now; updatedAt = now; if (origin == null) origin = ContentOrigin.USER; if (type == null) type = ResearchRecommendationType.OTHER; if (priority == null) priority = RecommendationPriority.MEDIUM; if (status == null) status = ResearchRecommendationStatus.DRAFT; if (revisionNumber < 1) revisionNumber = 1; if (displayOrder < 1) displayOrder = 1; if (recommendationText == null) recommendationText = recommendation; if (recommendation == null) recommendation = recommendationText; if (targetAudience == null) targetAudience = audience; if (audience == null) audience = targetAudience; if (conclusion != null) conclusions.add(conclusion); }
    @PreUpdate void onUpdate() { updatedAt = OffsetDateTime.now(); }
}
