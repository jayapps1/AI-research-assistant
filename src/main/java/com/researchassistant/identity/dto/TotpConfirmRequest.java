package com.researchassistant.identity.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record TotpConfirmRequest(

        @NotBlank(message = "TOTP code is required")
        @Pattern(
                regexp = "^\\d{6}$",
                message = "TOTP code must contain exactly 6 digits"
        )
        String totpCode
) {
}
