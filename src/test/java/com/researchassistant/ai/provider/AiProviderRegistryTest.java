package com.researchassistant.ai.provider;

import com.researchassistant.ai.config.AiProperties;
import com.researchassistant.ai.fake.FakeAiEmbeddingProvider;
import com.researchassistant.ai.fake.FakeAiGenerationProvider;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

class AiProviderRegistryTest {

    private AiProperties properties;
    private FakeAiGenerationProvider generationProvider;
    private FakeAiEmbeddingProvider embeddingProvider;
    private AiProviderRegistry registry;

    @BeforeEach
    void setUp() {
        properties = new AiProperties(
                new AiProperties.Generation(true, AiProviderType.OPENAI, "gpt-4", 0.2, 4096, Duration.ofSeconds(60)),
                new AiProperties.Embedding(true, AiProviderType.OPENAI, "text-embedding-3-small", 1536, 64, Duration.ofSeconds(60)),
                new AiProperties.Retrieval(true, true),
                new AiProperties.Privacy(false),
                new AiProperties.Context(12, 30000, 6, 4096),
                new AiProperties.RateLimit(30, 120, 2, 10)
        );
        generationProvider = new FakeAiGenerationProvider();
        embeddingProvider = new FakeAiEmbeddingProvider();
        registry = new AiProviderRegistry(properties, generationProvider, embeddingProvider);
    }

    @Test
    void shouldReportAvailableWhenEnabledAndAvailable() {
        AiCapabilityMetadata genMetadata = registry.generation();
        assertThat(genMetadata.enabled()).isTrue();
        assertThat(genMetadata.available()).isTrue();
        assertThat(genMetadata.healthStatus()).isEqualTo(AiProviderHealthStatus.AVAILABLE);

        AiCapabilityMetadata embMetadata = registry.embedding();
        assertThat(embMetadata.enabled()).isTrue();
        assertThat(embMetadata.available()).isTrue();
        assertThat(embMetadata.healthStatus()).isEqualTo(AiProviderHealthStatus.AVAILABLE);

        assertThat(registry.semanticRetrievalAvailable()).isTrue();
    }

    @Test
    void shouldReportDisabledWhenConfiguredDisabled() {
        AiProperties disabledProperties = new AiProperties(
                new AiProperties.Generation(false, AiProviderType.NONE, "", 0.2, 4096, Duration.ofSeconds(60)),
                new AiProperties.Embedding(false, AiProviderType.NONE, "", 1536, 64, Duration.ofSeconds(60)),
                new AiProperties.Retrieval(true, false),
                new AiProperties.Privacy(false),
                new AiProperties.Context(12, 30000, 6, 4096),
                new AiProperties.RateLimit(30, 120, 2, 10)
        );
        AiProviderRegistry disabledRegistry = new AiProviderRegistry(disabledProperties, generationProvider, embeddingProvider);

        assertThat(disabledRegistry.generation().enabled()).isFalse();
        assertThat(disabledRegistry.generation().healthStatus()).isEqualTo(AiProviderHealthStatus.DISABLED);
        assertThat(disabledRegistry.embedding().enabled()).isFalse();
        assertThat(disabledRegistry.embedding().healthStatus()).isEqualTo(AiProviderHealthStatus.DISABLED);
        assertThat(disabledRegistry.semanticRetrievalAvailable()).isFalse();
    }
}
