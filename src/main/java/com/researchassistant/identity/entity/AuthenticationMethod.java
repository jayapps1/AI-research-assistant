package com.researchassistant.identity.entity;

/**
 * Supported authentication methods for interactive login.
 *
 * <p>The enum keeps authentication policy explicit and avoids
 * spreading free-form strings through the identity and security
 * layers. Additional methods can be introduced without changing
 * the login endpoint shape.</p>
 */
public enum AuthenticationMethod {

    /**
     * Password-only authentication using Spring Security's
     * username/password authentication pipeline.
     */
    PASSWORD,

    /**
     * Authenticator-app Time-Based One-Time Password
     * authentication for accounts with verified TOTP enrollment.
     */
    TOTP,

    /**
     * Stronger authentication requiring both a valid password and
     * a valid authenticator-app TOTP code.
     */
    PASSWORD_AND_TOTP
}
