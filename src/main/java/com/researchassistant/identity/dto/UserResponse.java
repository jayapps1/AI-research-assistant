package com.researchassistant.identity.dto;

import com.researchassistant.identity.entity.UserStatus;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Safe API representation of a user account.
 *
 * <p>Sensitive internal fields such as password hashes are
 * intentionally excluded from this response.</p>
 *
 * @param id public UUID of the user
 * @param email primary email address
 * @param firstName given name
 * @param lastName family name
 * @param status current account status
 * @param emailVerified email verification state
 * @param locale preferred locale
 * @param createdAt account creation timestamp
 * @param updatedAt most recent modification timestamp
 */
public record UserResponse(
        UUID id,
        String email,
        String firstName,
        String lastName,
        UserStatus status,
        boolean emailVerified,
        String locale,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}