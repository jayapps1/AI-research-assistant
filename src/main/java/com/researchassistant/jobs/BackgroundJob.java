package com.researchassistant.jobs;

import com.researchassistant.identity.entity.User;
import com.researchassistant.project.entity.ResearchProject;
import com.researchassistant.workspace.entity.Workspace;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "background_jobs",
        indexes = {
                @Index(name = "idx_background_jobs_status_next", columnList = "status,next_attempt_at"),
                @Index(name = "idx_background_jobs_workspace", columnList = "workspace_id,created_at")
        })
@Getter
@Setter
@NoArgsConstructor
public class BackgroundJob {
    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 60)
    private BackgroundJobType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private BackgroundJobStatus status = BackgroundJobStatus.QUEUED;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "workspace_id")
    private Workspace workspace;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id")
    private ResearchProject project;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "requested_by")
    private User requestedBy;

    @Column(name = "source_type", length = 80)
    private String sourceType;

    @Column(name = "source_id")
    private UUID sourceId;

    @Column(nullable = false)
    private int attempts;

    @Column(name = "max_attempts", nullable = false)
    private int maxAttempts = 3;

    @Column(name = "progress_percent")
    private Integer progressPercent;

    @Column(name = "failure_code", length = 100)
    private String failureCode;

    @Column(name = "failure_message_safe", length = 500)
    private String failureMessageSafe;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "started_at")
    private OffsetDateTime startedAt;

    @Column(name = "completed_at")
    private OffsetDateTime completedAt;

    @Column(name = "next_attempt_at")
    private OffsetDateTime nextAttemptAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @PrePersist
    void onCreate() {
        if (id == null) id = UUID.randomUUID();
        OffsetDateTime now = OffsetDateTime.now();
        if (createdAt == null) createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = OffsetDateTime.now();
    }
}
