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
@Table(name = "artifact_comments", indexes = {
        @Index(name = "idx_artifact_comments_artifact", columnList = "project_id,artifact_type,artifact_id"),
        @Index(name = "idx_artifact_comments_parent", columnList = "parent_comment_id")
})
@Getter @Setter @NoArgsConstructor
public class ArtifactComment {
    @Id @Column(nullable = false, updatable = false)
    private UUID id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "project_id", nullable = false)
    private ResearchProject project;
    @Enumerated(EnumType.STRING) @Column(name = "artifact_type", nullable = false, length = 80)
    private CollaborationArtifactType artifactType;
    @Column(name = "artifact_id", nullable = false)
    private UUID artifactId;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "author_id", nullable = false)
    private User author;
    @Column(name = "parent_comment_id")
    private UUID parentCommentId;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "review_id")
    private ArtifactReview review;
    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 30)
    private ArtifactCommentStatus status = ArtifactCommentStatus.OPEN;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "resolved_by")
    private User resolvedBy;
    @Column(name = "resolved_at")
    private OffsetDateTime resolvedAt;
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;
    @PrePersist void onCreate() { if (id == null) id = UUID.randomUUID(); OffsetDateTime now = OffsetDateTime.now(); if (createdAt == null) createdAt = now; updatedAt = now; if (status == null) status = ArtifactCommentStatus.OPEN; }
    @PreUpdate void onUpdate() { updatedAt = OffsetDateTime.now(); }
}
