package com.researchassistant.identity.repository;

import com.researchassistant.identity.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

/**
 * Persistence repository for {@link User} entities.
 *
 * <p>Spring Data JPA provides the standard CRUD implementation
 * automatically. Additional methods declared here represent
 * identity-specific database queries.</p>
 */
public interface UserRepository extends JpaRepository<User, UUID> {

    /**
     * Finds a user by email address without considering case.
     *
     * @param email email address to search for
     * @return matching user when one exists
     */
    Optional<User> findByEmailIgnoreCase(String email);

    /**
     * Determines whether an account already exists for the
     * supplied email address.
     *
     * @param email email address to check
     * @return true when the email is already registered
     */
    boolean existsByEmailIgnoreCase(String email);

    long countByStatus(com.researchassistant.identity.entity.UserStatus status);
}