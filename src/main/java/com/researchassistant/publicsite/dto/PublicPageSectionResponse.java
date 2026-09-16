package com.researchassistant.publicsite.dto;

import com.researchassistant.publicsite.entity.PublicPageSection;
import com.researchassistant.publicsite.entity.PublicSectionType;

import java.util.UUID;

public record PublicPageSectionResponse(
        UUID id,
        PublicSectionType type,
        String sectionKey,
        String eyebrow,
        String heading,
        String subheading,
        String body,
        String imageStorageKey,
        String primaryCtaLabel,
        String primaryCtaUrl,
        String secondaryCtaLabel,
        String secondaryCtaUrl,
        String configurationJson,
        boolean enabled,
        int displayOrder
) {
    public static PublicPageSectionResponse fromEntity(PublicPageSection section) {
        return new PublicPageSectionResponse(
                section.getId(),
                section.getType(),
                section.getSectionKey(),
                section.getEyebrow(),
                section.getHeading(),
                section.getSubheading(),
                section.getBody(),
                section.getImageStorageKey(),
                section.getPrimaryCtaLabel(),
                section.getPrimaryCtaUrl(),
                section.getSecondaryCtaLabel(),
                section.getSecondaryCtaUrl(),
                section.getConfigurationJson(),
                section.isEnabled(),
                section.getDisplayOrder()
        );
    }
}
