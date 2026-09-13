package com.researchassistant.security.totp;

import java.util.UUID;

/**
 * Guards TOTP verification against brute-force attempts.
 *
 * <p>This boundary can later be backed by Redis or another
 * distributed rate limiter without changing TOTP verification
 * callers.</p>
 */
public interface TotpAttemptLimiter {

    void checkAllowed(UUID userId, TotpVerificationPurpose purpose);

    void recordFailure(UUID userId, TotpVerificationPurpose purpose);

    void reset(UUID userId, TotpVerificationPurpose purpose);
}
