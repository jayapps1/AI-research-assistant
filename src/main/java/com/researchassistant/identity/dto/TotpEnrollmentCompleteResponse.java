package com.researchassistant.identity.dto;

import java.util.List;

/**
 * Returned after TOTP is enabled or rotated.
 *
 * @param recoveryCodes plaintext recovery codes shown exactly once
 */
public record TotpEnrollmentCompleteResponse(
        String message,
        List<String> recoveryCodes
) {
}
