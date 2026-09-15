package com.researchassistant.common.web;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "app.rate-limit")
public record RateLimitProperties(boolean enabled, int generalPerMinute, int sensitivePerMinute) {
    public int generalPerMinute() {
        return generalPerMinute <= 0 ? 600 : generalPerMinute;
    }

    public int sensitivePerMinute() {
        return sensitivePerMinute <= 0 ? 120 : sensitivePerMinute;
    }

    public Duration window() {
        return Duration.ofMinutes(1);
    }
}
