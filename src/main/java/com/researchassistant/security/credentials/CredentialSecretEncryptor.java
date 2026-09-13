package com.researchassistant.security.credentials;

/**
 * Encrypts and decrypts authentication credential secrets.
 *
 * <p>The abstraction keeps callers independent from the local
 * encryption implementation so production can later delegate to a
 * managed key service without changing TOTP credential persistence
 * or verification code.</p>
 */
public interface CredentialSecretEncryptor {

    String encrypt(String secret);

    String decrypt(String encryptedSecret);
}
