package com.researchassistant.security.totp;

import com.researchassistant.common.exception.AuthenticationFailedException;
import com.researchassistant.identity.entity.User;
import com.researchassistant.identity.entity.UserStatus;
import com.researchassistant.security.credentials.CredentialSecretEncryptor;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URLEncoder;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.Locale;

/**
 * RFC 6238 Time-Based One-Time Password support.
 *
 * <p>The first implementation uses interoperable authenticator-app
 * defaults: 6 digits, 30-second periods and HMAC-SHA1. The database
 * stores encrypted secrets and does not encode the algorithm into
 * the schema, so stronger algorithms can be introduced later without
 * redesigning credential storage.</p>
 */
@Service
public class TotpService {

    private static final int SECRET_BYTES = 20;
    private static final int CODE_DIGITS = 6;
    private static final long PERIOD_SECONDS = 30;
    private static final int ALLOWED_WINDOW_STEPS = 1;
    private static final String HMAC_ALGORITHM = "HmacSHA1";
    private static final String INVALID_CREDENTIALS =
            "Invalid credentials.";
    private static final String UNVERIFIED_AUTHENTICATOR =
            "Your authenticator configuration could not be verified. Use a recovery code or reset your authenticator.";
    private static final org.slf4j.Logger log =
            org.slf4j.LoggerFactory.getLogger(TotpService.class);

    private final TotpCredentialRepository totpCredentialRepository;
    private final CredentialSecretEncryptor credentialSecretEncryptor;
    private final TotpAttemptLimiter totpAttemptLimiter;
    private final SecureRandom secureRandom;
    private final String issuer;

    public TotpService(
            TotpCredentialRepository totpCredentialRepository,
            CredentialSecretEncryptor credentialSecretEncryptor,
            TotpAttemptLimiter totpAttemptLimiter,
            @Value("${app.security.totp.issuer}") String issuer
    ) {
        this.totpCredentialRepository = totpCredentialRepository;
        this.credentialSecretEncryptor = credentialSecretEncryptor;
        this.totpAttemptLimiter = totpAttemptLimiter;
        this.secureRandom = new SecureRandom();
        this.issuer = issuer;
    }

    /**
     * Creates or replaces a pending TOTP enrollment secret.
     *
     * <p>The returned manual setup key is sensitive and should only
     * be exposed by a future authenticated enrollment endpoint. TOTP
     * is not enabled until a generated code is successfully verified.</p>
     */
    @Transactional
    public TotpProvisioningDetails beginEnrollment(User user) {

        String secret = generateSecret();

        TotpCredential credential = totpCredentialRepository
                .findByUserId(user.getId())
                .orElseGet(TotpCredential::new);

        credential.setUser(user);
        credential.setEncryptedSecret(
                credentialSecretEncryptor.encrypt(secret)
        );
        credential.setEnabled(false);
        credential.setVerifiedAt(null);
        credential.setLastUsedTimestep(null);
        credential.setPendingEncryptedSecret(null);
        credential.setPendingCreatedAt(null);
        credential.setDisabledAt(null);

        totpCredentialRepository.save(credential);

        return new TotpProvisioningDetails(
                secret,
                provisioningUri(user.getEmail(), secret)
        );
    }

    /**
     * Starts authenticator rotation without disabling the currently
     * active TOTP credential. The existing authenticator remains
     * usable until the pending secret is confirmed.
     */
    @Transactional
    public TotpProvisioningDetails beginRotation(User user) {

        TotpCredential credential = totpCredentialRepository
                .findByUserIdForUpdate(user.getId())
                .filter(TotpCredential::isEnabled)
                .orElseThrow(() -> new AuthenticationFailedException(
                        INVALID_CREDENTIALS
                ));

        String secret = generateSecret();

        credential.setPendingEncryptedSecret(
                credentialSecretEncryptor.encrypt(secret)
        );
        credential.setPendingCreatedAt(OffsetDateTime.now());

        return new TotpProvisioningDetails(
                secret,
                provisioningUri(user.getEmail(), secret)
        );
    }

    /**
     * Confirms enrollment by verifying the first authenticator-app
     * code. This is intentionally not exposed through a controller
     * yet so an authenticated enrollment flow can be designed safely.
     */
    @Transactional
    public boolean confirmEnrollment(User user, String totpCode) {

        TotpCredential credential = totpCredentialRepository
                .findByUserIdForUpdate(user.getId())
                .orElseThrow(() -> new AuthenticationFailedException(
                        INVALID_CREDENTIALS
                ));

        boolean rotation = credential.isEnabled()
                && credential.getPendingEncryptedSecret() != null;

        String encryptedSecret = rotation
                ? credential.getPendingEncryptedSecret()
                : credential.getEncryptedSecret();

        long timestep = verifyCodeAndReturnTimestep(
                encryptedSecret,
                credential.getLastUsedTimestep(),
                totpCode,
                true
        );

        if (rotation) {
            credential.setEncryptedSecret(credential.getPendingEncryptedSecret());
            credential.setPendingEncryptedSecret(null);
            credential.setPendingCreatedAt(null);
        }

        credential.setEnabled(true);
        credential.setVerifiedAt(OffsetDateTime.now());
        credential.setLastUsedTimestep(timestep);
        credential.setDisabledAt(null);

        return rotation;
    }

    /**
     * Verifies a TOTP login code for an already enrolled account.
     *
     * <p>The credential row is pessimistically locked. This makes
     * replay protection transactional: two simultaneous requests
     * using the same valid timestep cannot both advance
     * last_used_timestep and succeed.</p>
     */
    @Transactional
    public void verifyLoginCode(User user, String totpCode) {
        verifyCode(user, totpCode, TotpVerificationPurpose.LOGIN);
    }

    /**
     * Verifies the user's single enrolled TOTP credential for a
     * specific security workflow.
     *
     * <p>All purposes share the same locked credential row and the
     * same last-used timestep. This prevents a TOTP code accepted
     * for login from being reused concurrently for password recovery
     * or a later sensitive action.</p>
     */
    @Transactional
    public void verifyCode(
            User user,
            String totpCode,
            TotpVerificationPurpose purpose
    ) {

        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new AuthenticationFailedException(INVALID_CREDENTIALS);
        }

        totpAttemptLimiter.checkAllowed(user.getId(), purpose);

        try {
            TotpCredential credential = totpCredentialRepository
                    .findByUserIdForUpdate(user.getId())
                    .filter(TotpCredential::isEnabled)
                    .orElseThrow(() -> new AuthenticationFailedException(
                            INVALID_CREDENTIALS
                    ));

            long timestep = verifyCodeAndReturnTimestep(
                    credential.getEncryptedSecret(),
                    credential.getLastUsedTimestep(),
                    totpCode,
                    false
            );

            credential.setLastUsedTimestep(timestep);
            totpAttemptLimiter.reset(user.getId(), purpose);
        } catch (AuthenticationFailedException exception) {
            totpAttemptLimiter.recordFailure(user.getId(), purpose);
            throw exception;
        }
    }

    @Transactional
    public void disable(User user) {

        TotpCredential credential = totpCredentialRepository
                .findByUserIdForUpdate(user.getId())
                .filter(TotpCredential::isEnabled)
                .orElseThrow(() -> new AuthenticationFailedException(
                        INVALID_CREDENTIALS
                ));

        // Replace the encrypted secret with an unusable random value
        // so a disabled credential cannot be reactivated with the
        // previous authenticator secret.
        credential.setEncryptedSecret(
                credentialSecretEncryptor.encrypt(generateSecret())
        );
        credential.setEnabled(false);
        credential.setVerifiedAt(null);
        credential.setLastUsedTimestep(null);
        credential.setPendingEncryptedSecret(null);
        credential.setPendingCreatedAt(null);
        credential.setDisabledAt(OffsetDateTime.now());
    }

    @Transactional(readOnly = true)
    public boolean hasEnabledCredential(User user) {
        return totpCredentialRepository
                .findByUserId(user.getId())
                .map(TotpCredential::isEnabled)
                .orElse(false);
    }

    public String issuer() {
        return issuer;
    }

    private long verifyCodeAndReturnTimestep(
            String encryptedSecret,
            Long lastUsedTimestep,
            String submittedCode,
            boolean allowFirstUse
    ) {

        if (submittedCode == null || !submittedCode.matches("\\d{6}")) {
            throw new AuthenticationFailedException(INVALID_CREDENTIALS);
        }

        String secret;
        try {
            secret = credentialSecretEncryptor.decrypt(encryptedSecret);
        } catch (Exception exception) {
            String correlationId = java.util.UUID.randomUUID().toString();
            log.warn("TOTP secret decryption failed [correlationId={}]: unable to decrypt credential with configured key", correlationId);
            throw new AuthenticationFailedException(UNVERIFIED_AUTHENTICATOR);
        }

        byte[] secretBytes = decodeBase32(secret);
        long currentTimestep = Instant.now().getEpochSecond()
                / PERIOD_SECONDS;

        for (long timestep = currentTimestep - ALLOWED_WINDOW_STEPS;
                timestep <= currentTimestep + ALLOWED_WINDOW_STEPS;
                timestep++) {

            if (!allowFirstUse
                    && lastUsedTimestep != null
                    && timestep <= lastUsedTimestep) {
                continue;
            }

            String expectedCode = generateCode(secretBytes, timestep);

            if (constantTimeEquals(expectedCode, submittedCode)) {
                return timestep;
            }
        }

        throw new AuthenticationFailedException(INVALID_CREDENTIALS);
    }

    private String generateSecret() {

        byte[] secretBytes = new byte[SECRET_BYTES];
        secureRandom.nextBytes(secretBytes);

        return encodeBase32(secretBytes);
    }

    private String generateCode(byte[] secretBytes, long timestep) {

        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(secretBytes, HMAC_ALGORITHM));
            byte[] hash = mac.doFinal(
                    ByteBuffer.allocate(Long.BYTES)
                            .putLong(timestep)
                            .array()
            );

            int offset = hash[hash.length - 1] & 0x0f;
            int binary = ((hash[offset] & 0x7f) << 24)
                    | ((hash[offset + 1] & 0xff) << 16)
                    | ((hash[offset + 2] & 0xff) << 8)
                    | (hash[offset + 3] & 0xff);

            int otp = binary % 1_000_000;

            return String.format(Locale.ROOT, "%06d", otp);
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException(
                    "TOTP HMAC calculation failed.",
                    exception
            );
        }
    }

    private boolean constantTimeEquals(String expected, String submitted) {

        return MessageDigestSupport.constantTimeEquals(
                expected.getBytes(StandardCharsets.US_ASCII),
                submitted.getBytes(StandardCharsets.US_ASCII)
        );
    }

    private String provisioningUri(String email, String secret) {

        String label = issuer + ":" + email;

        return "otpauth://totp/"
                + urlEncode(label)
                + "?secret="
                + urlEncode(secret)
                + "&issuer="
                + urlEncode(issuer)
                + "&digits="
                + CODE_DIGITS
                + "&period="
                + PERIOD_SECONDS
                + "&algorithm=SHA1";
    }

    private String urlEncode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8)
                .replace("+", "%20");
    }

    private String encodeBase32(byte[] bytes) {

        final char[] alphabet =
                "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567".toCharArray();
        StringBuilder encoded = new StringBuilder();
        int buffer = 0;
        int bitsLeft = 0;

        for (byte value : bytes) {
            buffer = (buffer << 8) | (value & 0xff);
            bitsLeft += 8;

            while (bitsLeft >= 5) {
                encoded.append(
                        alphabet[(buffer >> (bitsLeft - 5)) & 0x1f]
                );
                bitsLeft -= 5;
            }
        }

        if (bitsLeft > 0) {
            encoded.append(alphabet[(buffer << (5 - bitsLeft)) & 0x1f]);
        }

        return encoded.toString();
    }

    private byte[] decodeBase32(String value) {

        String normalized = value
                .replace("=", "")
                .replace(" ", "")
                .toUpperCase(Locale.ROOT);

        int buffer = 0;
        int bitsLeft = 0;
        byte[] output = new byte[normalized.length() * 5 / 8];
        int outputIndex = 0;

        for (char character : normalized.toCharArray()) {
            int decoded = decodeBase32Character(character);
            buffer = (buffer << 5) | decoded;
            bitsLeft += 5;

            if (bitsLeft >= 8) {
                output[outputIndex++] =
                        (byte) ((buffer >> (bitsLeft - 8)) & 0xff);
                bitsLeft -= 8;
            }
        }

        return Arrays.copyOf(output, outputIndex);
    }

    private int decodeBase32Character(char character) {

        if (character >= 'A' && character <= 'Z') {
            return character - 'A';
        }

        if (character >= '2' && character <= '7') {
            return character - '2' + 26;
        }

        throw new AuthenticationFailedException(INVALID_CREDENTIALS);
    }

    /**
     * Small holder to isolate constant-time comparison details.
     */
    private static final class MessageDigestSupport {

        private MessageDigestSupport() {
        }

        private static boolean constantTimeEquals(
                byte[] expected,
                byte[] submitted
        ) {

            return java.security.MessageDigest.isEqual(
                    expected,
                    submitted
            );
        }
    }
}
