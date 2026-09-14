package com.researchassistant.collaboration.entity;

import com.researchassistant.identity.entity.User;
import com.researchassistant.project.entity.ResearchProject;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "project_tasks", indexes = {
        @Index(name = "idx_project_tasks_project_status", columnList = "project_id,status"),
        @Index(name = "idx_project_tasks_project_priority", columnList = "project_id,priority")
})
@Getter @Setter @NoArgsConstructor
public class ProjectTask {
    @Id @Column(nullable = false, updatable = false)
    private UUID id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "project_id", nullable = false)
    private ResearchProject project;
    @Column(nullable = false, length = 255)
    private String title;
    @Column(columnDefinition = "TEXT")
    private String description;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 30)
    private ProjectTaskStatus status = ProjectTaskStatus.TODO;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 30)
    private ProjectTaskPriority priority = ProjectTaskPriority.MEDIUM;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "created_by", nullable = false)
    private User createdBy;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "assigned_by")
    private User assignedBy;
    @Column(name = "due_date")
    private LocalDate dueDate;
    @Column(name = "due_at")
    private OffsetDateTime dueAt;
    @Column(name = "completed_at")
    private OffsetDateTime completedAt;
    @Version @Column(name = "version")
    private Long version;
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;
    @PrePersist void onCreate() { if (id == null) id = UUID.randomUUID(); OffsetDateTime now = OffsetDateTime.now(); if (createdAt == null) createdAt = now; updatedAt = now; if (status == null) status = ProjectTaskStatus.TODO; if (priority == null) priority = ProjectTaskPriority.MEDIUM; }
    @PreUpdate void onUpdate() { updatedAt = OffsetDateTime.now(); }
}
