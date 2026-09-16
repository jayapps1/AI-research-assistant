package com.researchassistant.publicsite.dto;

import com.researchassistant.publicsite.entity.FaqCategory;
import com.researchassistant.publicsite.entity.FaqItem;

import java.util.UUID;

public record FaqItemResponse(
        UUID id,
        String question,
        String answer,
        FaqCategory category,
        boolean featured,
        boolean published,
        int displayOrder
) {
    public static FaqItemResponse fromEntity(FaqItem item) {
        return new FaqItemResponse(
                item.getId(),
                item.getQuestion(),
                item.getAnswer(),
                item.getCategory(),
                item.isFeatured(),
                item.isPublished(),
                item.getDisplayOrder()
        );
    }
}
