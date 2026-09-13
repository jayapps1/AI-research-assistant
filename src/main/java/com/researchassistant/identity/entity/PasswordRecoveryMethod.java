package com.researchassistant.identity.entity;

/**
 * Factor used to authorize a password reset.
 */
public enum PasswordRecoveryMethod {

    TOTP,

    EMAIL,

    RECOVERY_CODE,

    SMS
}
