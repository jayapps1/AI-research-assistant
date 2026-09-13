package com.researchassistant.identity.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record VerifyPasswordRecoveryTotpRequest(

        @NotBlank(message = "Email is required")
        @Email(message = "Email must be valid")
        @Size(max = 255, message = "Email must not exceed 255 characters")
        String email,

        @NotBlank(message = "TOTP code is required")
        @Pattern(
                regexp = "^\\d{6}$",
                message = "TOTP code must contain exactly 6 digits"
        )
        String totpCode
) {
}
