package com.researchassistant.publicsite.dto;

import com.researchassistant.publicsite.entity.PublicPage;
import com.researchassistant.publicsite.entity.PublicPageStatus;
import com.researchassistant.publicsite.entity.PublicPageType;

import java.time.OffsetDateTime;
import java.util.UUID;

public record PublicPageSummaryResponse(
        UUID id,
        PublicPageType type,
        String slug,
        String title,
        String subtitle,
        PublicPageStatus status,
        boolean showInNavigation,
        int navigationOrder,
        OffsetDateTime publishedAt,
        int sectionCount
) {
    public static PublicPageSummaryResponse fromEntity(PublicPage page) {
        return new PublicPageSummaryResponse(
                page.getId(),
                page.getType(),
                page.getSlug(),
                page.getTitle(),
                page.getSubtitle(),
                page.getStatus(),
                page.isShowInNavigation(),
                page.getNavigationOrder(),
                page.getPublishedAt(),
                page.getSections() == null ? 0 : page.getSections().size()
        );
    }
}
