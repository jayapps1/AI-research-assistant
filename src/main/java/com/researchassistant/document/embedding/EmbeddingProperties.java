package com.researchassistant.document.embedding;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.ai.embeddings")
public record EmbeddingProperties(
        boolean enabled,
        String provider,
        String model,
        String dimensions,
        int batchSize
) {

    public Integer parsedDimensions() {
        if (dimensions == null || dimensions.isBlank()) {
            return null;
        }
        return Integer.valueOf(dimensions);
    }
}
