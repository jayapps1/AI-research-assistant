package com.researchassistant.identity.dto;

/**
 * Short-lived authorization returned after password-recovery
 * factor verification.
 *
 * @param resetToken opaque token used only to complete password reset
 * @param expiresIn token lifetime in seconds
 */
public record PasswordResetAuthorizationResponse(
        String resetToken,
        long expiresIn
) {
}
