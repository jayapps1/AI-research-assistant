package com.researchassistant.publicsite.entity;

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
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(
        name = "public_statistics",
        indexes = {
                @Index(name = "idx_public_statistics_nav", columnList = "enabled, display_order"),
                @Index(name = "idx_public_statistics_code", columnList = "code")
        }
)
@Getter
@Setter
@NoArgsConstructor
public class PublicStatistic {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "code", nullable = false, length = 80, unique = true)
    private String code;

    @Column(name = "label", nullable = false, length = 150)
    private String label;

    @Column(name = "description", length = 500)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "value_source", nullable = false, length = 30)
    private PublicStatisticValueSource valueSource = PublicStatisticValueSource.SYSTEM_DERIVED;

    @Column(name = "manual_value", length = 100)
    private String manualValue;

    @Enumerated(EnumType.STRING)
    @Column(name = "system_metric", length = 80)
    private PublicSystemMetric systemMetric;

    @Column(name = "prefix", length = 20)
    private String prefix;

    @Column(name = "suffix", length = 20)
    private String suffix;

    @Column(name = "icon_key", length = 60)
    private String iconKey;

    @Column(name = "enabled", nullable = false)
    private boolean enabled = true;

    @Column(name = "featured", nullable = false)
    private boolean featured = false;

    @Column(name = "display_order", nullable = false)
    private int displayOrder = 0;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "updated_by")
    private User updatedBy;

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
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = OffsetDateTime.now();
    }
}
