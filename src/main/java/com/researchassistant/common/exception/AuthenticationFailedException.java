package com.researchassistant.common.exception;

/**
 * Thrown when authentication or token refresh fails.
 *
 * <p>Messages should remain generic so API responses do not
 * reveal whether a specific email address exists, whether an
 * account is disabled, or which part of a credential was wrong.</p>
 */
public class AuthenticationFailedException extends RuntimeException {

    private final String errorCode;

    public AuthenticationFailedException(String message) {
        super(message);
        this.errorCode = null;
    }

    public AuthenticationFailedException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }
}
