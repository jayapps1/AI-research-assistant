package com.researchassistant.collaboration.entity;

import com.researchassistant.identity.entity.User;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "artifact_comment_mentions", uniqueConstraints = @UniqueConstraint(name = "uk_comment_mentioned_user", columnNames = {"comment_id","mentioned_user_id"}), indexes = @Index(name = "idx_comment_mentions_user", columnList = "mentioned_user_id"))
@Getter @Setter @NoArgsConstructor
public class ArtifactCommentMention {
    @Id @Column(nullable = false, updatable = false)
    private UUID id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "comment_id", nullable = false)
    private ArtifactComment comment;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "mentioned_user_id", nullable = false)
    private User mentionedUser;
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;
    @PrePersist void onCreate() { if (id == null) id = UUID.randomUUID(); if (createdAt == null) createdAt = OffsetDateTime.now(); }
}
