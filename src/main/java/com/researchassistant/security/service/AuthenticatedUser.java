package com.researchassistant.security.service;

import com.researchassistant.identity.entity.UserStatus;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

/**
 * Spring Security principal for a registered application user.
 *
 * <p>The principal exposes the stable user UUID, email and account
 * status needed by authentication code without exposing the full
 * JPA entity to the security context.</p>
 */
public final class AuthenticatedUser implements UserDetails {

    private final UUID userId;
    private final String email;
    private final String passwordHash;
    private final UserStatus status;

    public AuthenticatedUser(
            UUID userId,
            String email,
            String passwordHash,
            UserStatus status
    ) {
        this.userId = userId;
        this.email = email;
        this.passwordHash = passwordHash;
        this.status = status;
    }

    public UUID getUserId() {
        return userId;
    }

    public String getEmail() {
        return email;
    }

    public UserStatus getStatus() {
        return status;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of();
    }

    @Override
    public String getPassword() {
        return passwordHash;
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isEnabled() {
        return status == UserStatus.ACTIVE;
    }
}
