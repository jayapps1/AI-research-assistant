package com.researchassistant.ai.provider;

import com.researchassistant.ai.dto.AiCapabilityResponse;

import org.springframework.stereotype.Service;

@Service
public class AiCapabilityService {

    private final AiProviderRegistry registry;

    public AiCapabilityService(AiProviderRegistry registry) {
        this.registry = registry;
    }

    public AiCapabilityResponse capabilities() {
        AiCapabilityMetadata generation = registry.generation();
        AiCapabilityMetadata embedding = registry.embedding();
        return new AiCapabilityResponse(
                new AiCapabilityResponse.Capability(
                        generation.enabled(),
                        generation.available(),
                        generation.provider(),
                        generation.model(),
                        generation.healthStatus()
                ),
                new AiCapabilityResponse.Capability(
                        embedding.enabled(),
                        embedding.available(),
                        embedding.provider(),
                        embedding.model(),
                        embedding.healthStatus()
                ),
                new AiCapabilityResponse.SemanticRetrieval(registry.semanticRetrievalAvailable())
        );
    }
}
