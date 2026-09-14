package com.researchassistant.ai.config;

import com.researchassistant.ai.provider.AiProviderType;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "app.ai")
public record AiProperties(
        Generation generation,
        Embedding embedding,
        Retrieval retrieval,
        Privacy privacy,
        Context context,
        RateLimit rateLimit
) {
    public AiProperties {
        generation = generation == null ? new Generation(false, AiProviderType.NONE, "", 0.2, 4096, Duration.ofSeconds(60)) : generation;
        embedding = embedding == null ? new Embedding(false, AiProviderType.NONE, "", null, 64, Duration.ofSeconds(60)) : embedding;
        retrieval = retrieval == null ? new Retrieval(true, false) : retrieval;
        privacy = privacy == null ? new Privacy(false) : privacy;
        context = context == null ? new Context(12, 30000, 6, 4096) : context;
        rateLimit = rateLimit == null ? new RateLimit(30, 120, 2, 10) : rateLimit;
    }

    public record Generation(
            boolean enabled,
            AiProviderType provider,
            String model,
            double temperature,
            int maxOutputTokens,
            Duration timeout
    ) {
    }

    public record Embedding(
            boolean enabled,
            AiProviderType provider,
            String model,
            Integer dimensions,
            int batchSize,
            Duration timeout
    ) {
    }

    public record Retrieval(boolean lexicalEnabled, boolean semanticEnabled) {
    }

    public record Privacy(boolean externalResearchContentEnabled) {
    }

    public record Context(
            int maxEvidenceItems,
            int maxEvidenceCharacters,
            int maxConversationTurns,
            int maxOutputTokens
    ) {
    }

    public record RateLimit(
            int userRequestsPerMinute,
            int workspaceRequestsPerMinute,
            int userConcurrentRequests,
            int workspaceConcurrentRequests
    ) {
    }
}
