package com.researchassistant.identity.service;

import com.researchassistant.common.exception.AuthenticationFailedException;
import com.researchassistant.identity.dto.CompletePasswordResetRequest;
import com.researchassistant.identity.dto.ForgotPasswordRequest;
import com.researchassistant.identity.dto.GenericMessageResponse;
import com.researchassistant.identity.dto.PasswordResetAuthorizationResponse;
import com.researchassistant.identity.dto.VerifyPasswordRecoveryCodeRequest;
import com.researchassistant.identity.dto.VerifyPasswordRecoveryTotpRequest;
import com.researchassistant.identity.entity.PasswordRecoveryMethod;
import com.researchassistant.identity.entity.PasswordResetAuthorization;
import com.researchassistant.identity.entity.User;
import com.researchassistant.identity.entity.UserStatus;
import com.researchassistant.identity.repository.PasswordResetAuthorizationRepository;
import com.researchassistant.identity.repository.UserRepository;
import com.researchassistant.security.jwt.RefreshSession;
import com.researchassistant.security.jwt.RefreshSessionRepository;
import com.researchassistant.security.totp.RecoveryCodeService;
import com.researchassistant.security.totp.TotpService;
import com.researchassistant.security.totp.TotpVerificationPurpose;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Locale;

/**
 * Coordinates password recovery without exposing account state.
 *
 * <p>TOTP recovery deliberately reuses the user's single verified
 * authenticator credential. It never creates a recovery-specific
 * TOTP secret or asks the user to scan a second QR code.</p>
 */
@Service
public class PasswordRecoveryService {

    private static final String GENERIC_FORGOT_MESSAGE =
            "If the account can be recovered, recovery instructions are available.";
    private static final String INVALID_RESET_AUTHORIZATION =
            "Invalid password reset authorization.";
    private static final int RESET_TOKEN_BYTES = 32;
    private static final long RESET_TOKEN_SECONDS = 10 * 60;

    private final UserRepository userRepository;
    private final PasswordResetAuthorizationRepository authorizationRepository;
    private final RefreshSessionRepository refreshSessionRepository;
    private final TotpService totpService;
    private final RecoveryCodeService recoveryCodeService;
    private final PasswordEncoder passwordEncoder;
    private final SecureRandom secureRandom;

    public PasswordRecoveryService(
            UserRepository userRepository,
            PasswordResetAuthorizationRepository authorizationRepository,
            RefreshSessionRepository refreshSessionRepository,
            TotpService totpService,
            RecoveryCodeService recoveryCodeService,
            PasswordEncoder passwordEncoder
    ) {
        this.userRepository = userRepository;
        this.authorizationRepository = authorizationRepository;
        this.refreshSessionRepository = refreshSessionRepository;
        this.totpService = totpService;
        this.recoveryCodeService = recoveryCodeService;
        this.passwordEncoder = passwordEncoder;
        this.secureRandom = new SecureRandom();
    }

    public GenericMessageResponse forgotPassword(
            ForgotPasswordRequest request
    ) {

        // Future email recovery will enqueue mail here when the
        // account exists. The response stays generic to prevent
        // account enumeration.
        return new GenericMessageResponse(GENERIC_FORGOT_MESSAGE);
    }

    @Transactional
    public PasswordResetAuthorizationResponse verifyTotp(
            VerifyPasswordRecoveryTotpRequest request
    ) {

        User user = userRepository
                .findByEmailIgnoreCase(normalizeEmail(request.email()))
                .orElseThrow(() -> new AuthenticationFailedException(
                        "Invalid credentials."
                ));

        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new AuthenticationFailedException("Invalid credentials.");
        }

        totpService.verifyCode(
                user,
                request.totpCode(),
                TotpVerificationPurpose.PASSWORD_RECOVERY
        );

        return issueResetAuthorization(user, PasswordRecoveryMethod.TOTP);
    }

    @Transactional
    public PasswordResetAuthorizationResponse verifyRecoveryCode(
            VerifyPasswordRecoveryCodeRequest request
    ) {

        User user = userRepository
                .findByEmailIgnoreCase(normalizeEmail(request.email()))
                .orElseThrow(() -> new AuthenticationFailedException(
                        "Invalid credentials."
                ));

        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new AuthenticationFailedException("Invalid credentials.");
        }

        recoveryCodeService.consumeRecoveryCode(
                user,
                request.recoveryCode()
        );

        return issueResetAuthorization(
                user,
                PasswordRecoveryMethod.RECOVERY_CODE
        );
    }

    @Transactional
    public GenericMessageResponse completeReset(
            CompletePasswordResetRequest request
    ) {

        OffsetDateTime now = OffsetDateTime.now();

        PasswordResetAuthorization authorization =
                authorizationRepository
                        .findByTokenHashForUpdate(
                                hashToken(request.resetToken())
                        )
                        .orElseThrow(() ->
                                new AuthenticationFailedException(
                                        INVALID_RESET_AUTHORIZATION
                                )
                        );

        if (authorization.getUsedAt() != null
                || !authorization.getExpiresAt().isAfter(now)
                || authorization.getUser().getStatus() != UserStatus.ACTIVE) {
            throw new AuthenticationFailedException(
                    INVALID_RESET_AUTHORIZATION
            );
        }

        User user = authorization.getUser();
        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        authorization.setUsedAt(now);

        for (RefreshSession session
                : refreshSessionRepository.findByUserIdAndRevokedAtIsNull(
                        user.getId()
                )) {
            session.setRevokedAt(now);
        }

        return new GenericMessageResponse("Password has been reset.");
    }

    public String hashToken(String rawToken) {

        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(
                    digest.digest(rawToken.getBytes(StandardCharsets.UTF_8))
            );
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException(
                    "SHA-256 digest is not available.",
                    exception
            );
        }
    }

    private PasswordResetAuthorizationResponse issueResetAuthorization(
            User user,
            PasswordRecoveryMethod method
    ) {

        String rawToken = generateRawToken();

        PasswordResetAuthorization authorization =
                new PasswordResetAuthorization();
        authorization.setUser(user);
        authorization.setTokenHash(hashToken(rawToken));
        authorization.setVerificationMethod(method);
        authorization.setCreatedAt(OffsetDateTime.now());
        authorization.setExpiresAt(
                authorization.getCreatedAt().plusSeconds(RESET_TOKEN_SECONDS)
        );

        authorizationRepository.save(authorization);

        return new PasswordResetAuthorizationResponse(
                rawToken,
                RESET_TOKEN_SECONDS
        );
    }

    private String generateRawToken() {

        byte[] randomBytes = new byte[RESET_TOKEN_BYTES];
        secureRandom.nextBytes(randomBytes);

        return Base64
                .getUrlEncoder()
                .withoutPadding()
                .encodeToString(randomBytes);
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
