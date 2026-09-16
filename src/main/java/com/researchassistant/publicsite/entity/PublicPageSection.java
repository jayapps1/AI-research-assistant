package com.researchassistant.publicsite.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "public_page_sections",
        uniqueConstraints = @UniqueConstraint(name = "uk_public_page_sections_page_key", columnNames = {"page_id", "section_key"}),
        indexes = @Index(name = "idx_public_page_sections_page_order", columnList = "page_id, display_order"))
@Getter
@Setter
@NoArgsConstructor
public class PublicPageSection {

    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "page_id", nullable = false)
    private PublicPage page;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private PublicSectionType type = PublicSectionType.TEXT;

    @Column(name = "section_key", nullable = false, length = 100)
    private String sectionKey;

    @Column(length = 200)
    private String eyebrow;

    @Column(length = 300)
    private String heading;

    @Column(length = 500)
    private String subheading;

    @Column(columnDefinition = "TEXT")
    private String body;

    @Column(name = "image_storage_key", length = 255)
    private String imageStorageKey;

    @Column(name = "primary_cta_label", length = 100)
    private String primaryCtaLabel;

    @Column(name = "primary_cta_url", length = 500)
    private String primaryCtaUrl;

    @Column(name = "secondary_cta_label", length = 100)
    private String secondaryCtaLabel;

    @Column(name = "secondary_cta_url", length = 500)
    private String secondaryCtaUrl;

    @Column(name = "configuration_json", columnDefinition = "TEXT")
    private String configurationJson;

    @Column(nullable = false)
    private boolean enabled = true;

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
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = OffsetDateTime.now();
    }
}
