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
@Table(name = "artifact_reviews", indexes = {
        @Index(name = "idx_artifact_reviews_project_status", columnList = "project_id,status"),
        @Index(name = "idx_artifact_reviews_artifact", columnList = "project_id,artifact_type,artifact_id")
})
@Getter @Setter @NoArgsConstructor
public class ArtifactReview {
    @Id @Column(nullable = false, updatable = false)
    private UUID id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "project_id", nullable = false)
    private ResearchProject project;
    @Enumerated(EnumType.STRING) @Column(name = "artifact_type", nullable = false, length = 80)
    private CollaborationArtifactType artifactType;
    @Column(name = "artifact_id", nullable = false)
    private UUID artifactId;
    @Column(name = "artifact_revision", nullable = false)
    private int artifactRevision;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "requested_by", nullable = false)
    private User requestedBy;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "reviewer_id", nullable = false)
    private User reviewer;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 30)
    private ArtifactReviewStatus status = ArtifactReviewStatus.REQUESTED;
    @Column(columnDefinition = "TEXT")
    private String summary;
    @Column(name = "requested_at", nullable = false, updatable = false)
    private OffsetDateTime requestedAt;
    @Column(name = "completed_at")
    private OffsetDateTime completedAt;
    @PrePersist void onCreate() { if (id == null) id = UUID.randomUUID(); if (requestedAt == null) requestedAt = OffsetDateTime.now(); if (status == null) status = ArtifactReviewStatus.REQUESTED; if (artifactRevision < 1) artifactRevision = 1; }
}
