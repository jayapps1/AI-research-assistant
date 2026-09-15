package com.researchassistant.jobs;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

@Component
public class BackgroundJobMetrics {
    public BackgroundJobMetrics(MeterRegistry registry, BackgroundJobRepository repository) {
        Gauge.builder("researchassistant.background.jobs.queued", repository,
                        r -> r.countByStatus(BackgroundJobStatus.QUEUED))
                .description("Queued durable background jobs")
                .register(registry);
        Gauge.builder("researchassistant.background.jobs.dead_letter", repository,
                        r -> r.countByStatus(BackgroundJobStatus.DEAD_LETTER))
                .description("Dead-letter durable background jobs")
                .register(registry);
    }
}
