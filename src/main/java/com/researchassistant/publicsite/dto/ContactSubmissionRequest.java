package com.researchassistant.publicsite.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ContactSubmissionRequest(
        @NotBlank(message = "Name is required.")
        @Size(min = 2, max = 120, message = "Name must be between 2 and 120 characters.")
        String name,

        @NotBlank(message = "Email is required.")
        @Email(message = "Enter a valid email address.")
        @Size(max = 254, message = "Email must not exceed 254 characters.")
        String email,

        @Size(max = 50, message = "Phone must not exceed 50 characters.")
        String phone,

        @NotBlank(message = "Subject is required.")
        @Size(min = 3, max = 200, message = "Subject must be between 3 and 200 characters.")
        String subject,

        @NotBlank(message = "Message is required.")
        @Size(min = 10, max = 5000, message = "Message must be between 10 and 5000 characters.")
        String message,

        // Spam honeypot field. Legitimate users leave this blank.
        String honeypot
) {}
