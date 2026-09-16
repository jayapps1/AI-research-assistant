package com.researchassistant.publicsite.dto;

import com.researchassistant.publicsite.entity.PublicSiteSettings;

import java.time.OffsetDateTime;

public record PublicSiteSettingsResponse(
        String siteName,
        String tagline,
        String supportEmail,
        String supportPhone,
        String contactAddress,
        String facebookUrl,
        String instagramUrl,
        String linkedinUrl,
        String youtubeUrl,
        String xUrl,
        String logoStorageKey,
        String faviconStorageKey,
        String defaultMetaTitle,
        String defaultMetaDescription,
        String copyrightText,
        boolean registrationEnabled,
        boolean publicPricingEnabled,
        OffsetDateTime updatedAt
) {
    public static PublicSiteSettingsResponse fromEntity(PublicSiteSettings settings) {
        if (settings == null) {
            return new PublicSiteSettingsResponse(
                    "AI Research Assistant",
                    "From Research Question to Final Report.",
                    "support@researchassistant.ai",
                    null,
                    null,
                    null, null, null, null, null,
                    null, null,
                    "AI Research Assistant — Grounded Academic Research Platform",
                    "A responsible, source-grounded research assistant supporting literature review, methodology, data analysis, and traceable report generation.",
                    "© 2026 AI Research Assistant. All rights reserved.",
                    true,
                    true,
                    OffsetDateTime.now()
            );
        }
        return new PublicSiteSettingsResponse(
                settings.getSiteName(),
                settings.getTagline(),
                settings.getSupportEmail(),
                settings.getSupportPhone(),
                settings.getContactAddress(),
                settings.getFacebookUrl(),
                settings.getInstagramUrl(),
                settings.getLinkedinUrl(),
                settings.getYoutubeUrl(),
                settings.getXUrl(),
                settings.getLogoStorageKey(),
                settings.getFaviconStorageKey(),
                settings.getDefaultMetaTitle(),
                settings.getDefaultMetaDescription(),
                settings.getCopyrightText(),
                settings.isRegistrationEnabled(),
                settings.isPublicPricingEnabled(),
                settings.getUpdatedAt()
        );
    }
}
