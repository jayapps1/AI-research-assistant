package com.researchassistant.document.extraction;

public record ExtractedPage(
        int pageNumber,
        String sourceLabel,
        String text
) {
}
