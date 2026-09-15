package com.researchassistant.usage;

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
@Table(name = "usage_ledger_entries",
        uniqueConstraints = @UniqueConstraint(name = "uk_usage_ledger_idempotency", columnNames = "idempotency_key"),
        indexes = @Index(name = "idx_usage_ledger_workspace_metric_time", columnList = "workspace_id,metric,occurred_at"))
@Getter
@Setter
@NoArgsConstructor
public class UsageLedgerEntry {
    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "workspace_id", nullable = false)
    private Workspace workspace;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id")
    private ResearchProject project;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 80)
    private UsageMetricType metric;

    @Column(nullable = false)
    private long quantity;

    @Column(name = "source_type", nullable = false, length = 100)
    private String sourceType;

    @Column(name = "source_id")
    private UUID sourceId;

    @Column(name = "idempotency_key", length = 180)
    private String idempotencyKey;

    @Column(name = "occurred_at", nullable = false)
    private OffsetDateTime occurredAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @PrePersist
    void onCreate() {
        if (id == null) id = UUID.randomUUID();
        OffsetDateTime now = OffsetDateTime.now();
        if (occurredAt == null) occurredAt = now;
        if (createdAt == null) createdAt = now;
    }
}
