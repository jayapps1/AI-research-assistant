package com.researchassistant.identity.service;

import com.researchassistant.common.exception.DuplicateResourceException;
import com.researchassistant.common.exception.ResourceNotFoundException;
import com.researchassistant.identity.dto.CreateUserRequest;
import com.researchassistant.identity.dto.UserResponse;
import com.researchassistant.identity.entity.User;
import com.researchassistant.identity.entity.UserStatus;
import com.researchassistant.identity.repository.UserRepository;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.UUID;

/**
 * Application service responsible for user-account operations.
 *
 * <p>This service forms the transaction boundary between the
 * API layer and persistence layer. Controllers should not
 * manipulate {@link User} entities directly.</p>
 */
@Service
@Transactional
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * Creates the service with its required dependencies.
     *
     * @param userRepository persistence repository
     * @param passwordEncoder secure password encoder
     */
    public UserService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }


    /**
     * Creates a new user account.
     *
     * <p>Email addresses are normalized before persistence so
     * logically equivalent addresses do not create multiple
     * identities.</p>
     *
     * <p>The raw password exists only long enough to be encoded.
     * Only the resulting hash is written to PostgreSQL.</p>
     *
     * @param request validated account creation request
     * @return safe representation of the newly created user
     */
    public UserResponse createUser(CreateUserRequest request) {

        String normalizedEmail =
                request.email()
                        .trim()
                        .toLowerCase(Locale.ROOT);

        if (userRepository.existsByEmailIgnoreCase(normalizedEmail)) {
            throw new DuplicateResourceException(
                    "An account already exists for this email address."
            );
        }

        User user = new User();

        user.setEmail(normalizedEmail);

        user.setPasswordHash(
                passwordEncoder.encode(request.password())
        );

        user.setFirstName(normalizeOptionalText(request.firstName()));
        user.setLastName(normalizeOptionalText(request.lastName()));

        user.setLocale(
                request.locale() == null || request.locale().isBlank()
                        ? "en"
                        : request.locale().trim()
        );

        user.setStatus(UserStatus.ACTIVE);
        user.setEmailVerified(false);

        User savedUser = userRepository.save(user);

        return toResponse(savedUser);
    }


    /**
     * Retrieves an existing user by UUID.
     *
     * @param userId unique user identifier
     * @return safe user representation
     * @throws ResourceNotFoundException when no matching user exists
     */
    @Transactional(readOnly = true)
    public UserResponse getUser(UUID userId) {

        User user = userRepository.findById(userId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "User not found: " + userId
                        )
                );

        return toResponse(user);
    }


    /**
     * Converts the persistence entity into a safe response DTO.
     *
     * <p>Password information is intentionally never copied into
     * the response object.</p>
     */
    private UserResponse toResponse(User user) {

        return new UserResponse(
                user.getId(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                user.getStatus(),
                user.isEmailVerified(),
                user.getLocale(),
                user.getCreatedAt(),
                user.getUpdatedAt()
        );
    }


    /**
     * Trims optional human-readable values while preserving null.
     */
    private String normalizeOptionalText(String value) {

        if (value == null) {
            return null;
        }

        String normalized = value.trim();

        return normalized.isEmpty() ? null : normalized;
    }
}