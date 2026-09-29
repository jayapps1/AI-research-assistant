package com.researchassistant.websearch;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "app.web-search")
public record WebSearchProperties(
        boolean enabled,
        WebSearchProviderType provider,
        String apiKey,
        String baseUrl,
        String country,
        String searchLang,
        String uiLang,
        int maxResults,
        Duration timeout
) {
    public WebSearchProperties {
        provider = provider == null ? WebSearchProviderType.NONE : provider;
        baseUrl = baseUrl == null || baseUrl.isBlank()
                ? "https://api.search.brave.com"
                : baseUrl.trim();
        country = country == null || country.isBlank() ? "US" : country.trim();
        searchLang = searchLang == null || searchLang.isBlank() ? "en" : searchLang.trim();
        uiLang = uiLang == null || uiLang.isBlank() ? "en-US" : uiLang.trim();
        maxResults = maxResults <= 0 ? 8 : Math.min(maxResults, 20);
        timeout = timeout == null ? Duration.ofSeconds(15) : timeout;
    }
}
