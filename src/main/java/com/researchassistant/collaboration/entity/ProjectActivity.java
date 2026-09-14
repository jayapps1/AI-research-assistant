package com.researchassistant.collaboration.entity;

import com.researchassistant.identity.entity.User;
import com.researchassistant.project.entity.ResearchProject;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "project_activities", indexes = {
        @Index(name = "idx_project_activities_project_time", columnList = "project_id,occurred_at"),
        @Index(name = "idx_project_activities_actor", columnList = "actor_id"),
        @Index(name = "idx_project_activities_type", columnList = "type")
})
@Getter @Setter @NoArgsConstructor
public class ProjectActivity {
    @Id @Column(nullable = false, updatable = false)
    private UUID id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "project_id", nullable = false)
    private ResearchProject project;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "actor_id")
    private User actor;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 80)
    private ProjectActivityType type;
    @Enumerated(EnumType.STRING) @Column(name = "artifact_type", length = 80)
    private CollaborationArtifactType artifactType;
    @Column(name = "artifact_id")
    private UUID artifactId;
    @Column(name = "safe_summary", length = 500)
    private String safeSummary;
    @Column(name = "metadata_json", columnDefinition = "TEXT")
    private String metadataJson;
    @Column(name = "occurred_at", nullable = false, updatable = false)
    private OffsetDateTime occurredAt;
    @PrePersist void onCreate() { if (id == null) id = UUID.randomUUID(); if (occurredAt == null) occurredAt = OffsetDateTime.now(); }
}
