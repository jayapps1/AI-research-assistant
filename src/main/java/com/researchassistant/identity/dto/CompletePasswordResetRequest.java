package com.researchassistant.identity.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CompletePasswordResetRequest(

        @NotBlank(message = "Reset token is required")
        @Size(max = 512, message = "Reset token is too long")
        String resetToken,

        @NotBlank(message = "New password is required")
        @Size(
                min = 8,
                max = 128,
                message = "Password must contain between 8 and 128 characters"
        )
        String newPassword
) {
}
