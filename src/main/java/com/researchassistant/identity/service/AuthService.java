package com.researchassistant.identity.service;

import com.researchassistant.common.exception.AuthenticationFailedException;
import com.researchassistant.identity.dto.AuthTokenResponse;
import com.researchassistant.identity.dto.GenericMessageResponse;
import com.researchassistant.identity.dto.LoginRequest;
import com.researchassistant.identity.dto.LogoutRequest;
import com.researchassistant.identity.dto.RefreshTokenRequest;
import com.researchassistant.identity.entity.AuthenticationMethod;
import com.researchassistant.identity.entity.User;
import com.researchassistant.identity.entity.UserStatus;
import com.researchassistant.identity.repository.UserRepository;
import com.researchassistant.security.jwt.JwtTokenService;
import com.researchassistant.security.jwt.JwtTokenService.IssuedAccessToken;
import com.researchassistant.security.jwt.RefreshTokenService;
import com.researchassistant.security.jwt.RefreshTokenService.IssuedRefreshToken;
import com.researchassistant.security.jwt.RefreshTokenService.RotatedRefreshToken;
import com.researchassistant.security.audit.SecurityAuditEventType;
import com.researchassistant.security.audit.SecurityAuditService;
import com.researchassistant.security.service.AuthenticatedUser;
import com.researchassistant.security.totp.TotpService;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

/**
 * Coordinates public authentication workflows.
 */
@Service
public class AuthService {

    private static final String INVALID_LOGIN =
            "Invalid credentials.";

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final JwtTokenService jwtTokenService;
    private final RefreshTokenService refreshTokenService;
    private final TotpService totpService;
    private final SecurityAuditService securityAuditService;

    public AuthService(
            AuthenticationManager authenticationManager,
            UserRepository userRepository,
            JwtTokenService jwtTokenService,
            RefreshTokenService refreshTokenService,
            TotpService totpService,
            SecurityAuditService securityAuditService
    ) {
        this.authenticationManager = authenticationManager;
        this.userRepository = userRepository;
        this.jwtTokenService = jwtTokenService;
        this.refreshTokenService = refreshTokenService;
        this.totpService = totpService;
        this.securityAuditService = securityAuditService;
    }

    /**
     * Authenticates a user and returns a short-lived access token
     * plus a rotating opaque refresh token.
     */
    @Transactional
    public AuthTokenResponse login(
            LoginRequest request,
            HttpServletRequest httpRequest
    ) {

        User user = authenticate(request);

        IssuedAccessToken accessToken =
                jwtTokenService.issueAccessToken(user);

        IssuedRefreshToken refreshToken =
                refreshTokenService.createRefreshToken(user, httpRequest);

        return tokenResponse(accessToken, refreshToken.tokenValue());
    }

    private User authenticate(LoginRequest request) {

        AuthenticationMethod method =
                request.requestedAuthenticationMethod();

        return switch (method) {
            case PASSWORD -> authenticatePassword(request);
            case TOTP -> authenticateTotp(request);
            case PASSWORD_AND_TOTP -> authenticatePasswordAndTotp(request);
        };
    }

    private User authenticatePassword(LoginRequest request) {

        Authentication authentication;

        try {
            authentication = authenticationManager.authenticate(
                    UsernamePasswordAuthenticationToken.unauthenticated(
                            normalizeEmail(request.email()),
                            request.password()
                    )
            );
        } catch (AuthenticationException exception) {
            throw new AuthenticationFailedException(INVALID_LOGIN);
        }

        AuthenticatedUser authenticatedUser =
                requireAuthenticatedUser(authentication);

        if (authenticatedUser.getStatus() != UserStatus.ACTIVE) {
            throw new AuthenticationFailedException(INVALID_LOGIN);
        }

        return userRepository.getReferenceById(
                authenticatedUser.getUserId()
        );
    }

    private User authenticateTotp(LoginRequest request) {

        User user = loadActiveUserByEmail(request.email());
        totpService.verifyLoginCode(user, request.totpCode());

        return user;
    }

    private User authenticatePasswordAndTotp(LoginRequest request) {

        User user = authenticatePassword(request);
        totpService.verifyLoginCode(user, request.totpCode());

        return user;
    }

    /**
     * Rotates an existing refresh token and issues a new token pair.
     */
    @Transactional
    public AuthTokenResponse refresh(
            RefreshTokenRequest request,
            HttpServletRequest httpRequest
    ) {

        RotatedRefreshToken rotatedRefreshToken =
                refreshTokenService.rotateRefreshToken(
                        request.refreshToken(),
                        httpRequest
                );

        IssuedAccessToken accessToken = jwtTokenService.issueAccessToken(
                rotatedRefreshToken.user()
        );

        return tokenResponse(
                accessToken,
                rotatedRefreshToken.refreshTokenValue()
        );
    }

    @Transactional
    public GenericMessageResponse logout(LogoutRequest request) {

        refreshTokenService.revokeRefreshToken(request.refreshToken());

        return new GenericMessageResponse("Logged out.");
    }

    @Transactional
    public GenericMessageResponse logoutAll(User user) {

        refreshTokenService.revokeAllForUser(user);
        securityAuditService.record(
                user.getId(),
                SecurityAuditEventType.LOGOUT_ALL
        );

        return new GenericMessageResponse("Logged out from all sessions.");
    }

    private User loadActiveUserByEmail(String email) {

        User user = userRepository
                .findByEmailIgnoreCase(normalizeEmail(email))
                .orElseThrow(() -> new AuthenticationFailedException(
                        INVALID_LOGIN
                ));

        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new AuthenticationFailedException(INVALID_LOGIN);
        }

        return user;
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private AuthenticatedUser requireAuthenticatedUser(
            Authentication authentication
    ) {

        Object principal = authentication.getPrincipal();

        if (principal instanceof AuthenticatedUser authenticatedUser) {
            return authenticatedUser;
        }

        throw new AuthenticationFailedException(INVALID_LOGIN);
    }

    private AuthTokenResponse tokenResponse(
            IssuedAccessToken accessToken,
            String refreshToken
    ) {

        return new AuthTokenResponse(
                accessToken.tokenValue(),
                refreshToken,
                "Bearer",
                accessToken.expiresInSeconds()
        );
    }
}
