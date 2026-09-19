package com.researchassistant.security.credentials;

/**
 * Thrown when credential secret decryption fails, e.g., due to an
 * encryption key mismatch, corrupted ciphertext, or altered tag/payload.
 */
public class CredentialDecryptionException extends RuntimeException {

    public CredentialDecryptionException(String message, Throwable cause) {
        super(message, cause);
    }

    public CredentialDecryptionException(String message) {
        super(message);
    }
}
