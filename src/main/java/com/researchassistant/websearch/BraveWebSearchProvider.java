package com.researchassistant.websearch;

import com.fasterxml.jackson.databind.JsonNode;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.net.URI;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
@ConditionalOnProperty(name = "app.web-search.provider", havingValue = "brave")
public class BraveWebSearchProvider implements WebSearchProvider {

    private static final Pattern DOI_PATTERN = Pattern.compile("(?i)\\b10\\.\\d{4,9}/[-._;()/:A-Z0-9]+");

    private final WebSearchProperties properties;
    private final RestClient restClient;

    public BraveWebSearchProvider(WebSearchProperties properties, RestClient.Builder builder) {
        this.properties = properties;
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(properties.timeout());
        requestFactory.setReadTimeout(properties.timeout());
        this.restClient = builder
                .baseUrl(properties.baseUrl())
                .requestFactory(requestFactory)
                .build();
    }

    @Override
    public String providerName() {
        return WebSearchProviderType.BRAVE.name();
    }

    @Override
    public boolean available() {
        return properties.enabled()
                && properties.provider() == WebSearchProviderType.BRAVE
                && properties.apiKey() != null
                && !properties.apiKey().isBlank();
    }

    @Override
    public WebSearchResponse search(WebSearchRequest request) {
        if (!available()) {
            return WebSearchResponse.notConfigured(providerName());
        }
        OffsetDateTime retrievedAt = OffsetDateTime.now();
        int count = request.maxResults() <= 0
                ? properties.maxResults()
                : Math.min(request.maxResults(), properties.maxResults());
        try {
            JsonNode response = restClient.get()
                    .uri(uriBuilder -> {
                        var builder = uriBuilder
                                .path("/res/v1/web/search")
                                .queryParam("q", request.query())
                                .queryParam("count", count)
                                .queryParam("country", properties.country())
                                .queryParam("search_lang", properties.searchLang())
                                .queryParam("ui_lang", properties.uiLang());
                        if (request.freshness() != null && !request.freshness().isBlank()) {
                            builder.queryParam("freshness", request.freshness());
                        }
                        URI uri = builder.build();
                        return uri;
                    })
                    .accept(MediaType.APPLICATION_JSON)
                    .header("X-Subscription-Token", properties.apiKey())
                    .retrieve()
                    .body(JsonNode.class);

            List<WebSearchResult> results = new ArrayList<>();
            JsonNode webResults = response == null ? null : response.path("web").path("results");
            if (webResults != null && webResults.isArray()) {
                int rank = 1;
                for (JsonNode item : webResults) {
                    String url = text(item, "url");
                    if (url == null || url.isBlank()) {
                        continue;
                    }
                    String title = firstNonBlank(text(item, "title"), url);
                    String snippet = firstNonBlank(text(item, "description"), text(item, "snippet"), "");
                    OffsetDateTime publishedAt = parsePublishedAt(item);
                    Map<String, Object> metadata = new LinkedHashMap<>();
                    metadata.put("rank", rank++);
                    putIfPresent(metadata, "age", text(item, "age"));
                    putIfPresent(metadata, "pageAge", text(item, "page_age"));
                    putIfPresent(metadata, "language", text(item, "language"));
                    if (item.has("family_friendly")) {
                        metadata.put("familyFriendly", item.path("family_friendly").asBoolean());
                    }
                    String doi = extractDoi(url + " " + title + " " + snippet);
                    putIfPresent(metadata, "doi", doi);
                    results.add(new WebSearchResult(
                            title,
                            url,
                            snippet,
                            providerName(),
                            List.of(),
                            publishedAt,
                            retrievedAt,
                            doi == null ? WebSourceType.WEB_PAGE : WebSourceType.ACADEMIC_SOURCE,
                            metadata
                    ));
                }
            }
            return new WebSearchResponse(
                    true,
                    true,
                    null,
                    null,
                    providerName(),
                    retrievedAt,
                    results,
                    null
            );
        } catch (RestClientResponseException exception) {
            return new WebSearchResponse(
                    true,
                    false,
                    providerErrorCode(exception),
                    "Web search provider request failed.",
                    providerName(),
                    retrievedAt,
                    List.of(),
                    null
            );
        } catch (RuntimeException exception) {
            return new WebSearchResponse(
                    true,
                    false,
                    "WEB_SEARCH_PROVIDER_UNAVAILABLE",
                    "Web search provider is currently unavailable.",
                    providerName(),
                    retrievedAt,
                    List.of(),
                    null
            );
        }
    }

    private String providerErrorCode(RestClientResponseException exception) {
        int status = exception.getStatusCode().value();
        if (status == 401 || status == 403) {
            return "WEB_SEARCH_AUTHENTICATION_FAILED";
        }
        if (status == 429) {
            return "WEB_SEARCH_RATE_LIMITED";
        }
        return "WEB_SEARCH_PROVIDER_ERROR";
    }

    private OffsetDateTime parsePublishedAt(JsonNode item) {
        for (String field : List.of("published", "published_at", "date", "page_age")) {
            String value = text(item, field);
            if (value == null || value.isBlank()) {
                continue;
            }
            try {
                return OffsetDateTime.parse(value);
            } catch (DateTimeParseException ignored) {
                // Brave can return human-readable ages; preserve those in metadata instead.
            }
        }
        return null;
    }

    private String extractDoi(String value) {
        if (value == null) {
            return null;
        }
        Matcher matcher = DOI_PATTERN.matcher(value);
        if (!matcher.find()) {
            return null;
        }
        String doi = matcher.group();
        while (doi.endsWith(".") || doi.endsWith(",") || doi.endsWith(";") || doi.endsWith(")")) {
            doi = doi.substring(0, doi.length() - 1);
        }
        return doi.toLowerCase(Locale.ROOT);
    }

    private String text(JsonNode node, String field) {
        if (node == null || !node.hasNonNull(field)) {
            return null;
        }
        String value = node.path(field).asText();
        return value == null || value.isBlank() ? null : value.trim();
    }

    private void putIfPresent(Map<String, Object> metadata, String key, String value) {
        if (value != null && !value.isBlank()) {
            metadata.put(key, value);
        }
    }

    private String firstNonBlank(String first, String second) {
        return firstNonBlank(first, second, null);
    }

    private String firstNonBlank(String first, String second, String third) {
        if (first != null && !first.isBlank()) {
            return first.trim();
        }
        if (second != null && !second.isBlank()) {
            return second.trim();
        }
        return third == null ? null : third.trim();
    }
}
