package com.researchassistant.identity.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LogoutRequest(

        @NotBlank(message = "Refresh token is required")
        @Size(max = 512, message = "Refresh token is too long")
        String refreshToken
) {
}
