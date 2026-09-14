package com.researchassistant.participant.service;

import com.researchassistant.security.credentials.CredentialSecretEncryptor;
import org.springframework.stereotype.Service;

/**
 * Encryption boundary for direct research participant identifiers.
 *
 * <p>The first implementation delegates to the existing AES-GCM
 * secret encryptor. Keeping a dedicated service prevents future
 * participant identity encryption from being coupled to JWT secrets
 * or exposed through ordinary participant APIs.</p>
 */
@Service
public class ResearchDataEncryptionService {
    private final CredentialSecretEncryptor encryptor;

    public ResearchDataEncryptionService(CredentialSecretEncryptor encryptor) {
        this.encryptor = encryptor;
    }

    public String encryptNullable(String value) {
        return value == null || value.isBlank() ? null : encryptor.encrypt(value);
    }

    public String decryptNullable(String value) {
        return value == null || value.isBlank() ? null : encryptor.decrypt(value);
    }
}
