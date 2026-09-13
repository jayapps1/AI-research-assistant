package com.researchassistant.identity.controller;

import com.researchassistant.identity.dto.AuthTokenResponse;
import com.researchassistant.identity.dto.CompletePasswordResetRequest;
import com.researchassistant.identity.dto.CreateUserRequest;
import com.researchassistant.identity.dto.ForgotPasswordRequest;
import com.researchassistant.identity.dto.GenericMessageResponse;
import com.researchassistant.identity.dto.LoginRequest;
import com.researchassistant.identity.dto.LogoutRequest;
import com.researchassistant.identity.dto.PasswordResetAuthorizationResponse;
import com.researchassistant.identity.dto.RecoveryCodesResponse;
import com.researchassistant.identity.dto.RefreshTokenRequest;
import com.researchassistant.identity.dto.TotpConfirmRequest;
import com.researchassistant.identity.dto.TotpEnrollmentCompleteResponse;
import com.researchassistant.identity.dto.TotpEnrollmentResponse;
import com.researchassistant.identity.dto.TotpStepUpRequest;
import com.researchassistant.identity.dto.UserResponse;
import com.researchassistant.identity.dto.VerifyPasswordRecoveryCodeRequest;
import com.researchassistant.identity.dto.VerifyPasswordRecoveryTotpRequest;
import com.researchassistant.identity.entity.User;
import com.researchassistant.identity.service.AuthService;
import com.researchassistant.identity.service.AuthenticatedUserResolver;
import com.researchassistant.identity.service.PasswordRecoveryService;
import com.researchassistant.identity.service.TotpManagementService;
import com.researchassistant.identity.service.UserService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Public authentication entry points for the Research Assistant.
 *
 * <p>This controller currently provides account registration.
 * Login, token issuance, token refresh, logout and email
 * verification will be added as the authentication module
 * develops.</p>
 */
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final UserService userService;
    private final AuthService authService;
    private final PasswordRecoveryService passwordRecoveryService;
    private final AuthenticatedUserResolver authenticatedUserResolver;
    private final TotpManagementService totpManagementService;

    /**
     * Creates the controller with its required user service.
     *
     * @param userService application service for user operations
     */
    public AuthController(
            UserService userService,
            AuthService authService,
            PasswordRecoveryService passwordRecoveryService,
            AuthenticatedUserResolver authenticatedUserResolver,
            TotpManagementService totpManagementService
    ) {
        this.userService = userService;
        this.authService = authService;
        this.passwordRecoveryService = passwordRecoveryService;
        this.authenticatedUserResolver = authenticatedUserResolver;
        this.totpManagementService = totpManagementService;
    }


    /**
     * Registers a new platform user.
     *
     * <p>The supplied password is never persisted directly.
     * {@link UserService} encodes it before storing the user.</p>
     *
     * @param request validated registration request
     * @return safe representation of the created account
     */
    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse register(
            @Valid @RequestBody CreateUserRequest request
    ) {
        return userService.createUser(request);
    }


    /**
     * Authenticates an active user and issues an access-token pair.
     *
     * @param request validated login request
     * @param httpRequest servlet request used for refresh-session metadata
     * @return access JWT and opaque refresh token
     */
    @PostMapping("/login")
    public AuthTokenResponse login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest
    ) {
        return authService.login(request, httpRequest);
    }


    /**
     * Rotates a refresh token and returns a replacement token pair.
     *
     * @param request validated refresh request
     * @param httpRequest servlet request used for refresh-session metadata
     * @return replacement access JWT and refresh token
     */
    @PostMapping("/refresh")
    public AuthTokenResponse refresh(
            @Valid @RequestBody RefreshTokenRequest request,
            HttpServletRequest httpRequest
    ) {
        return authService.refresh(request, httpRequest);
    }


    /**
     * Revokes the submitted refresh token. Existing access JWTs
     * remain valid until their short expiration; logout removes the
     * ability to refresh.
     */
    @PostMapping("/logout")
    public GenericMessageResponse logout(
            @Valid @RequestBody LogoutRequest request
    ) {
        return authService.logout(request);
    }


    /**
     * Revokes all refresh sessions for the authenticated user.
     */
    @PostMapping("/logout-all")
    public GenericMessageResponse logoutAll(Authentication authentication) {

        User user = authenticatedUserResolver.requireActiveUser(
                authentication
        );

        return authService.logoutAll(user);
    }


    /**
     * Starts password recovery without revealing account existence
     * or configured recovery factors.
     */
    @PostMapping("/password/forgot")
    public GenericMessageResponse forgotPassword(
            @Valid @RequestBody ForgotPasswordRequest request
    ) {
        return passwordRecoveryService.forgotPassword(request);
    }


    /**
     * Verifies the user's existing enrolled TOTP credential for
     * password recovery and returns a short-lived reset
     * authorization.
     */
    @PostMapping("/password/verify-totp")
    public PasswordResetAuthorizationResponse verifyPasswordRecoveryTotp(
            @Valid @RequestBody VerifyPasswordRecoveryTotpRequest request
    ) {
        return passwordRecoveryService.verifyTotp(request);
    }


    @PostMapping("/password/verify-recovery-code")
    public PasswordResetAuthorizationResponse verifyPasswordRecoveryCode(
            @Valid @RequestBody VerifyPasswordRecoveryCodeRequest request
    ) {
        return passwordRecoveryService.verifyRecoveryCode(request);
    }


    /**
     * Completes password reset using a single-purpose reset
     * authorization. This endpoint does not accept normal access
     * tokens as reset proof.
     */
    @PostMapping("/password/reset")
    public GenericMessageResponse resetPassword(
            @Valid @RequestBody CompletePasswordResetRequest request
    ) {
        return passwordRecoveryService.completeReset(request);
    }


    @PostMapping("/totp/enrollment")
    public TotpEnrollmentResponse startTotpEnrollment(
            Authentication authentication
    ) {

        User user = authenticatedUserResolver.requireActiveUser(
                authentication
        );

        return totpManagementService.startEnrollment(user);
    }


    @PostMapping("/totp/enrollment/confirm")
    public TotpEnrollmentCompleteResponse confirmTotpEnrollment(
            Authentication authentication,
            @Valid @RequestBody TotpConfirmRequest request
    ) {

        User user = authenticatedUserResolver.requireActiveUser(
                authentication
        );

        return totpManagementService.confirmEnrollment(
                user,
                request.totpCode()
        );
    }


    @PostMapping("/totp/recovery-codes/regenerate")
    public RecoveryCodesResponse regenerateTotpRecoveryCodes(
            Authentication authentication,
            @Valid @RequestBody TotpStepUpRequest request
    ) {

        User user = authenticatedUserResolver.requireActiveUser(
                authentication
        );

        return totpManagementService.regenerateRecoveryCodes(user, request);
    }


    @PostMapping("/totp/disable")
    public GenericMessageResponse disableTotp(
            Authentication authentication,
            @Valid @RequestBody TotpStepUpRequest request
    ) {

        User user = authenticatedUserResolver.requireActiveUser(
                authentication
        );

        totpManagementService.disableTotp(user, request);

        return new GenericMessageResponse("TOTP disabled.");
    }


    @PostMapping("/totp/re-enrollment")
    public TotpEnrollmentResponse startTotpReEnrollment(
            Authentication authentication,
            @Valid @RequestBody TotpStepUpRequest request
    ) {

        User user = authenticatedUserResolver.requireActiveUser(
                authentication
        );

        return totpManagementService.startReEnrollment(user, request);
    }
}
