package com.researchassistant.researchdesign.entity;

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
@Table(name = "research_objectives", indexes = @Index(name = "idx_research_objectives_project", columnList = "project_id"))
@Getter @Setter @NoArgsConstructor
public class ResearchObjective {
    @Id @Column(nullable = false, updatable = false)
    private UUID id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "project_id", nullable = false)
    private ResearchProject project;
    @Enumerated(EnumType.STRING) @Column(name = "type", nullable = false, length = 30)
    private ResearchObjectiveType type = ResearchObjectiveType.SPECIFIC;
    @Column(name = "text", nullable = false, columnDefinition = "TEXT")
    private String text;
    @Column(name = "display_order", nullable = false)
    private int displayOrder = 1;
    @Enumerated(EnumType.STRING) @Column(name = "origin", nullable = false, length = 30)
    private ContentOrigin origin = ContentOrigin.USER;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "created_by", nullable = false)
    private User createdBy;
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;
    @PrePersist void onCreate() { if (id == null) id = UUID.randomUUID(); if (createdAt == null) createdAt = OffsetDateTime.now(); if (origin == null) origin = ContentOrigin.USER; }
}
