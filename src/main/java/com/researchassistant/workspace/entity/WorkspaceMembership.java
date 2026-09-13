package com.researchassistant.workspace.entity;

import com.researchassistant.identity.entity.User;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(
        name = "workspace_memberships",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_workspace_memberships_workspace_user",
                        columnNames = {"workspace_id", "user_id"}
                )
        },
        indexes = {
                @Index(name = "idx_workspace_memberships_workspace_id", columnList = "workspace_id"),
                @Index(name = "idx_workspace_memberships_user_id", columnList = "user_id"),
                @Index(name = "idx_workspace_memberships_workspace_status", columnList = "workspace_id,status"),
                @Index(name = "idx_workspace_memberships_user_status", columnList = "user_id,status")
        }
)
@Getter
@Setter
@NoArgsConstructor
public class WorkspaceMembership {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "workspace_id", nullable = false)
    private Workspace workspace;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 30)
    private WorkspaceRole role;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private WorkspaceMembershipStatus status;

    @Column(name = "joined_at")
    private OffsetDateTime joinedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        OffsetDateTime now = OffsetDateTime.now();
        if (createdAt == null) {
            createdAt = now;
        }
        updatedAt = now;
        if (status == null) {
            status = WorkspaceMembershipStatus.ACTIVE;
        }
        if (status == WorkspaceMembershipStatus.ACTIVE && joinedAt == null) {
            joinedAt = now;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = OffsetDateTime.now();
        if (status == WorkspaceMembershipStatus.ACTIVE && joinedAt == null) {
            joinedAt = OffsetDateTime.now();
        }
    }
}
