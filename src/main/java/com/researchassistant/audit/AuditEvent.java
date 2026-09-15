package com.researchassistant.audit;

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
@Table(name = "audit_events",
        indexes = {
                @Index(name = "idx_audit_events_occurred", columnList = "occurred_at"),
                @Index(name = "idx_audit_events_actor_time", columnList = "actor_id,occurred_at"),
                @Index(name = "idx_audit_events_workspace", columnList = "workspace_id,occurred_at"),
                @Index(name = "idx_audit_events_type", columnList = "type")
        })
@Getter
@Setter
@NoArgsConstructor
public class AuditEvent {
    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "actor_id")
    private User actor;

    @Column(name = "actor_type", nullable = false, length = 40)
    private String actorType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "workspace_id")
    private Workspace workspace;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id")
    private ResearchProject project;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 80)
    private AuditEventType type;

    @Column(name = "target_type", length = 120)
    private String targetType;

    @Column(name = "target_id")
    private UUID targetId;

    @Column(name = "ip_address_hash", length = 128)
    private String ipAddressHash;

    @Column(name = "user_agent_summary", length = 255)
    private String userAgentSummary;

    @Column(name = "metadata_json", columnDefinition = "TEXT")
    private String metadataJson;

    @Column(name = "occurred_at", nullable = false, updatable = false)
    private OffsetDateTime occurredAt;

    @PrePersist
    void onCreate() {
        if (id == null) id = UUID.randomUUID();
        if (occurredAt == null) occurredAt = OffsetDateTime.now();
    }
}
