package com.researchassistant.ai.observability;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;

import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
public class AiMetrics {

    private final MeterRegistry registry;

    public AiMetrics(MeterRegistry registry) {
        this.registry = registry;
    }

    public void recordRequest(String taskType, String provider, String status, long durationMs) {
        Timer.builder("ai.request.duration")
                .tag("task_type", safeTag(taskType))
                .tag("provider", safeTag(provider))
                .tag("status", safeTag(status))
                .register(registry)
                .record(durationMs, TimeUnit.MILLISECONDS);

        Counter.builder("ai.requests.total")
                .tag("task_type", safeTag(taskType))
                .tag("provider", safeTag(provider))
                .tag("status", safeTag(status))
                .register(registry)
                .increment();
    }

    public void recordTokens(String provider, String model, int inputTokens, int outputTokens) {
        Counter.builder("ai.tokens.input")
                .tag("provider", safeTag(provider))
                .tag("model", safeTag(model))
                .register(registry)
                .increment(inputTokens);

        Counter.builder("ai.tokens.output")
                .tag("provider", safeTag(provider))
                .tag("model", safeTag(model))
                .register(registry)
                .increment(outputTokens);
    }

    public void recordEmbedding(String provider, int chunkCount) {
        Counter.builder("ai.embedding.requests")
                .tag("provider", safeTag(provider))
                .register(registry)
                .increment();

        Counter.builder("ai.embedding.chunks")
                .tag("provider", safeTag(provider))
                .register(registry)
                .increment(chunkCount);
    }

    public void recordCitationFailure() {
        Counter.builder("ai.citation.verification.failures")
                .register(registry)
                .increment();
    }

    public void recordRateLimitRejection() {
        Counter.builder("ai.rate.limit.rejections")
                .register(registry)
                .increment();
    }

    private String safeTag(String value) {
        return value == null || value.isBlank() ? "unknown" : value;
    }
}
