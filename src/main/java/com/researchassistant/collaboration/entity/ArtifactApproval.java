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
@Table(name = "artifact_approvals", indexes = @Index(name = "idx_artifact_approvals_artifact", columnList = "project_id,artifact_type,artifact_id"))
@Getter @Setter @NoArgsConstructor
public class ArtifactApproval {
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
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 30)
    private ArtifactApprovalStatus status = ArtifactApprovalStatus.SUBMITTED;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "submitted_by", nullable = false)
    private User submittedBy;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "decided_by")
    private User decidedBy;
    @Column(name = "decision_comment", columnDefinition = "TEXT")
    private String decisionComment;
    @Column(name = "submitted_at", nullable = false, updatable = false)
    private OffsetDateTime submittedAt;
    @Column(name = "decided_at")
    private OffsetDateTime decidedAt;
    @PrePersist void onCreate() { if (id == null) id = UUID.randomUUID(); if (submittedAt == null) submittedAt = OffsetDateTime.now(); if (status == null) status = ArtifactApprovalStatus.SUBMITTED; if (artifactRevision < 1) artifactRevision = 1; }
}
