package com.researchassistant.security.totp;

/**
 * Security workflow requesting TOTP verification.
 *
 * <p>The same verified TOTP credential is reused across purposes.
 * Purpose is captured for clear service boundaries and future audit
 * or rate-limit policy, not to select a different TOTP secret.</p>
 */
public enum TotpVerificationPurpose {

    LOGIN,

    PASSWORD_RECOVERY,

    SENSITIVE_ACTION
}
