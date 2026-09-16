package com.researchassistant.publicsite.entity;

import com.researchassistant.identity.entity.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "public_site_settings")
@Getter
@Setter
@NoArgsConstructor
public class PublicSiteSettings {

    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(name = "site_name", nullable = false, length = 120)
    private String siteName = "AI Research Assistant";

    @Column(length = 255)
    private String tagline;

    @Column(name = "support_email", length = 254)
    private String supportEmail;

    @Column(name = "support_phone", length = 50)
    private String supportPhone;

    @Column(name = "contact_address", length = 500)
    private String contactAddress;

    @Column(name = "facebook_url", length = 500)
    private String facebookUrl;

    @Column(name = "instagram_url", length = 500)
    private String instagramUrl;

    @Column(name = "linkedin_url", length = 500)
    private String linkedinUrl;

    @Column(name = "youtube_url", length = 500)
    private String youtubeUrl;

    @Column(name = "x_url", length = 500)
    private String xUrl;

    @Column(name = "logo_storage_key", length = 255)
    private String logoStorageKey;

    @Column(name = "favicon_storage_key", length = 255)
    private String faviconStorageKey;

    @Column(name = "default_meta_title", length = 255)
    private String defaultMetaTitle;

    @Column(name = "default_meta_description", columnDefinition = "TEXT")
    private String defaultMetaDescription;

    @Column(name = "copyright_text", length = 255)
    private String copyrightText;

    @Column(name = "registration_enabled", nullable = false)
    private boolean registrationEnabled = true;

    @Column(name = "public_pricing_enabled", nullable = false)
    private boolean publicPricingEnabled = true;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "updated_by")
    private User updatedBy;

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
