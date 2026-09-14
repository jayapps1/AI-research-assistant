package com.researchassistant.rag.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.rag")
public record RagProperties(
        Evidence evidence,
        Generation generation,
        Conversation conversation
) {
    public record Evidence(
            int maxItems,
            int maxTotalCharacters,
            int maxItemCharacters,
            int minimumItems
    ) {
    }

    public record Generation(
            boolean enabled,
            String provider,
            String model
    ) {
    }

    public record Conversation(int contextTurns) {
    }
}
