package com.researchassistant.common.exception;

/**
 * Thrown when an application resource cannot be found using
 * the supplied identifier.
 */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }
}