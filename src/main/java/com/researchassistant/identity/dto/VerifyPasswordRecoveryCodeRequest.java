package com.researchassistant.identity.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record VerifyPasswordRecoveryCodeRequest(

        @NotBlank(message = "Email is required")
        @Email(message = "Email must be valid")
        @Size(max = 255, message = "Email must not exceed 255 characters")
        String email,

        @NotBlank(message = "Recovery code is required")
        @Size(max = 64, message = "Recovery code is too long")
        String recoveryCode
) {
}
