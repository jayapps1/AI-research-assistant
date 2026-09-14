package com.researchassistant.collaboration.entity;

import com.researchassistant.identity.entity.User;
import com.researchassistant.project.entity.ProjectMembership;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "project_task_assignees", uniqueConstraints = @UniqueConstraint(name = "uk_task_assignee_membership", columnNames = {"task_id","project_membership_id"}), indexes = @Index(name = "idx_project_task_assignees_user", columnList = "user_id"))
@Getter @Setter @NoArgsConstructor
public class ProjectTaskAssignee {
    @Id @Column(nullable = false, updatable = false)
    private UUID id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "task_id", nullable = false)
    private ProjectTask task;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "project_membership_id", nullable = false)
    private ProjectMembership projectMembership;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "user_id", nullable = false)
    private User user;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "assigned_by", nullable = false)
    private User assignedBy;
    @Column(name = "assigned_at", nullable = false, updatable = false)
    private OffsetDateTime assignedAt;
    @PrePersist void onCreate() { if (id == null) id = UUID.randomUUID(); if (assignedAt == null) assignedAt = OffsetDateTime.now(); }
}
