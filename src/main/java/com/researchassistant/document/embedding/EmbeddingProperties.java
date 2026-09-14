package com.researchassistant.document.embedding;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "app.ai.embedding")
public record EmbeddingProperties(
        boolean enabled,
        String provider,
        String model,
        String dimensions,
        int batchSize,
        Duration timeout
) {
    public EmbeddingProperties() {
        this(false, "none", "", null, 64, Duration.ofSeconds(60));
    }

    public EmbeddingProperties(
            boolean enabled,
            String provider,
            String model,
            String dimensions,
            int batchSize
    ) {
        this(enabled, provider, model, dimensions, batchSize, Duration.ofSeconds(60));
    }

    public EmbeddingProperties {
        provider = provider == null || provider.isBlank() ? "none" : provider;
        model = model == null ? "" : model;
        batchSize = batchSize <= 0 ? 64 : batchSize;
        timeout = timeout == null ? Duration.ofSeconds(60) : timeout;
    }

    public Integer parsedDimensions() {
        if (dimensions == null || dimensions.isBlank()) {
            return null;
        }
        return Integer.valueOf(dimensions);
    }
}
