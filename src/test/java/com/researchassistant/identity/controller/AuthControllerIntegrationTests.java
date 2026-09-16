package com.researchassistant.identity.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.researchassistant.identity.dto.AuthTokenResponse;
import com.researchassistant.identity.dto.CreateUserRequest;
import com.researchassistant.identity.dto.LoginChallengeResponse;
import com.researchassistant.identity.dto.PasswordResetAuthorizationResponse;
import com.researchassistant.identity.dto.RecoveryCodesResponse;
import com.researchassistant.identity.dto.TotpEnrollmentCompleteResponse;
import com.researchassistant.identity.dto.TotpEnrollmentResponse;
import com.researchassistant.identity.dto.UserResponse;
import com.researchassistant.identity.entity.User;
import com.researchassistant.identity.repository.UserRepository;
import com.researchassistant.identity.service.UserService;
import com.researchassistant.security.credentials.CredentialSecretEncryptor;
import com.researchassistant.security.jwt.RefreshSession;
import com.researchassistant.security.jwt.RefreshSessionRepository;
import com.researchassistant.security.jwt.RefreshTokenService;
import com.researchassistant.security.totp.TotpCredential;
import com.researchassistant.security.totp.TotpCredentialRepository;
import com.researchassistant.security.totp.TotpRecoveryCode;
import com.researchassistant.security.totp.TotpRecoveryCodeRepository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "app.security.jwt.secret=MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=",
        "app.security.credentials.encryption-key=MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY="
})
@AutoConfigureMockMvc
@Transactional
class AuthControllerIntegrationTests {

    private static final String PASSWORD = "correct-password-123";
    private static final String TOTP_SECRET =
            "JBSWY3DPEHPK3PXPJBSWY3DPEHPK3PXP";

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule());

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RefreshSessionRepository refreshSessionRepository;

    @Autowired
    private RefreshTokenService refreshTokenService;

    @Autowired
    private TotpCredentialRepository totpCredentialRepository;

    @Autowired
    private TotpRecoveryCodeRepository totpRecoveryCodeRepository;

    @Autowired
    private CredentialSecretEncryptor credentialSecretEncryptor;

    @Autowired
    private JwtDecoder jwtDecoder;

    @Test
    void loginWithCorrectPasswordReturnsTokenPair() throws Exception {

        UserResponse user = createUser();

        AuthTokenResponse response = login(user.email(), PASSWORD);

        assertThat(response.accessToken()).isNotBlank();
        assertThat(response.refreshToken()).isNotBlank();
        assertThat(response.tokenType()).isEqualTo("Bearer");
        assertThat(response.expiresIn()).isEqualTo(900);
    }

    @Test
    void loginWithIncorrectPasswordReturnsUnauthorized()
            throws Exception {

        UserResponse user = createUser();

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", user.email(),
                                "password", "wrong-password"
                        ))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message")
                        .value("Invalid credentials."));
    }

    @Test
    void duplicateEmailRegistrationStillReturnsConflict()
            throws Exception {

        UserResponse user = createUser();

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", user.email().toUpperCase(),
                                "password", PASSWORD,
                                "firstName", "Second"
                        ))))
                .andExpect(status().isConflict());
    }

    @Test
    void validRefreshRotatesRefreshTokenAndIssuesNewAccessToken()
            throws Exception {

        UserResponse user = createUser();
        AuthTokenResponse loginResponse = login(user.email(), PASSWORD);

        AuthTokenResponse refreshResponse = refresh(
                loginResponse.refreshToken()
        );

        assertThat(refreshResponse.accessToken()).isNotBlank();
        assertThat(refreshResponse.refreshToken())
                .isNotEqualTo(loginResponse.refreshToken());
        assertThat(refreshResponse.tokenType()).isEqualTo("Bearer");
        assertThat(refreshResponse.expiresIn()).isEqualTo(900);
    }

    @Test
    void expiredRefreshTokenReturnsUnauthorized()
            throws Exception {

        String rawToken = "expired-token-" + UUID.randomUUID();
        createRefreshSession(rawToken, true, false);

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "refreshToken", rawToken
                        ))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message")
                        .value("Invalid refresh token."));
    }

    @Test
    void revokedRefreshTokenReturnsUnauthorized()
            throws Exception {

        String rawToken = "revoked-token-" + UUID.randomUUID();
        createRefreshSession(rawToken, false, true);

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "refreshToken", rawToken
                        ))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message")
                        .value("Invalid refresh token."));
    }

    @Test
    void reusedRotatedRefreshTokenReturnsUnauthorized()
            throws Exception {

        UserResponse user = createUser();
        AuthTokenResponse loginResponse = login(user.email(), PASSWORD);

        refresh(loginResponse.refreshToken());

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "refreshToken",
                                loginResponse.refreshToken()
                        ))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message")
                        .value("Invalid refresh token."));
    }

    @Test
    void accessTokenContainsExpectedClaims() throws Exception {

        UserResponse user = createUser();
        AuthTokenResponse response = login(user.email(), PASSWORD);

        Jwt jwt = jwtDecoder.decode(response.accessToken());

        assertThat(jwt.getClaimAsString("iss"))
                .isEqualTo("research-assistant-api");
        assertThat(jwt.getSubject()).isEqualTo(user.id().toString());
        assertThat(jwt.getClaimAsString("email")).isEqualTo(user.email());
        assertThat(jwt.getIssuedAt()).isNotNull();
        assertThat(jwt.getExpiresAt()).isNotNull();
        assertThat(jwt.getId()).isNotBlank();
        assertThat(jwt.getExpiresAt()).isAfter(jwt.getIssuedAt());
    }

    @Test
    void totpLoginWithVerifiedCredentialReturnsTokenPair()
            throws Exception {

        UserResponse userResponse = createUser();
        User user = userRepository.findByEmailIgnoreCase(userResponse.email())
                .orElseThrow();
        createVerifiedTotpCredential(user, null);

        AuthTokenResponse response = loginWithTotp(
                userResponse.email(),
                currentTotpCode(TOTP_SECRET)
        );

        assertThat(response.accessToken()).isNotBlank();
        assertThat(response.refreshToken()).isNotBlank();
        assertThat(response.tokenType()).isEqualTo("Bearer");
    }

    @Test
    void totpLoginWithoutCredentialReturnsGenericUnauthorized()
            throws Exception {

        UserResponse user = createUser();

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", user.email(),
                                "authenticationMethod", "TOTP",
                                "totpCode", "123456"
                        ))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message")
                        .value("Invalid credentials."));
    }

    @Test
    void reusedTotpTimestepReturnsGenericUnauthorized()
            throws Exception {

        UserResponse userResponse = createUser();
        User user = userRepository.findByEmailIgnoreCase(userResponse.email())
                .orElseThrow();
        createVerifiedTotpCredential(user, null);
        String code = currentTotpCode(TOTP_SECRET);

        loginWithTotp(userResponse.email(), code);

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", userResponse.email(),
                                "authenticationMethod", "TOTP",
                                "totpCode", code
                        ))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message")
                        .value("Invalid credentials."));
    }

    @Test
    void passwordAndTotpLoginRequiresBothFactors()
            throws Exception {

        UserResponse userResponse = createUser();
        User user = userRepository.findByEmailIgnoreCase(userResponse.email())
                .orElseThrow();
        createVerifiedTotpCredential(user, null);

        AuthTokenResponse response = loginWithPasswordAndTotp(
                userResponse.email(),
                PASSWORD,
                currentTotpCode(TOTP_SECRET)
        );

        assertThat(response.accessToken()).isNotBlank();
        assertThat(response.refreshToken()).isNotBlank();
    }

    @Test
    void passwordStepReturnsTotpChallengeForEnrolledAccount()
            throws Exception {

        UserResponse userResponse = createUser();
        User user = userRepository.findByEmailIgnoreCase(userResponse.email())
                .orElseThrow();
        createVerifiedTotpCredential(user, null);

        LoginChallengeResponse response =
                passwordLoginChallenge(userResponse.email(), PASSWORD);

        assertThat(response.status()).isEqualTo("TOTP_REQUIRED");
        assertThat(response.authenticationMethod())
                .isEqualTo("PASSWORD_AND_TOTP");
        assertThat(response.challengeId()).isNotBlank();
        assertThat(response.expiresIn()).isEqualTo(300);
    }

    @Test
    void validTotpChallengeCompletesAuthentication()
            throws Exception {

        UserResponse userResponse = createUser();
        User user = userRepository.findByEmailIgnoreCase(userResponse.email())
                .orElseThrow();
        createVerifiedTotpCredential(user, null);
        LoginChallengeResponse challenge =
                passwordLoginChallenge(userResponse.email(), PASSWORD);

        AuthTokenResponse response = completeTotpChallenge(
                challenge.challengeId(),
                currentTotpCode(TOTP_SECRET)
        );

        assertThat(response.accessToken()).isNotBlank();
        assertThat(response.refreshToken()).isNotBlank();
    }

    @Test
    void invalidTotpChallengeCodeFails() throws Exception {

        UserResponse userResponse = createUser();
        User user = userRepository.findByEmailIgnoreCase(userResponse.email())
                .orElseThrow();
        createVerifiedTotpCredential(user, null);
        LoginChallengeResponse challenge =
                passwordLoginChallenge(userResponse.email(), PASSWORD);

        mockMvc.perform(post("/api/v1/auth/login/totp-challenge")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "challengeId", challenge.challengeId(),
                                "totpCode", "000000"
                        ))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message")
                        .value("Invalid credentials."));
    }

    @Test
    void unknownTotpChallengeFailsSafely() throws Exception {

        mockMvc.perform(post("/api/v1/auth/login/totp-challenge")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "challengeId", UUID.randomUUID().toString(),
                                "totpCode", "000000"
                        ))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message")
                        .value("Invalid credentials."));
    }

    @Test
    void inactiveUserTotpLoginFails() throws Exception {
        UserResponse userResponse = createUser();
        User user = userRepository.findByEmailIgnoreCase(userResponse.email())
                .orElseThrow();
        createVerifiedTotpCredential(user, null);
        user.setStatus(com.researchassistant.identity.entity.UserStatus.INACTIVE);
        userRepository.save(user);

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", userResponse.email(),
                                "authenticationMethod", "TOTP",
                                "totpCode", currentTotpCode(TOTP_SECRET)
                        ))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid credentials."));
    }

    @Test
    void totpLoginRateLimitAppliesAfterMaxFailedAttempts() throws Exception {
        UserResponse userResponse = createUser();
        User user = userRepository.findByEmailIgnoreCase(userResponse.email())
                .orElseThrow();
        createVerifiedTotpCredential(user, null);

        for (int i = 0; i < 5; i++) {
            mockMvc.perform(post("/api/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(Map.of(
                                    "email", userResponse.email(),
                                    "authenticationMethod", "TOTP",
                                    "totpCode", "000000"
                            ))))
                    .andExpect(status().isUnauthorized());
        }

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", userResponse.email(),
                                "authenticationMethod", "TOTP",
                                "totpCode", currentTotpCode(TOTP_SECRET)
                        ))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid credentials."));
    }

    @Test
    void consumedOrExpiredTotpChallengeCannotBeReused() throws Exception {
        UserResponse userResponse = createUser();
        User user = userRepository.findByEmailIgnoreCase(userResponse.email())
                .orElseThrow();
        createVerifiedTotpCredential(user, null);
        LoginChallengeResponse challenge =
                passwordLoginChallenge(userResponse.email(), PASSWORD);

        completeTotpChallenge(challenge.challengeId(), currentTotpCode(TOTP_SECRET));

        mockMvc.perform(post("/api/v1/auth/login/totp-challenge")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "challengeId", challenge.challengeId(),
                                "totpCode", currentTotpCode(TOTP_SECRET)
                        ))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid credentials."));
    }

    @Test
    void passwordLoginWithoutTotpCredentialDoesNotRequireChallenge() throws Exception {
        UserResponse userResponse = createUser();

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", userResponse.email(),
                                "password", PASSWORD
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.status").doesNotExist());
    }

    @Test
    void forgotPasswordReturnsGenericMessageForUnknownAccount()
            throws Exception {

        mockMvc.perform(post("/api/v1/auth/password/forgot")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email",
                                "missing-" + UUID.randomUUID()
                                        + "@example.com"
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message")
                        .value("If the account can be recovered, recovery instructions are available."));
    }

    @Test
    void passwordRecoveryTotpUsesExistingCredentialAndIssuesResetToken()
            throws Exception {

        UserResponse userResponse = createUser();
        User user = userRepository.findByEmailIgnoreCase(userResponse.email())
                .orElseThrow();
        createVerifiedTotpCredential(user, null);

        PasswordResetAuthorizationResponse response =
                verifyPasswordRecoveryTotp(
                        userResponse.email(),
                        currentTotpCode(TOTP_SECRET)
                );

        assertThat(response.resetToken()).isNotBlank();
        assertThat(response.expiresIn()).isEqualTo(600);
    }

    @Test
    void totpCodeUsedForLoginCannotBeReusedForPasswordRecovery()
            throws Exception {

        UserResponse userResponse = createUser();
        User user = userRepository.findByEmailIgnoreCase(userResponse.email())
                .orElseThrow();
        createVerifiedTotpCredential(user, null);
        String code = currentTotpCode(TOTP_SECRET);

        loginWithTotp(userResponse.email(), code);

        mockMvc.perform(post("/api/v1/auth/password/verify-totp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", userResponse.email(),
                                "totpCode", code
                        ))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message")
                        .value("Invalid credentials."));
    }

    @Test
    void passwordResetTokenIsSingleUseAndRevokesOldRefreshToken()
            throws Exception {

        UserResponse userResponse = createUser();
        User user = userRepository.findByEmailIgnoreCase(userResponse.email())
                .orElseThrow();
        createVerifiedTotpCredential(user, null);
        AuthTokenResponse loginResponse =
                loginWithPasswordAndTotp(
                        userResponse.email(),
                        PASSWORD,
                        currentTotpCode(TOTP_SECRET)
                );

        PasswordResetAuthorizationResponse authorization =
                verifyPasswordRecoveryTotp(
                        userResponse.email(),
                        nextTotpCode(TOTP_SECRET)
                );

        mockMvc.perform(post("/api/v1/auth/password/reset")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "resetToken", authorization.resetToken(),
                                "newPassword", "new-password-12345"
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message")
                        .value("Password has been reset."));

        mockMvc.perform(post("/api/v1/auth/password/reset")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "resetToken", authorization.resetToken(),
                                "newPassword", "another-password-12345"
                        ))))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "refreshToken",
                                loginResponse.refreshToken()
                        ))))
                .andExpect(status().isUnauthorized());

        LoginChallengeResponse newLoginChallenge =
                passwordLoginChallenge(
                        userResponse.email(),
                        "new-password-12345"
                );

        assertThat(newLoginChallenge.challengeId()).isNotBlank();
    }

    @Test
    void authenticatedTotpEnrollmentStartsPendingCredential()
            throws Exception {

        UserResponse user = createUser();
        AuthTokenResponse loginResponse = login(user.email(), PASSWORD);

        TotpEnrollmentResponse enrollment =
                startTotpEnrollment(loginResponse.accessToken());

        assertThat(enrollment.issuer()).isEqualTo("AI Research Assistant");
        assertThat(enrollment.accountName()).isEqualTo(user.email());
        assertThat(enrollment.secret()).isNotBlank();
        assertThat(enrollment.provisioningUri())
                .startsWith("otpauth://totp/");
        assertThat(enrollment.provisioningUri())
                .contains("issuer=AI%20Research%20Assistant");

        TotpCredential credential = totpCredentialRepository
                .findByUserId(user.id())
                .orElseThrow();

        assertThat(credential.isEnabled()).isFalse();
        assertThat(credential.getVerifiedAt()).isNull();
    }

    @Test
    void unauthenticatedTotpEnrollmentIsRejected()
            throws Exception {

        mockMvc.perform(post("/api/v1/auth/totp/enrollment"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void totpEnrollmentConfirmationEnablesCredentialAndRecoveryCodes()
            throws Exception {

        UserResponse user = createUser();
        AuthTokenResponse loginResponse = login(user.email(), PASSWORD);
        TotpEnrollmentResponse enrollment =
                startTotpEnrollment(loginResponse.accessToken());

        TotpEnrollmentCompleteResponse confirmation =
                confirmTotpEnrollment(
                        loginResponse.accessToken(),
                        currentTotpCode(enrollment.secret())
                );

        assertThat(confirmation.recoveryCodes()).hasSize(10);

        TotpCredential credential = totpCredentialRepository
                .findByUserId(user.id())
                .orElseThrow();

        assertThat(credential.isEnabled()).isTrue();
        assertThat(credential.getVerifiedAt()).isNotNull();

        for (String recoveryCode : confirmation.recoveryCodes()) {
            assertThat(totpRecoveryCodeRepository
                    .findByUserIdAndUsedAtIsNullAndRevokedAtIsNull(user.id()))
                    .noneMatch(stored ->
                            recoveryCode.equals(stored.getCodeHash())
                    );
        }

        assertThat(new UserResponse(
                user.id(),
                user.email(),
                user.firstName(),
                user.lastName(),
                user.phoneNumber(),
                user.status(),
                user.emailVerified(),
                user.locale(),
                user.systemRoles(),
                user.createdAt(),
                user.updatedAt()
        ).toString()).doesNotContain(enrollment.secret());
    }

    @Test
    void incorrectTotpEnrollmentConfirmationIsRejected()
            throws Exception {

        UserResponse user = createUser();
        AuthTokenResponse loginResponse = login(user.email(), PASSWORD);
        startTotpEnrollment(loginResponse.accessToken());

        mockMvc.perform(post("/api/v1/auth/totp/enrollment/confirm")
                        .header(
                                "Authorization",
                                "Bearer " + loginResponse.accessToken()
                        )
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "totpCode", "123456"
                        ))))
                .andExpect(status().isUnauthorized());

        TotpCredential credential = totpCredentialRepository
                .findByUserId(user.id())
                .orElseThrow();

        assertThat(credential.isEnabled()).isFalse();
    }

    @Test
    void recoveryCodeCanAuthorizePasswordResetOnlyOnce()
            throws Exception {

        UserResponse user = createUser();
        AuthTokenResponse loginResponse = login(user.email(), PASSWORD);
        TotpEnrollmentResponse enrollment =
                startTotpEnrollment(loginResponse.accessToken());
        TotpEnrollmentCompleteResponse confirmation =
                confirmTotpEnrollment(
                        loginResponse.accessToken(),
                        currentTotpCode(enrollment.secret())
                );
        String recoveryCode = confirmation.recoveryCodes().getFirst();

        PasswordResetAuthorizationResponse resetAuthorization =
                verifyPasswordRecoveryCode(user.email(), recoveryCode);

        assertThat(resetAuthorization.resetToken()).isNotBlank();

        mockMvc.perform(post("/api/v1/auth/password/verify-recovery-code")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", user.email(),
                                "recoveryCode", recoveryCode
                        ))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void recoveryCodeRegenerationRevokesPreviousCodes()
            throws Exception {

        UserResponse user = createUser();
        AuthTokenResponse loginResponse = login(user.email(), PASSWORD);
        TotpEnrollmentResponse enrollment =
                startTotpEnrollment(loginResponse.accessToken());
        TotpEnrollmentCompleteResponse confirmation =
                confirmTotpEnrollment(
                        loginResponse.accessToken(),
                        currentTotpCode(enrollment.secret())
                );
        String oldRecoveryCode = confirmation.recoveryCodes().getFirst();

        RecoveryCodesResponse regenerated = regenerateRecoveryCodes(
                loginResponse.accessToken(),
                oldRecoveryCode
        );

        assertThat(regenerated.recoveryCodes()).hasSize(10);
        assertThat(regenerated.recoveryCodes())
                .doesNotContainAnyElementsOf(confirmation.recoveryCodes());

        mockMvc.perform(post("/api/v1/auth/password/verify-recovery-code")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", user.email(),
                                "recoveryCode", oldRecoveryCode
                        ))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void totpDisableRequiresStepUpAndRevokesRecoveryCodes()
            throws Exception {

        UserResponse user = createUser();
        AuthTokenResponse loginResponse = login(user.email(), PASSWORD);
        TotpEnrollmentResponse enrollment =
                startTotpEnrollment(loginResponse.accessToken());
        TotpEnrollmentCompleteResponse confirmation =
                confirmTotpEnrollment(
                        loginResponse.accessToken(),
                        currentTotpCode(enrollment.secret())
                );
        String recoveryCode = confirmation.recoveryCodes().getFirst();

        mockMvc.perform(post("/api/v1/auth/totp/disable")
                        .header(
                                "Authorization",
                                "Bearer " + loginResponse.accessToken()
                        )
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "password", PASSWORD
                        ))))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/v1/auth/totp/disable")
                        .header(
                                "Authorization",
                                "Bearer " + loginResponse.accessToken()
                        )
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "password", PASSWORD,
                                "recoveryCode", recoveryCode
                        ))))
                .andExpect(status().isOk());

        TotpCredential credential = totpCredentialRepository
                .findByUserId(user.id())
                .orElseThrow();

        assertThat(credential.isEnabled()).isFalse();
        assertThat(credential.getDisabledAt()).isNotNull();
        assertThat(totpRecoveryCodeRepository
                .findByUserIdAndUsedAtIsNullAndRevokedAtIsNull(user.id()))
                .isEmpty();
    }

    @Test
    void logoutRevokesOnlySubmittedRefreshToken()
            throws Exception {

        UserResponse user = createUser();
        AuthTokenResponse firstLogin = login(user.email(), PASSWORD);
        AuthTokenResponse secondLogin = login(user.email(), PASSWORD);

        mockMvc.perform(post("/api/v1/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "refreshToken", firstLogin.refreshToken()
                        ))))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "refreshToken", firstLogin.refreshToken()
                        ))))
                .andExpect(status().isUnauthorized());

        AuthTokenResponse refreshedSecond =
                refresh(secondLogin.refreshToken());

        assertThat(refreshedSecond.accessToken()).isNotBlank();
    }

    @Test
    void logoutAllRevokesOnlyAuthenticatedUsersSessions()
            throws Exception {

        UserResponse firstUser = createUser();
        UserResponse secondUser = createUser();
        AuthTokenResponse firstLogin = login(firstUser.email(), PASSWORD);
        AuthTokenResponse firstSecondSession =
                login(firstUser.email(), PASSWORD);
        AuthTokenResponse otherUserLogin = login(secondUser.email(), PASSWORD);

        mockMvc.perform(post("/api/v1/auth/logout-all")
                        .header(
                                "Authorization",
                                "Bearer " + firstLogin.accessToken()
                        ))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "refreshToken",
                                firstSecondSession.refreshToken()
                        ))))
                .andExpect(status().isUnauthorized());

        AuthTokenResponse otherRefresh =
                refresh(otherUserLogin.refreshToken());

        assertThat(otherRefresh.accessToken()).isNotBlank();
    }

    @Test
    void totpReEnrollmentKeepsOldSecretUntilNewSecretConfirmed()
            throws Exception {

        UserResponse user = createUser();
        AuthTokenResponse loginResponse = login(user.email(), PASSWORD);
        TotpEnrollmentResponse initialEnrollment =
                startTotpEnrollment(loginResponse.accessToken());
        TotpEnrollmentCompleteResponse initialConfirmation =
                confirmTotpEnrollment(
                        loginResponse.accessToken(),
                        currentTotpCode(initialEnrollment.secret())
                );
        String recoveryCode = initialConfirmation.recoveryCodes().getFirst();

        TotpEnrollmentResponse rotationEnrollment =
                startTotpReEnrollment(
                        loginResponse.accessToken(),
                        recoveryCode
                );

        assertThat(rotationEnrollment.secret())
                .isNotEqualTo(initialEnrollment.secret());

        AuthTokenResponse oldTotpLogin = loginWithTotp(
                user.email(),
                nextTotpCode(initialEnrollment.secret())
        );

        assertThat(oldTotpLogin.accessToken()).isNotBlank();

        TotpEnrollmentCompleteResponse rotated =
                confirmTotpEnrollment(
                        loginResponse.accessToken(),
                        currentTotpCode(rotationEnrollment.secret())
                );

        assertThat(rotated.recoveryCodes()).hasSize(10);

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", user.email(),
                                "authenticationMethod", "TOTP",
                                "totpCode",
                                nextTotpCode(initialEnrollment.secret())
                        ))))
                .andExpect(status().isUnauthorized());
    }

    private UserResponse createUser() {

        String email = "auth-test-" + UUID.randomUUID() + "@example.com";

        return userService.createUser(new CreateUserRequest(
                email,
                PASSWORD,
                "Auth",
                "Test",
                "en"
        ));
    }

    private AuthTokenResponse login(String email, String password)
            throws Exception {

        String responseJson = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", email,
                                "password", password
                        ))))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        return objectMapper.readValue(responseJson, AuthTokenResponse.class);
    }

    private AuthTokenResponse loginWithTotp(String email, String totpCode)
            throws Exception {

        String responseJson = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", email,
                                "authenticationMethod", "TOTP",
                                "totpCode", totpCode
                        ))))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        return objectMapper.readValue(responseJson, AuthTokenResponse.class);
    }

    private AuthTokenResponse loginWithPasswordAndTotp(
            String email,
            String password,
            String totpCode
    ) throws Exception {

        String responseJson = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", email,
                                "authenticationMethod", "PASSWORD_AND_TOTP",
                                "password", password,
                                "totpCode", totpCode
                        ))))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        return objectMapper.readValue(responseJson, AuthTokenResponse.class);
    }

    private LoginChallengeResponse passwordLoginChallenge(
            String email,
            String password
    ) throws Exception {

        String responseJson = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", email,
                                "password", password
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("TOTP_REQUIRED"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        return objectMapper.readValue(
                responseJson,
                LoginChallengeResponse.class
        );
    }

    private AuthTokenResponse completeTotpChallenge(
            String challengeId,
            String totpCode
    ) throws Exception {

        String responseJson = mockMvc.perform(
                        post("/api/v1/auth/login/totp-challenge")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(
                                        Map.of(
                                                "challengeId", challengeId,
                                                "totpCode", totpCode
                                        )
                                )))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        return objectMapper.readValue(responseJson, AuthTokenResponse.class);
    }

    private AuthTokenResponse refresh(String refreshToken)
            throws Exception {

        String responseJson = mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "refreshToken", refreshToken
                        ))))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        return objectMapper.readValue(responseJson, AuthTokenResponse.class);
    }

    private PasswordResetAuthorizationResponse verifyPasswordRecoveryTotp(
            String email,
            String totpCode
    ) throws Exception {

        String responseJson = mockMvc
                .perform(post("/api/v1/auth/password/verify-totp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", email,
                                "totpCode", totpCode
                        ))))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        return objectMapper.readValue(
                responseJson,
                PasswordResetAuthorizationResponse.class
        );
    }

    private PasswordResetAuthorizationResponse verifyPasswordRecoveryCode(
            String email,
            String recoveryCode
    ) throws Exception {

        String responseJson = mockMvc
                .perform(post("/api/v1/auth/password/verify-recovery-code")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", email,
                                "recoveryCode", recoveryCode
                        ))))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        return objectMapper.readValue(
                responseJson,
                PasswordResetAuthorizationResponse.class
        );
    }

    private TotpEnrollmentResponse startTotpEnrollment(String accessToken)
            throws Exception {

        String responseJson = mockMvc
                .perform(post("/api/v1/auth/totp/enrollment")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        return objectMapper.readValue(
                responseJson,
                TotpEnrollmentResponse.class
        );
    }

    private TotpEnrollmentCompleteResponse confirmTotpEnrollment(
            String accessToken,
            String totpCode
    ) throws Exception {

        String responseJson = mockMvc
                .perform(post("/api/v1/auth/totp/enrollment/confirm")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "totpCode", totpCode
                        ))))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        return objectMapper.readValue(
                responseJson,
                TotpEnrollmentCompleteResponse.class
        );
    }

    private RecoveryCodesResponse regenerateRecoveryCodes(
            String accessToken,
            String recoveryCode
    ) throws Exception {

        String responseJson = mockMvc
                .perform(post("/api/v1/auth/totp/recovery-codes/regenerate")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "recoveryCode", recoveryCode
                        ))))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        return objectMapper.readValue(
                responseJson,
                RecoveryCodesResponse.class
        );
    }

    private TotpEnrollmentResponse startTotpReEnrollment(
            String accessToken,
            String recoveryCode
    ) throws Exception {

        String responseJson = mockMvc
                .perform(post("/api/v1/auth/totp/re-enrollment")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "recoveryCode", recoveryCode
                        ))))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        return objectMapper.readValue(
                responseJson,
                TotpEnrollmentResponse.class
        );
    }

    private void createRefreshSession(
            String rawToken,
            boolean expired,
            boolean revoked
    ) {

        UserResponse userResponse = createUser();
        User user = userRepository.findByEmailIgnoreCase(userResponse.email())
                .orElseThrow();

        OffsetDateTime now = OffsetDateTime.now();

        RefreshSession session = new RefreshSession();
        session.setUser(user);
        session.setTokenHash(refreshTokenService.hashToken(rawToken));
        session.setCreatedAt(now.minusDays(2));
        session.setExpiresAt(
                expired ? now.minusDays(1) : now.plusDays(30)
        );
        session.setRevokedAt(revoked ? now.minusMinutes(1) : null);

        refreshSessionRepository.save(session);
    }

    private void createVerifiedTotpCredential(
            User user,
            Long lastUsedTimestep
    ) {

        TotpCredential credential = new TotpCredential();
        credential.setUser(user);
        credential.setEncryptedSecret(
                credentialSecretEncryptor.encrypt(TOTP_SECRET)
        );
        credential.setEnabled(true);
        credential.setVerifiedAt(OffsetDateTime.now().minusMinutes(1));
        credential.setLastUsedTimestep(lastUsedTimestep);

        totpCredentialRepository.save(credential);
    }

    private String currentTotpCode(String secret) throws Exception {
        return totpCode(secret, Instant.now().getEpochSecond() / 30);
    }

    private String nextTotpCode(String secret) throws Exception {
        return totpCode(secret, (Instant.now().getEpochSecond() / 30) + 1);
    }

    private String totpCode(String secret, long timestep) throws Exception {

        Mac mac = Mac.getInstance("HmacSHA1");
        mac.init(new SecretKeySpec(decodeBase32(secret), "HmacSHA1"));
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

        return String.format("%06d", binary % 1_000_000);
    }

    private byte[] decodeBase32(String value) {

        int buffer = 0;
        int bitsLeft = 0;
        byte[] output = new byte[value.length() * 5 / 8];
        int outputIndex = 0;

        for (char character : value.toCharArray()) {
            int decoded = character >= 'A' && character <= 'Z'
                    ? character - 'A'
                    : character - '2' + 26;
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
}
