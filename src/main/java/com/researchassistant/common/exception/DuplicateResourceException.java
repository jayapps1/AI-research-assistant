package com.researchassistant.common.exception;

/**
 * Thrown when an operation would violate a uniqueness rule.
 *
 * <p>An example is attempting to register an email address
 * that already belongs to another user.</p>
 */
public class DuplicateResourceException extends RuntimeException {

    public DuplicateResourceException(String message) {
        super(message);
    }
}