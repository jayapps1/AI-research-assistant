package com.researchassistant.framework.entity;

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
@Table(name = "theoretical_frameworks", indexes = @Index(name = "idx_theoretical_frameworks_project", columnList = "project_id"))
@Getter @Setter @NoArgsConstructor
public class TheoreticalFramework {
    @Id @Column(nullable = false, updatable = false)
    private UUID id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "project_id", nullable = false)
    private ResearchProject project;
    @Column(nullable = false, length = 255)
    private String title;
    @Column(nullable = false, columnDefinition = "TEXT")
    private String overview;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 30)
    private TheoreticalFrameworkStatus status = TheoreticalFrameworkStatus.DRAFT;
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
    @PrePersist void onCreate() { if (id == null) id = UUID.randomUUID(); OffsetDateTime now = OffsetDateTime.now(); if (createdAt == null) createdAt = now; updatedAt = now; if (status == null) status = TheoreticalFrameworkStatus.DRAFT; if (origin == null) origin = ContentOrigin.USER; if (revisionNumber < 1) revisionNumber = 1; }
    @PreUpdate void onUpdate() { updatedAt = OffsetDateTime.now(); }
}
