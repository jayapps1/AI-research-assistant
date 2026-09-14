package com.researchassistant.ai.provider;

public record AiCapabilityMetadata(
        boolean enabled,
        boolean available,
        AiProviderType provider,
        String model,
        AiProviderHealthStatus healthStatus
) {
}
