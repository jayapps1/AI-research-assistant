package com.researchassistant.identity.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request model used when creating a new user account.
 *
 * <p>API request DTOs are deliberately separated from JPA
 * entities so clients cannot directly manipulate internal
 * persistence fields such as password hashes, status flags,
 * UUID values or audit timestamps.</p>
 *
 * @param email user's primary email address
 * @param password raw password supplied during registration
 * @param firstName user's given name
 * @param lastName user's family name
 * @param locale preferred locale such as en or en-GH
 */
public record CreateUserRequest(

        @NotBlank(message = "Email is required")
        @Email(message = "Email must be valid")
        @Size(max = 255, message = "Email must not exceed 255 characters")
        String email,

        @NotBlank(message = "Password is required")
        @Size(
                min = 8,
                max = 128,
                message = "Password must contain between 8 and 128 characters"
        )
        String password,

        @Size(max = 100, message = "First name must not exceed 100 characters")
        String firstName,

        @Size(max = 100, message = "Last name must not exceed 100 characters")
        String lastName,

        @Size(max = 20, message = "Locale must not exceed 20 characters")
        String locale
) {
}