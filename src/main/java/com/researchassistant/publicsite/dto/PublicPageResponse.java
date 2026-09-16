package com.researchassistant.publicsite.dto;

import com.researchassistant.publicsite.entity.PublicPage;
import com.researchassistant.publicsite.entity.PublicPageStatus;
import com.researchassistant.publicsite.entity.PublicPageType;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record PublicPageResponse(
        UUID id,
        PublicPageType type,
        String slug,
        String title,
        String subtitle,
        String metaTitle,
        String metaDescription,
        PublicPageStatus status,
        boolean showInNavigation,
        int navigationOrder,
        OffsetDateTime publishedAt,
        List<PublicPageSectionResponse> sections
) {
    public static PublicPageResponse fromEntity(PublicPage page, boolean onlyEnabledSections) {
        List<PublicPageSectionResponse> sectionResponses = page.getSections().stream()
                .filter(s -> !onlyEnabledSections || s.isEnabled())
                .map(PublicPageSectionResponse::fromEntity)
                .toList();

        return new PublicPageResponse(
                page.getId(),
                page.getType(),
                page.getSlug(),
                page.getTitle(),
                page.getSubtitle(),
                page.getMetaTitle(),
                page.getMetaDescription(),
                page.getStatus(),
                page.isShowInNavigation(),
                page.getNavigationOrder(),
                page.getPublishedAt(),
                sectionResponses
        );
    }
}
