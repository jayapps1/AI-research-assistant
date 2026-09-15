package com.researchassistant.billing;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "app.payment.paystack")
public record PaymentProperties(
        boolean enabled,
        PaymentEnvironment mode,
        @JsonIgnore
        String secretKey,
        @JsonIgnore
        String publicKey,
        String callbackBaseUrl,
        boolean liveEnabled,
        Duration connectTimeout,
        Duration readTimeout,
        int maxAttemptsPerIntent,
        Duration retryCooldown,
        Duration intentTtl,
        Duration attemptTtl
) {
    public PaymentEnvironment mode() {
        return mode == null ? PaymentEnvironment.TEST : mode;
    }

    public Duration connectTimeout() {
        return connectTimeout == null ? Duration.ofSeconds(10) : connectTimeout;
    }

    public Duration readTimeout() {
        return readTimeout == null ? Duration.ofSeconds(30) : readTimeout;
    }

    public int maxAttemptsPerIntent() {
        return maxAttemptsPerIntent < 1 ? 5 : maxAttemptsPerIntent;
    }

    public Duration retryCooldown() {
        return retryCooldown == null ? Duration.ofSeconds(15) : retryCooldown;
    }

    public Duration intentTtl() {
        return intentTtl == null ? Duration.ofHours(24) : intentTtl;
    }

    public Duration attemptTtl() {
        return attemptTtl == null ? Duration.ofMinutes(30) : attemptTtl;
    }

    @Override
    public String toString() {
        return "PaymentProperties[enabled=%s, mode=%s, secretKey=<redacted>, publicKey=<redacted>, callbackBaseUrl=%s, liveEnabled=%s, connectTimeout=%s, readTimeout=%s, maxAttemptsPerIntent=%s, retryCooldown=%s, intentTtl=%s, attemptTtl=%s]"
                .formatted(enabled, mode(), callbackBaseUrl, liveEnabled, connectTimeout(), readTimeout(),
                        maxAttemptsPerIntent(), retryCooldown(), intentTtl(), attemptTtl());
    }
}
