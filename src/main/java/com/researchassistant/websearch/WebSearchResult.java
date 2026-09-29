package com.researchassistant.websearch;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

public record WebSearchResult(
        String title,
        String url,
        String snippet,
        String provider,
        List<String> authors,
        OffsetDateTime publishedAt,
        OffsetDateTime retrievedAt,
        WebSourceType sourceType,
        Map<String, Object> metadata
) {
    public WebSearchResult {
        authors = authors == null ? List.of() : List.copyOf(authors);
        metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
    }
}
