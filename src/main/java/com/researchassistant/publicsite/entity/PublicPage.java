package com.researchassistant.publicsite.entity;

import com.researchassistant.identity.entity.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "public_pages",
        uniqueConstraints = @UniqueConstraint(name = "uk_public_pages_slug", columnNames = "slug"),
        indexes = {
                @Index(name = "idx_public_pages_slug", columnList = "slug"),
                @Index(name = "idx_public_pages_status", columnList = "status"),
                @Index(name = "idx_public_pages_nav", columnList = "show_in_navigation, navigation_order")
        })
@Getter
@Setter
@NoArgsConstructor
public class PublicPage {

    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private PublicPageType type = PublicPageType.CUSTOM;

    @Column(nullable = false, length = 120)
    private String slug;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(length = 500)
    private String subtitle;

    @Column(name = "meta_title", length = 255)
    private String metaTitle;

    @Column(name = "meta_description", columnDefinition = "TEXT")
    private String metaDescription;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private PublicPageStatus status = PublicPageStatus.DRAFT;

    @Column(name = "show_in_navigation", nullable = false)
    private boolean showInNavigation = true;

    @Column(name = "navigation_order", nullable = false)
    private int navigationOrder = 0;

    @Column(name = "published_at")
    private OffsetDateTime publishedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User createdBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "updated_by")
    private User updatedBy;

    @OneToMany(mappedBy = "page", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("displayOrder ASC")
    private List<PublicPageSection> sections = new ArrayList<>();

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
        if (slug != null) {
            slug = slug.trim().toLowerCase();
        }
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = OffsetDateTime.now();
        if (slug != null) {
            slug = slug.trim().toLowerCase();
        }
    }
}
