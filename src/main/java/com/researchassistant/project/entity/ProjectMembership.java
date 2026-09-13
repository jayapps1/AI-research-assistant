package com.researchassistant.project.entity;

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
        name = "project_memberships",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_project_memberships_project_user",
                        columnNames = {"project_id", "user_id"}
                )
        },
        indexes = {
                @Index(name = "idx_project_memberships_project_id", columnList = "project_id"),
                @Index(name = "idx_project_memberships_user_id", columnList = "user_id"),
                @Index(name = "idx_project_memberships_project_status", columnList = "project_id,status"),
                @Index(name = "idx_project_memberships_user_status", columnList = "user_id,status"),
                @Index(name = "idx_project_memberships_project_role_status", columnList = "project_id,role,status")
        }
)
@Getter
@Setter
@NoArgsConstructor
public class ProjectMembership {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "project_id", nullable = false)
    private ResearchProject project;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 30)
    private ProjectRole role;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private ProjectMembershipStatus status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "added_by")
    private User addedBy;

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
            status = ProjectMembershipStatus.ACTIVE;
        }
        if (status == ProjectMembershipStatus.ACTIVE && joinedAt == null) {
            joinedAt = now;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = OffsetDateTime.now();
        if (status == ProjectMembershipStatus.ACTIVE && joinedAt == null) {
            joinedAt = OffsetDateTime.now();
        }
    }
}
