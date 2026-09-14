package com.researchassistant.cache;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "app.cache")
public record AppCacheProperties(
        CacheProvider provider,
        String keyPrefix,
        Duration projectMetadataTtl,
        Duration documentMetadataTtl,
        Duration literatureSummaryTtl,
        Duration researchDesignValidationTtl
) {

    public AppCacheProperties {
        if (provider == null) {
            provider = CacheProvider.SIMPLE;
        }
        if (keyPrefix == null || keyPrefix.isBlank()) {
            keyPrefix = "research-assistant:v1:";
        }
        if (projectMetadataTtl == null) {
            projectMetadataTtl = Duration.ofMinutes(5);
        }
        if (documentMetadataTtl == null) {
            documentMetadataTtl = Duration.ofMinutes(5);
        }
        if (literatureSummaryTtl == null) {
            literatureSummaryTtl = Duration.ofMinutes(5);
        }
        if (researchDesignValidationTtl == null) {
            researchDesignValidationTtl = Duration.ofMinutes(1);
        }
    }
}
