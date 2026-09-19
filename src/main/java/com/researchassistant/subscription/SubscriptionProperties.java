package com.researchassistant.subscription;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties(prefix = "app.subscription.renewal-reminders")
public record SubscriptionProperties(
        boolean enabled,
        List<Integer> daysBefore,
        String cron
) {
    public SubscriptionProperties {
        if (daysBefore == null || daysBefore.isEmpty()) {
            daysBefore = List.of(7, 3, 1, 0);
        }
        if (cron == null || cron.isBlank()) {
            cron = "0 0 8 * * *";
        }
    }
}
