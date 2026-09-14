package com.researchassistant.ai.observability;

import com.researchassistant.ai.provider.AiCapabilityMetadata;
import com.researchassistant.ai.provider.AiProviderRegistry;

import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.stereotype.Component;

@Component("ai")
public class AiHealthIndicator implements HealthIndicator {

    private final AiProviderRegistry registry;

    public AiHealthIndicator(AiProviderRegistry registry) {
        this.registry = registry;
    }

    @Override
    public Health health() {
        AiCapabilityMetadata generation = registry.generation();
        AiCapabilityMetadata embedding = registry.embedding();

        boolean anyEnabled = generation.enabled() || embedding.enabled();
        boolean allAvailableWhenEnabled = (!generation.enabled() || generation.available())
                && (!embedding.enabled() || embedding.available());

        Health.Builder builder = Health.up();
        builder.withDetail("generation", generation);
        builder.withDetail("embedding", embedding);
        builder.withDetail("semanticRetrievalAvailable", registry.semanticRetrievalAvailable());

        if (!anyEnabled) {
            builder.withDetail("overallStatus", "DISABLED");
        } else if (allAvailableWhenEnabled) {
            builder.withDetail("overallStatus", "AVAILABLE");
        } else {
            builder.withDetail("overallStatus", "CONFIGURED_BUT_UNAVAILABLE");
        }

        return builder.build();
    }
}
