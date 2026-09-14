package com.researchassistant.collaboration.entity;

import com.researchassistant.project.entity.ResearchProject;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "project_collaboration_policies", uniqueConstraints = @UniqueConstraint(name = "uk_collaboration_policy_project", columnNames = "project_id"))
@Getter @Setter @NoArgsConstructor
public class ProjectCollaborationPolicy {
    @Id @Column(nullable = false, updatable = false)
    private UUID id;
    @OneToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "project_id", nullable = false)
    private ResearchProject project;
    @Column(name = "require_review_before_approval", nullable = false)
    private boolean requireReviewBeforeApproval;
    @Column(name = "allow_lead_self_approval", nullable = false)
    private boolean allowLeadSelfApproval = true;
    @Column(name = "supervisor_approval_required", nullable = false)
    private boolean supervisorApprovalRequired;
    @Column(name = "minimum_reviewers", nullable = false)
    private int minimumReviewers;
    @Column(name = "allow_editors_invite_members", nullable = false)
    private boolean allowEditorsInviteMembers;
    @Column(name = "comments_enabled", nullable = false)
    private boolean commentsEnabled = true;
    @Column(name = "task_assignments_enabled", nullable = false)
    private boolean taskAssignmentsEnabled = true;
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;
    @PrePersist void onCreate() { if (id == null) id = UUID.randomUUID(); OffsetDateTime now = OffsetDateTime.now(); if (createdAt == null) createdAt = now; updatedAt = now; }
    @PreUpdate void onUpdate() { updatedAt = OffsetDateTime.now(); }
}
