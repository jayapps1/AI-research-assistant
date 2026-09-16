package com.researchassistant.identity.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record CompleteTotpLoginChallengeRequest(
        @NotBlank(message = "Challenge is required")
        String challengeId,

        @Pattern(regexp = "^\\d{6}$", message = "TOTP code must contain exactly 6 digits")
        String totpCode
) {
}
