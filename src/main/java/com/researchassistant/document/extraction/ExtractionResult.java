package com.researchassistant.document.extraction;

import java.util.List;

public record ExtractionResult(
        String extractor,
        String extractorVersion,
        List<ExtractedPage> pages
) {
}
