package com.researchassistant.billing;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

@Component
public class PaymentMetrics {
    public PaymentMetrics(MeterRegistry registry, PaymentAttemptRepository repository) {
        Gauge.builder("researchassistant.payment.attempts.pending", repository,
                        r -> r.countByStatus(PaymentAttemptStatus.PENDING))
                .description("Pending payment attempts")
                .register(registry);
    }
}
