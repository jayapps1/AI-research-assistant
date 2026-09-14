package com.researchassistant.collaboration.entity;

import com.researchassistant.ai.usage.AiRequest;
import com.researchassistant.identity.entity.User;
import com.researchassistant.project.entity.ResearchProject;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "ai_artifact_proposals", indexes = @Index(name = "idx_ai_artifact_proposals_artifact", columnList = "project_id,artifact_type,artifact_id"))
@Getter @Setter @NoArgsConstructor
public class AiArtifactProposal {
    @Id @Column(nullable = false, updatable = false)
    private UUID id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "project_id", nullable = false)
    private ResearchProject project;
    @Enumerated(EnumType.STRING) @Column(name = "artifact_type", nullable = false, length = 80)
    private CollaborationArtifactType artifactType;
    @Column(name = "artifact_id", nullable = false)
    private UUID artifactId;
    @Column(name = "based_on_revision")
    private Integer basedOnRevision;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "ai_request_id")
    private AiRequest aiRequest;
    @Column(name = "proposed_content", columnDefinition = "TEXT")
    private String proposedContent;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 30)
    private AiArtifactProposalStatus status = AiArtifactProposalStatus.GENERATED;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "requested_by", nullable = false)
    private User requestedBy;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "accepted_by")
    private User acceptedBy;
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;
    @Column(name = "accepted_at")
    private OffsetDateTime acceptedAt;
    @PrePersist void onCreate() { if (id == null) id = UUID.randomUUID(); if (createdAt == null) createdAt = OffsetDateTime.now(); if (status == null) status = AiArtifactProposalStatus.GENERATED; }
}
