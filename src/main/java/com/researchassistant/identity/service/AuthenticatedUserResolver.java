package com.researchassistant.identity.service;

import com.researchassistant.common.exception.AuthenticationFailedException;
import com.researchassistant.identity.entity.User;
import com.researchassistant.identity.entity.UserStatus;
import com.researchassistant.identity.repository.UserRepository;

import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Resolves the currently authenticated API user from the JWT
 * resource-server principal.
 */
@Service
public class AuthenticatedUserResolver {

    private final UserRepository userRepository;

    public AuthenticatedUserResolver(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public User requireActiveUser(Authentication authentication) {

        if (authentication == null
                || !(authentication.getPrincipal() instanceof Jwt jwt)) {
            throw new AuthenticationFailedException("Authentication required.");
        }

        UUID userId;

        try {
            userId = UUID.fromString(jwt.getSubject());
        } catch (IllegalArgumentException exception) {
            throw new AuthenticationFailedException("Authentication required.");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() ->
                        new AuthenticationFailedException(
                                "Authentication required."
                        )
                );

        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new AuthenticationFailedException("Authentication required.");
        }

        return user;
    }
}
