package com.researchassistant.websearch;

public record WebSearchRequest(
        String query,
        int maxResults,
        String freshness
) {
}
