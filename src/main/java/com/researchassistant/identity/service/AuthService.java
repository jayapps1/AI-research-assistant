package com.researchassistant.identity.service;

import com.researchassistant.admin.SystemUserRole;
import com.researchassistant.admin.SystemUserRoleRepository;
import com.researchassistant.common.exception.AuthenticationFailedException;
import com.researchassistant.identity.dto.AuthTokenResponse;
import com.researchassistant.identity.dto.CompleteTotpLoginChallengeRequest;
import com.researchassistant.identity.dto.GenericMessageResponse;
import com.researchassistant.identity.dto.LoginChallengeResponse;
import com.researchassistant.identity.dto.LoginRequest;
import com.researchassistant.identity.dto.LogoutRequest;
import com.researchassistant.identity.dto.RefreshTokenRequest;
import com.researchassistant.identity.dto.UserResponse;
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
import java.util.UUID;

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
    private final TotpLoginChallengeService totpLoginChallengeService;
    private final SystemUserRoleRepository systemUserRoleRepository;
    private final com.researchassistant.workspace.service.PersonalWorkspaceService personalWorkspaceService;
    private final com.researchassistant.security.totp.RecoveryCodeService recoveryCodeService;

    public AuthService(
            AuthenticationManager authenticationManager,
            UserRepository userRepository,
            JwtTokenService jwtTokenService,
            RefreshTokenService refreshTokenService,
            TotpService totpService,
            SecurityAuditService securityAuditService,
            TotpLoginChallengeService totpLoginChallengeService,
            SystemUserRoleRepository systemUserRoleRepository,
            com.researchassistant.workspace.service.PersonalWorkspaceService personalWorkspaceService,
            com.researchassistant.security.totp.RecoveryCodeService recoveryCodeService
    ) {
        this.authenticationManager = authenticationManager;
        this.userRepository = userRepository;
        this.jwtTokenService = jwtTokenService;
        this.refreshTokenService = refreshTokenService;
        this.totpService = totpService;
        this.securityAuditService = securityAuditService;
        this.totpLoginChallengeService = totpLoginChallengeService;
        this.systemUserRoleRepository = systemUserRoleRepository;
        this.personalWorkspaceService = personalWorkspaceService;
        this.recoveryCodeService = recoveryCodeService;
    }

    /**
     * Authenticates a user and returns a short-lived access token
     * plus a rotating opaque refresh token.
     */
    @Transactional
    public Object login(
            LoginRequest request,
            HttpServletRequest httpRequest
    ) {

        User user = authenticate(request);

        AuthenticationMethod userMethod = user.getAuthenticationMethod() == null
                ? AuthenticationMethod.PASSWORD
                : user.getAuthenticationMethod();

        if (userMethod == AuthenticationMethod.PASSWORD_AND_TOTP
                && request.requestedAuthenticationMethod() == AuthenticationMethod.PASSWORD
                && totpService.hasEnabledCredential(user)) {
            TotpLoginChallengeService.CreatedChallenge challenge =
                    totpLoginChallengeService.create(user);
            return new LoginChallengeResponse(
                    "TOTP_REQUIRED",
                    challenge.challengeId(),
                    AuthenticationMethod.PASSWORD_AND_TOTP.name(),
                    challenge.expiresInSeconds(),
                    user.getEmail()
            );
        }

        return issueTokenPair(user, httpRequest);
    }

    @Transactional
    public AuthTokenResponse completeTotpLoginChallenge(
            CompleteTotpLoginChallengeRequest request,
            HttpServletRequest httpRequest
    ) {
        UUID userId = totpLoginChallengeService.consume(request.challengeId());
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AuthenticationFailedException(INVALID_LOGIN));
        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new AuthenticationFailedException(INVALID_LOGIN);
        }

        if (request.hasTotpCode()) {
            totpService.verifyLoginCode(user, request.totpCode());
        } else if (request.hasRecoveryCode()) {
            recoveryCodeService.consumeRecoveryCode(user, request.recoveryCode());
            securityAuditService.record(
                    user.getId(),
                    SecurityAuditEventType.RECOVERY_CODE_USED
            );
        } else {
            throw new AuthenticationFailedException("Either authenticator code or recovery code is required.");
        }

        return issueTokenPair(user, httpRequest);
    }

    private AuthTokenResponse issueTokenPair(User user, HttpServletRequest httpRequest) {
        try {
            personalWorkspaceService.ensurePersonalWorkspace(user);
        } catch (Exception ex) {
            // Do not block authentication if personal workspace already exists or encounters non-critical race
        }
        IssuedAccessToken accessToken = jwtTokenService.issueAccessToken(user);
        IssuedRefreshToken refreshToken = refreshTokenService.createRefreshToken(user, httpRequest);
        return tokenResponse(accessToken, refreshToken.tokenValue(), user);
    }

    private User authenticate(LoginRequest request) {

        AuthenticationMethod method =
                request.requestedAuthenticationMethod();

        return switch (method) {
            case PASSWORD -> authenticatePassword(request);
            case TOTP -> authenticateTotp(request);
            case PASSWORD_AND_TOTP -> authenticatePasswordAndTotp(request);
            case PASSWORD_OR_TOTP -> authenticatePasswordOrTotp(request);
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

        return userRepository.findById(authenticatedUser.getUserId())
                .orElseThrow(() -> new AuthenticationFailedException(INVALID_LOGIN));
    }

    private User authenticateTotp(LoginRequest request) {

        User user = loadActiveUserByEmail(request.email());
        if (user.getAuthenticationMethod() == AuthenticationMethod.PASSWORD_AND_TOTP) {
            throw new AuthenticationFailedException("Password authentication is required for accounts with two-factor authentication.");
        }
        if (user.getAuthenticationMethod() == AuthenticationMethod.PASSWORD) {
            throw new AuthenticationFailedException("TOTP_NOT_CONFIGURED", INVALID_LOGIN);
        }

        if (request.recoveryCode() != null && !request.recoveryCode().isBlank()) {
            recoveryCodeService.consumeRecoveryCode(user, request.recoveryCode());
            securityAuditService.record(
                    user.getId(),
                    SecurityAuditEventType.RECOVERY_CODE_USED
            );
        } else {
            totpService.verifyLoginCode(user, request.totpCode());
        }

        return user;
    }

    private User authenticatePasswordAndTotp(LoginRequest request) {

        User user = authenticatePassword(request);
        totpService.verifyLoginCode(user, request.totpCode());

        return user;
    }

    private User authenticatePasswordOrTotp(LoginRequest request) {

        if ((request.totpCode() != null && !request.totpCode().isBlank())
                || (request.recoveryCode() != null && !request.recoveryCode().isBlank())) {
            return authenticateTotp(request);
        }

        return authenticatePassword(request);
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
                rotatedRefreshToken.refreshTokenValue(),
                rotatedRefreshToken.user()
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
            String refreshToken,
            User user
    ) {

        return new AuthTokenResponse(
                accessToken.tokenValue(),
                refreshToken,
                "Bearer",
                accessToken.expiresInSeconds(),
                userResponse(user)
        );
    }

    private UserResponse userResponse(User user) {
        return new UserResponse(
                user.getId(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                user.getPhoneNumber(),
                user.getStatus(),
                user.isEmailVerified(),
                user.getLocale(),
                systemUserRoleRepository.findAllByUserId(user.getId())
                        .stream()
                        .map(SystemUserRole::getRole)
                        .map(Enum::name)
                        .toList(),
                user.getCreatedAt(),
                user.getUpdatedAt()
        );
    }
}
