package com.researchassistant.usage;

import com.researchassistant.workspace.entity.Workspace;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "usage_reservations",
        uniqueConstraints = @UniqueConstraint(name = "uk_usage_reservations_idempotency", columnNames = "idempotency_key"),
        indexes = @Index(name = "idx_usage_reservations_workspace_metric_status", columnList = "workspace_id,metric,status"))
@Getter
@Setter
@NoArgsConstructor
public class UsageReservation {
    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "workspace_id", nullable = false)
    private Workspace workspace;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 80)
    private UsageMetricType metric;

    @Column(nullable = false)
    private long quantity;

    @Column(nullable = false, length = 30)
    private String status = "HELD";

    @Column(name = "idempotency_key", nullable = false, length = 180)
    private String idempotencyKey;

    @Column(name = "expires_at", nullable = false)
    private OffsetDateTime expiresAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @PrePersist
    void onCreate() {
        if (id == null) id = UUID.randomUUID();
        OffsetDateTime now = OffsetDateTime.now();
        if (createdAt == null) createdAt = now;
        if (expiresAt == null) expiresAt = now.plusMinutes(10);
    }
}
