package com.researchassistant.identity.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request body for refresh-token rotation.
 *
 * @param refreshToken opaque refresh token previously issued to the client
 */
public record RefreshTokenRequest(

        @NotBlank(message = "Refresh token is required")
        @Size(max = 512, message = "Refresh token is too long")
        String refreshToken
) {
}
