package com.researchassistant.security.credentials;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Local AES-GCM credential-secret encryption.
 *
 * <p>The encryption key is externalized through configuration and
 * must never be committed. AES-GCM provides confidentiality and
 * authentication for secrets stored in PostgreSQL. This class is a
 * boundary that can later be replaced by a KMS-backed implementation.</p>
 */
@Component
public class AesGcmCredentialSecretEncryptor
        implements CredentialSecretEncryptor {

    private static final int GCM_IV_BYTES = 12;
    private static final int GCM_TAG_BITS = 128;

    private final SecretKey secretKey;
    private final SecureRandom secureRandom;

    public AesGcmCredentialSecretEncryptor(
            @Value("${app.security.credentials.encryption-key}")
            String base64EncryptionKey
    ) {
        this.secretKey = new SecretKeySpec(
                decodeKey(base64EncryptionKey),
                "AES"
        );
        this.secureRandom = new SecureRandom();
    }

    @Override
    public String encrypt(String secret) {

        byte[] iv = new byte[GCM_IV_BYTES];
        secureRandom.nextBytes(iv);

        try {
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(
                    Cipher.ENCRYPT_MODE,
                    secretKey,
                    new GCMParameterSpec(GCM_TAG_BITS, iv)
            );

            byte[] ciphertext = cipher.doFinal(
                    secret.getBytes(StandardCharsets.UTF_8)
            );

            return Base64.getUrlEncoder().withoutPadding()
                    .encodeToString(iv)
                    + "."
                    + Base64.getUrlEncoder().withoutPadding()
                    .encodeToString(ciphertext);
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException(
                    "Credential secret encryption failed.",
                    exception
            );
        }
    }

    @Override
    public String decrypt(String encryptedSecret) {

        String[] parts = encryptedSecret.split("\\.", 2);

        if (parts.length != 2) {
            throw new CredentialDecryptionException(
                    "Credential secret ciphertext is malformed."
            );
        }

        try {
            byte[] iv = Base64.getUrlDecoder().decode(parts[0]);
            byte[] ciphertext = Base64.getUrlDecoder().decode(parts[1]);

            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(
                    Cipher.DECRYPT_MODE,
                    secretKey,
                    new GCMParameterSpec(GCM_TAG_BITS, iv)
            );

            byte[] plaintext = cipher.doFinal(ciphertext);

            return new String(plaintext, StandardCharsets.UTF_8);
        } catch (IllegalArgumentException | GeneralSecurityException exception) {
            throw new CredentialDecryptionException(
                    "Credential secret decryption failed.",
                    exception
            );
        }
    }

    private byte[] decodeKey(String base64EncryptionKey) {

        byte[] keyBytes;

        try {
            keyBytes = Base64.getDecoder().decode(base64EncryptionKey);
        } catch (IllegalArgumentException exception) {
            throw new IllegalStateException(
                    "Credential encryption key must be valid Base64.",
                    exception
            );
        }

        if (keyBytes.length != 32) {
            throw new IllegalStateException(
                    "Credential encryption key must decode to 256 bits."
            );
        }

        return keyBytes;
    }
}
