package com.researchassistant.websearch;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

public record WebSearchResponse(
        boolean configured,
        boolean successful,
        String errorCode,
        String errorMessage,
        String provider,
        OffsetDateTime retrievedAt,
        List<WebSearchResult> results,
        BigDecimal providerCost
) {
    public WebSearchResponse {
        results = results == null ? List.of() : List.copyOf(results);
    }

    public static WebSearchResponse notConfigured(String providerName) {
        return new WebSearchResponse(
                false,
                false,
                "WEB_SEARCH_NOT_CONFIGURED",
                "Web search is not configured for this environment.",
                providerName,
                OffsetDateTime.now(),
                List.of(),
                null
        );
    }
}
