package com.researchassistant.ai.dto;

import com.researchassistant.ai.provider.AiProviderHealthStatus;
import com.researchassistant.ai.provider.AiProviderType;

public record AiCapabilityResponse(
        Capability generation,
        Capability embedding,
        SemanticRetrieval semanticRetrieval
) {
    public record Capability(
            boolean enabled,
            boolean available,
            AiProviderType provider,
            String model,
            AiProviderHealthStatus healthStatus
    ) {
    }

    public record SemanticRetrieval(boolean available) {
    }
}
