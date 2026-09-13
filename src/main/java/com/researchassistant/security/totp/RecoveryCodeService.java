package com.researchassistant.security.totp;

import com.researchassistant.common.exception.AuthenticationFailedException;
import com.researchassistant.identity.entity.User;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Generates and verifies one-time recovery codes for TOTP users.
 *
 * <p>Recovery codes are not TOTP secrets. They are high-entropy
 * one-time credentials that are displayed once and stored only as
 * slow PasswordEncoder hashes.</p>
 */
@Service
public class RecoveryCodeService {

    private static final int RECOVERY_CODE_COUNT = 10;
    private static final int RECOVERY_CODE_BYTES = 9;
    private static final char[] ALPHABET =
            "ABCDEFGHJKLMNPQRSTUVWXYZ23456789".toCharArray();
    private static final String INVALID_CREDENTIALS =
            "Invalid credentials.";

    private final TotpRecoveryCodeRepository recoveryCodeRepository;
    private final PasswordEncoder passwordEncoder;
    private final SecureRandom secureRandom;

    public RecoveryCodeService(
            TotpRecoveryCodeRepository recoveryCodeRepository,
            PasswordEncoder passwordEncoder
    ) {
        this.recoveryCodeRepository = recoveryCodeRepository;
        this.passwordEncoder = passwordEncoder;
        this.secureRandom = new SecureRandom();
    }

    @Transactional
    public List<String> replaceRecoveryCodes(User user) {

        revokeAvailableCodes(user);

        List<String> plaintextCodes = new ArrayList<>();

        for (int i = 0; i < RECOVERY_CODE_COUNT; i++) {
            String plaintextCode = generateRecoveryCode();
            plaintextCodes.add(plaintextCode);

            TotpRecoveryCode code = new TotpRecoveryCode();
            code.setUser(user);
            code.setCodeHash(passwordEncoder.encode(normalize(plaintextCode)));

            recoveryCodeRepository.save(code);
        }

        return plaintextCodes;
    }

    @Transactional
    public void revokeAvailableCodes(User user) {

        OffsetDateTime now = OffsetDateTime.now();

        for (TotpRecoveryCode code
                : recoveryCodeRepository.findAvailableForUserForUpdate(
                        user.getId()
                )) {
            code.setRevokedAt(now);
        }
    }

    /**
     * Verifies and consumes one recovery code atomically.
     */
    @Transactional
    public void consumeRecoveryCode(User user, String submittedCode) {

        String normalizedCode = normalize(submittedCode);
        OffsetDateTime now = OffsetDateTime.now();

        for (TotpRecoveryCode code
                : recoveryCodeRepository.findAvailableForUserForUpdate(
                        user.getId()
                )) {

            if (passwordEncoder.matches(
                    normalizedCode,
                    code.getCodeHash()
            )) {
                code.setUsedAt(now);
                return;
            }
        }

        throw new AuthenticationFailedException(INVALID_CREDENTIALS);
    }

    private String generateRecoveryCode() {

        byte[] bytes = new byte[RECOVERY_CODE_BYTES];
        secureRandom.nextBytes(bytes);

        StringBuilder code = new StringBuilder();

        for (byte value : bytes) {
            code.append(ALPHABET[Byte.toUnsignedInt(value) % ALPHABET.length]);
        }

        return code.substring(0, 4)
                + "-"
                + code.substring(4, 8)
                + "-"
                + code.substring(8, 9)
                + randomGroup(3);
    }

    private String randomGroup(int length) {

        StringBuilder group = new StringBuilder();

        for (int i = 0; i < length; i++) {
            group.append(ALPHABET[secureRandom.nextInt(ALPHABET.length)]);
        }

        return group.toString();
    }

    private String normalize(String code) {

        if (code == null || code.isBlank()) {
            throw new AuthenticationFailedException(INVALID_CREDENTIALS);
        }

        return code.trim().toUpperCase(Locale.ROOT);
    }
}
