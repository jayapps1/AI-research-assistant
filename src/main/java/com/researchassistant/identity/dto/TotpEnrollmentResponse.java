package com.researchassistant.identity.dto;

/**
 * Provisioning information for authenticator-app enrollment.
 *
 * <p>The secret is returned only during enrollment or
 * re-enrollment setup so the user can manually enter it if QR
 * rendering is unavailable.</p>
 */
public record TotpEnrollmentResponse(
        String issuer,
        String accountName,
        String secret,
        String provisioningUri
) {
}
