package com.researchassistant.identity.entity;

/**
 * Represents the current lifecycle state of a user account.
 *
 * <p>The values are stored as strings in PostgreSQL rather than
 * ordinal numbers. This prevents database values from changing
 * if the enum declaration is reordered in the future.</p>
 */
public enum UserStatus {

    /**
     * Normal account that is permitted to use the platform.
     */
    ACTIVE,

    /**
     * Account that has been deactivated without being deleted.
     */
    INACTIVE,

    /**
     * Account temporarily prevented from accessing the platform.
     */
    SUSPENDED,

    /**
     * Account created but not yet fully activated.
     */
    PENDING
}