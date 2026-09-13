package com.researchassistant.identity.dto;

import jakarta.validation.constraints.Size;

/**
 * Step-up verification request for sensitive TOTP operations.
 *
 * <p>Services decide which factor combinations are acceptable for
 * a specific operation.</p>
 */
public record TotpStepUpRequest(

        @Size(max = 128, message = "Password must not exceed 128 characters")
        String password,

        @Size(max = 16, message = "TOTP code is too long")
        String totpCode,

        @Size(max = 64, message = "Recovery code is too long")
        String recoveryCode
) {
}
