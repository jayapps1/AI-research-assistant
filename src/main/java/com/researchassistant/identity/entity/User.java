package com.researchassistant.identity.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Represents a registered user of the AI Research Assistant.
 *
 * <p>This entity is the root identity record used throughout
 * the application. Workspaces, research projects, documents,
 * billing records, audit records and other modules will
 * eventually reference a user through this identifier.</p>
 *
 * <p>The entity deliberately uses UUID primary keys so that
 * predictable sequential identifiers are not exposed through
 * public APIs.</p>
 *
 * <p>Plain-text passwords must never be persisted. Only a
 * secure password hash may be stored in {@code passwordHash}.</p>
 */
@Entity
@Table(
        name = "users",
        indexes = {
                @Index(
                        name = "idx_users_status",
                        columnList = "status"
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
public class User {

    /**
     * Globally unique identifier for the user.
     *
     * <p>The identifier is generated before the first database
     * insert and cannot be changed afterwards.</p>
     */
    @Id
    @Column(
            name = "id",
            nullable = false,
            updatable = false
    )
    private UUID id;

    /**
     * Primary email address associated with the account.
     *
     * <p>Case-insensitive uniqueness is enforced in PostgreSQL
     * using an index on {@code LOWER(email)}.</p>
     */
    @Column(
            name = "email",
            nullable = false,
            length = 255
    )
    private String email;

    /**
     * Secure one-way hash of the user's password.
     *
     * <p>This may remain null for accounts that later use
     * external authentication providers exclusively.</p>
     */
    @Column(
            name = "password_hash",
            length = 255
    )
    private String passwordHash;

    /**
     * User's given or first name.
     */
    @Column(
            name = "first_name",
            length = 100
    )
    private String firstName;

    /**
     * User's family or last name.
     */
    @Column(
            name = "last_name",
            length = 100
    )
    private String lastName;

    /**
     * Current lifecycle state of the account.
     *
     * <p>The enum name is stored in PostgreSQL instead of its
     * ordinal value.</p>
     */
    @Enumerated(EnumType.STRING)
    @Column(
            name = "status",
            nullable = false,
            length = 30
    )
    private UserStatus status = UserStatus.ACTIVE;

    /**
     * Indicates whether the account owner has confirmed
     * ownership of the registered email address.
     */
    @Column(
            name = "email_verified",
            nullable = false
    )
    private boolean emailVerified = false;

    /**
     * Preferred locale used for language and formatting.
     *
     * <p>Examples include {@code en} and {@code en-GH}.</p>
     */
    @Column(
            name = "locale",
            length = 20
    )
    private String locale = "en";

    /**
     * Timestamp when the account was first created.
     */
    @Column(
            name = "created_at",
            nullable = false,
            updatable = false
    )
    private OffsetDateTime createdAt;

    /**
     * Timestamp of the most recent modification.
     */
    @Column(
            name = "updated_at",
            nullable = false
    )
    private OffsetDateTime updatedAt;


    /**
     * Initializes system-managed values before the entity is
     * inserted into PostgreSQL.
     */
    @PrePersist
    protected void onCreate() {

        if (id == null) {
            id = UUID.randomUUID();
        }

        OffsetDateTime now = OffsetDateTime.now();

        if (createdAt == null) {
            createdAt = now;
        }

        updatedAt = now;

        if (status == null) {
            status = UserStatus.ACTIVE;
        }

        if (locale == null || locale.isBlank()) {
            locale = "en";
        }
    }


    /**
     * Refreshes the modification timestamp before Hibernate
     * performs an UPDATE operation.
     */
    @PreUpdate
    protected void onUpdate() {
        updatedAt = OffsetDateTime.now();
    }
}