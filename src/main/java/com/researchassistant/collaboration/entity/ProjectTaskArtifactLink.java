package com.researchassistant.collaboration.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "project_task_artifact_links", indexes = @Index(name = "idx_task_artifact_links_artifact", columnList = "artifact_type,artifact_id"))
@Getter @Setter @NoArgsConstructor
public class ProjectTaskArtifactLink {
    @Id @Column(nullable = false, updatable = false)
    private UUID id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "task_id", nullable = false)
    private ProjectTask task;
    @Enumerated(EnumType.STRING) @Column(name = "artifact_type", nullable = false, length = 80)
    private CollaborationArtifactType artifactType;
    @Column(name = "artifact_id", nullable = false)
    private UUID artifactId;
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;
    @PrePersist void onCreate() { if (id == null) id = UUID.randomUUID(); if (createdAt == null) createdAt = OffsetDateTime.now(); }
}
