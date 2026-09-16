package com.researchassistant.publicsite.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "service_offerings",
        uniqueConstraints = @UniqueConstraint(name = "uk_service_offerings_code", columnNames = "code"),
        indexes = @Index(name = "idx_service_offerings_status_order", columnList = "status, display_order"))
@Getter
@Setter
@NoArgsConstructor
public class ServiceOffering {

    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(nullable = false, length = 80)
    private String code;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(name = "short_description", nullable = false, length = 500)
    private String shortDescription;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "icon_key", length = 100)
    private String iconKey;

    @Column(name = "feature_list_json", columnDefinition = "TEXT")
    private String featureListJson;

    @Column(name = "cta_label", length = 100)
    private String ctaLabel;

    @Column(name = "cta_url", length = 500)
    private String ctaUrl;

    @Column(nullable = false)
    private boolean featured = false;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ServiceOfferingStatus status = ServiceOfferingStatus.ACTIVE;

    @Column(name = "display_order", nullable = false)
    private int displayOrder = 0;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @PrePersist
    void onCreate() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        OffsetDateTime now = OffsetDateTime.now();
        if (createdAt == null) {
            createdAt = now;
        }
        updatedAt = now;
        if (code != null) {
            code = code.trim().toUpperCase();
        }
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = OffsetDateTime.now();
        if (code != null) {
            code = code.trim().toUpperCase();
        }
    }
}
