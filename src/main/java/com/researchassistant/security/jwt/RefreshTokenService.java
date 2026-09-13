package com.researchassistant.security.jwt;

import com.researchassistant.common.exception.AuthenticationFailedException;
import com.researchassistant.identity.entity.User;
import com.researchassistant.identity.entity.UserStatus;
import com.researchassistant.security.audit.SecurityAuditEventType;
import com.researchassistant.security.audit.SecurityAuditService;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.util.Base64;
import java.util.HexFormat;

/**
 * Manages opaque refresh tokens and server-side refresh sessions.
 *
 * <p>Refresh tokens are not JWTs because the server must be able
 * to revoke and rotate them immediately. Only SHA-256 hashes are
 * stored, which avoids persisting bearer credentials while still
 * allowing efficient lookup of high-entropy random tokens.</p>
 */
@Service
public class RefreshTokenService {

    private static final int REFRESH_TOKEN_BYTES = 32;
    private static final String INVALID_REFRESH_TOKEN =
            "Invalid refresh token.";

    private final RefreshSessionRepository refreshSessionRepository;
    private final SecurityAuditService securityAuditService;
    private final SecureRandom secureRandom;
    private final long refreshTokenDays;

    public RefreshTokenService(
            RefreshSessionRepository refreshSessionRepository,
            SecurityAuditService securityAuditService,
            @Value("${app.security.jwt.refresh-token-days}")
            long refreshTokenDays
    ) {
        this.refreshSessionRepository = refreshSessionRepository;
        this.securityAuditService = securityAuditService;
        this.secureRandom = new SecureRandom();
        this.refreshTokenDays = refreshTokenDays;
    }

    @Transactional
    public IssuedRefreshToken createRefreshToken(
            User user,
            HttpServletRequest request
    ) {

        String rawToken = generateRawToken();
        RefreshSession session = buildSession(user, rawToken, request);

        refreshSessionRepository.save(session);

        return new IssuedRefreshToken(rawToken, session);
    }

    /**
     * Rotates a refresh token in one transaction.
     *
     * <p>The old session is locked, revoked and linked to the
     * replacement before the transaction commits. A second request
     * racing with the same token will observe the revoked state and
     * fail instead of receiving another valid token pair.</p>
     */
    @Transactional
    public RotatedRefreshToken rotateRefreshToken(
            String rawRefreshToken,
            HttpServletRequest request
    ) {

        OffsetDateTime now = OffsetDateTime.now();
        String tokenHash = hashToken(rawRefreshToken);

        RefreshSession existingSession = refreshSessionRepository
                .findByTokenHashForUpdate(tokenHash)
                .orElseThrow(() -> new AuthenticationFailedException(
                        INVALID_REFRESH_TOKEN
                ));

        if (existingSession.getRevokedAt() != null) {
            throw new AuthenticationFailedException(INVALID_REFRESH_TOKEN);
        }

        if (!existingSession.getExpiresAt().isAfter(now)) {
            throw new AuthenticationFailedException(INVALID_REFRESH_TOKEN);
        }

        User user = existingSession.getUser();

        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new AuthenticationFailedException(INVALID_REFRESH_TOKEN);
        }

        existingSession.setRevokedAt(now);
        existingSession.setLastUsedAt(now);

        String replacementRawToken = generateRawToken();
        RefreshSession replacementSession =
                buildSession(user, replacementRawToken, request);

        refreshSessionRepository.save(replacementSession);
        existingSession.setReplacedBySessionId(replacementSession.getId());

        return new RotatedRefreshToken(
                user,
                replacementRawToken,
                replacementSession
        );
    }

    /**
     * Revokes a single refresh session by raw token.
     *
     * <p>The operation is intentionally idempotent from the API
     * perspective; callers should not learn whether a specific
     * refresh token existed.</p>
     */
    @Transactional
    public void revokeRefreshToken(String rawRefreshToken) {

        String tokenHash = hashToken(rawRefreshToken);

        refreshSessionRepository.findByTokenHashForUpdate(tokenHash)
                .ifPresent(session -> {
                    if (session.getRevokedAt() == null) {
                        session.setRevokedAt(OffsetDateTime.now());
                    }
                    securityAuditService.record(
                            session.getUser().getId(),
                            SecurityAuditEventType.LOGOUT
                    );
                });
    }

    /**
     * Revokes all active refresh sessions for the user.
     */
    @Transactional
    public void revokeAllForUser(User user) {

        OffsetDateTime now = OffsetDateTime.now();

        for (RefreshSession session
                : refreshSessionRepository.findByUserIdAndRevokedAtIsNull(
                        user.getId()
                )) {
            session.setRevokedAt(now);
        }
    }

    public String hashToken(String rawToken) {

        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(
                    rawToken.getBytes(StandardCharsets.UTF_8)
            );

            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException(
                    "SHA-256 digest is not available.",
                    exception
            );
        }
    }

    private RefreshSession buildSession(
            User user,
            String rawToken,
            HttpServletRequest request
    ) {

        OffsetDateTime now = OffsetDateTime.now();

        RefreshSession session = new RefreshSession();
        session.setUser(user);
        session.setTokenHash(hashToken(rawToken));
        session.setCreatedAt(now);
        session.setExpiresAt(now.plusDays(refreshTokenDays));
        session.setUserAgent(normalizeHeader(request.getHeader("User-Agent")));
        session.setIpAddress(normalizeText(request.getRemoteAddr(), 64));

        return session;
    }

    private String generateRawToken() {

        byte[] randomBytes = new byte[REFRESH_TOKEN_BYTES];
        secureRandom.nextBytes(randomBytes);

        return Base64
                .getUrlEncoder()
                .withoutPadding()
                .encodeToString(randomBytes);
    }

    private String normalizeHeader(String value) {
        return normalizeText(value, 512);
    }

    private String normalizeText(String value, int maxLength) {

        if (value == null || value.isBlank()) {
            return null;
        }

        String normalized = value.trim();

        if (normalized.length() > maxLength) {
            return normalized.substring(0, maxLength);
        }

        return normalized;
    }

    public record IssuedRefreshToken(
            String tokenValue,
            RefreshSession session
    ) {
    }

    public record RotatedRefreshToken(
            User user,
            String refreshTokenValue,
            RefreshSession session
    ) {
    }
}
