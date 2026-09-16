package com.researchassistant.publicsite.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateSiteSettingsRequest(
        @NotBlank(message = "Site name is required.")
        @Size(max = 120, message = "Site name must not exceed 120 characters.")
        String siteName,

        @Size(max = 255)
        String tagline,

        @Size(max = 254)
        String supportEmail,

        @Size(max = 50)
        String supportPhone,

        @Size(max = 500)
        String contactAddress,

        @Size(max = 500)
        String facebookUrl,

        @Size(max = 500)
        String instagramUrl,

        @Size(max = 500)
        String linkedinUrl,

        @Size(max = 500)
        String youtubeUrl,

        @Size(max = 500)
        String xUrl,

        @Size(max = 255)
        String logoStorageKey,

        @Size(max = 255)
        String faviconStorageKey,

        @Size(max = 255)
        String defaultMetaTitle,

        String defaultMetaDescription,

        @Size(max = 255)
        String copyrightText,

        Boolean registrationEnabled,

        Boolean publicPricingEnabled
) {}
