package com.researchassistant.security.totp;

import com.researchassistant.common.exception.AuthenticationFailedException;

import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Local application-level TOTP attempt limiter.
 *
 * <p>This is intentionally small and replaceable. It protects a
 * single application instance while Redis-based distributed rate
 * limiting remains deferred.</p>
 */
@Component
public class InMemoryTotpAttemptLimiter implements TotpAttemptLimiter {

    private static final int MAX_ATTEMPTS = 5;
    private static final long WINDOW_SECONDS = 300;
    private static final String INVALID_CREDENTIALS =
            "Invalid credentials.";

    private final Map<String, AttemptWindow> attempts =
            new ConcurrentHashMap<>();

    @Override
    public void checkAllowed(UUID userId, TotpVerificationPurpose purpose) {

        AttemptWindow window = attempts.get(key(userId, purpose));

        if (window == null) {
            return;
        }

        if (window.expiresAtEpochSecond() <= Instant.now().getEpochSecond()) {
            attempts.remove(key(userId, purpose));
            return;
        }

        if (window.count() >= MAX_ATTEMPTS) {
            throw new AuthenticationFailedException("RATE_LIMITED", INVALID_CREDENTIALS);
        }
    }

    @Override
    public void recordFailure(UUID userId, TotpVerificationPurpose purpose) {

        long now = Instant.now().getEpochSecond();
        String key = key(userId, purpose);

        attempts.compute(key, (ignored, existing) -> {
            if (existing == null || existing.expiresAtEpochSecond() <= now) {
                return new AttemptWindow(1, now + WINDOW_SECONDS);
            }

            return new AttemptWindow(
                    existing.count() + 1,
                    existing.expiresAtEpochSecond()
            );
        });
    }

    @Override
    public void reset(UUID userId, TotpVerificationPurpose purpose) {
        attempts.remove(key(userId, purpose));
    }

    private String key(UUID userId, TotpVerificationPurpose purpose) {
        return userId + ":" + purpose.name();
    }

    private record AttemptWindow(
            int count,
            long expiresAtEpochSecond
    ) {
    }
}
