package com.researchassistant.dataset.model;

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
@Table(name = "research_datasets", indexes = @Index(name = "idx_research_datasets_project", columnList = "project_id"))
@Getter @Setter @NoArgsConstructor
public class ResearchDataset {
    public enum SourceType { COLLECTED_SESSIONS, IMPORTED_CSV, IMPORTED_XLSX, MANUAL, SECONDARY_DATA, MERGED, OTHER }
    public enum Status { DRAFT, VALIDATING, READY, INVALID, ARCHIVED }
    @Id @Column(nullable = false, updatable = false)
    private UUID id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "project_id", nullable = false)
    private ResearchProject project;
    @Column(nullable = false, length = 255)
    private String name;
    @Column(columnDefinition = "TEXT")
    private String description;
    @Enumerated(EnumType.STRING) @Column(name = "source_type", nullable = false, length = 40)
    private SourceType sourceType = SourceType.MANUAL;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 30)
    private Status status = Status.DRAFT;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 30)
    private ContentOrigin origin = ContentOrigin.USER;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "created_by", nullable = false)
    private User createdBy;
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;
    @PrePersist void onCreate() { if (id == null) id = UUID.randomUUID(); OffsetDateTime now = OffsetDateTime.now(); if (createdAt == null) createdAt = now; updatedAt = now; if (sourceType == null) sourceType = SourceType.MANUAL; if (status == null) status = Status.DRAFT; if (origin == null) origin = ContentOrigin.USER; }
    @PreUpdate void onUpdate() { updatedAt = OffsetDateTime.now(); }
}
