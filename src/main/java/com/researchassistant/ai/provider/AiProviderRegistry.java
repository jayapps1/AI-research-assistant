package com.researchassistant.ai.provider;

import com.researchassistant.ai.config.AiProperties;
import com.researchassistant.document.embedding.DocumentEmbeddingProvider;
import com.researchassistant.document.embedding.DisabledDocumentEmbeddingProvider;
import com.researchassistant.document.embedding.EmbeddingProperties;
import com.researchassistant.rag.generation.DisabledGroundedAnswerGenerator;
import com.researchassistant.rag.generation.GroundedAnswerGenerator;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

@Service
public class AiProviderRegistry {

    private final AiProperties properties;
    private final GroundedAnswerGenerator generationProvider;
    private final DocumentEmbeddingProvider embeddingProvider;

    @Autowired
    public AiProviderRegistry(
            AiProperties properties,
            ObjectProvider<GroundedAnswerGenerator> generationProvider,
            ObjectProvider<DocumentEmbeddingProvider> embeddingProvider
    ) {
        this.properties = properties;
        this.generationProvider = generationProvider.getIfAvailable(DisabledGroundedAnswerGenerator::new);
        this.embeddingProvider = embeddingProvider.getIfAvailable(() -> new DisabledDocumentEmbeddingProvider(
                new EmbeddingProperties(false, "none", "", null, 64)
        ));
    }

    AiProviderRegistry(
            AiProperties properties,
            GroundedAnswerGenerator generationProvider,
            DocumentEmbeddingProvider embeddingProvider
    ) {
        this.properties = properties;
        this.generationProvider = generationProvider == null ? new DisabledGroundedAnswerGenerator() : generationProvider;
        this.embeddingProvider = embeddingProvider;
    }

    public AiCapabilityMetadata generation() {
        boolean enabled = properties.generation().enabled();
        boolean available = enabled && generationProvider.available();
        return new AiCapabilityMetadata(
                enabled,
                available,
                properties.generation().provider(),
                safeModel(properties.generation().model(), generationProvider.modelName()),
                health(enabled, available)
        );
    }

    public AiCapabilityMetadata embedding() {
        boolean enabled = properties.embedding().enabled();
        boolean available = enabled && embeddingProvider.available();
        return new AiCapabilityMetadata(
                enabled,
                available,
                properties.embedding().provider(),
                safeModel(properties.embedding().model(), embeddingProvider.model()),
                health(enabled, available)
        );
    }

    public boolean semanticRetrievalAvailable() {
        return properties.retrieval().semanticEnabled() && embedding().available();
    }

    private AiProviderHealthStatus health(boolean enabled, boolean available) {
        if (!enabled) {
            return AiProviderHealthStatus.DISABLED;
        }
        return available
                ? AiProviderHealthStatus.AVAILABLE
                : AiProviderHealthStatus.CONFIGURED_BUT_UNAVAILABLE;
    }

    private String safeModel(String configured, String providerModel) {
        if (configured != null && !configured.isBlank()) {
            return configured;
        }
        return providerModel == null || providerModel.isBlank() ? null : providerModel;
    }
}
