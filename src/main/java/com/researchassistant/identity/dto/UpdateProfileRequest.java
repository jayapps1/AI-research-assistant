package com.researchassistant.identity.dto;

import jakarta.validation.constraints.Size;

/**
 * Request payload for updating the authenticated user's profile.
 *
 * <p>Only safe, user-editable identity attributes are allowed.
 * Security credentials, roles, email verification, account status,
 * and workspace memberships cannot be updated through this endpoint.</p>
 */
public record UpdateProfileRequest(
        @Size(max = 100, message = "First name must not exceed 100 characters")
        String firstName,

        @Size(max = 100, message = "Last name must not exceed 100 characters")
        String lastName,

        @Size(max = 40, message = "Phone number must not exceed 40 characters")
        String phoneNumber,

        @Size(max = 20, message = "Locale must not exceed 20 characters")
        String locale
) {
}
