package com.researchassistant.collaboration.entity;

import com.researchassistant.identity.entity.User;
import com.researchassistant.project.entity.ProjectRole;
import com.researchassistant.project.entity.ResearchProject;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "project_invitations", indexes = {
        @Index(name = "idx_project_invitations_project_status", columnList = "project_id,status"),
        @Index(name = "idx_project_invitations_email_status", columnList = "invited_email_normalized,status")
})
@Getter @Setter @NoArgsConstructor
public class ProjectInvitation {
    @Id @Column(nullable = false, updatable = false)
    private UUID id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "project_id", nullable = false)
    private ResearchProject project;
    @Column(name = "invited_email_normalized", nullable = false, length = 255)
    private String invitedEmailNormalized;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 30)
    private ProjectRole role;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 30)
    private ProjectInvitationStatus status = ProjectInvitationStatus.PENDING;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "invited_by", nullable = false)
    private User invitedBy;
    @Column(name = "token_hash", nullable = false, length = 64)
    private String tokenHash;
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;
    @Column(name = "expires_at", nullable = false)
    private OffsetDateTime expiresAt;
    @Column(name = "accepted_at")
    private OffsetDateTime acceptedAt;
    @Column(name = "declined_at")
    private OffsetDateTime declinedAt;
    @Column(name = "revoked_at")
    private OffsetDateTime revokedAt;
    @PrePersist void onCreate() { if (id == null) id = UUID.randomUUID(); if (createdAt == null) createdAt = OffsetDateTime.now(); if (status == null) status = ProjectInvitationStatus.PENDING; }
}
