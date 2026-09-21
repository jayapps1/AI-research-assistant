package com.researchassistant.identity.service;

import com.researchassistant.common.exception.AuthenticationFailedException;
import com.researchassistant.identity.dto.RecoveryCodesResponse;
import com.researchassistant.identity.dto.TotpEnrollmentCompleteResponse;
import com.researchassistant.identity.dto.TotpEnrollmentResponse;
import com.researchassistant.identity.dto.TotpStepUpRequest;
import com.researchassistant.identity.entity.User;
import com.researchassistant.security.audit.SecurityAuditEventType;
import com.researchassistant.security.audit.SecurityAuditService;
import com.researchassistant.security.jwt.RefreshTokenService;
import com.researchassistant.security.totp.RecoveryCodeService;
import com.researchassistant.security.totp.TotpProvisioningDetails;
import com.researchassistant.security.totp.TotpService;
import com.researchassistant.security.totp.TotpVerificationPurpose;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * User-facing TOTP account management workflows.
 */
@Service
public class TotpManagementService {

    private static final String INVALID_CREDENTIALS =
            "Invalid credentials.";

    private final TotpService totpService;
    private final RecoveryCodeService recoveryCodeService;
    private final RefreshTokenService refreshTokenService;
    private final AuthenticationManager authenticationManager;
    private final SecurityAuditService securityAuditService;
    private final com.researchassistant.identity.repository.UserRepository userRepository;

    public TotpManagementService(
            TotpService totpService,
            RecoveryCodeService recoveryCodeService,
            RefreshTokenService refreshTokenService,
            AuthenticationManager authenticationManager,
            SecurityAuditService securityAuditService,
            com.researchassistant.identity.repository.UserRepository userRepository
    ) {
        this.totpService = totpService;
        this.recoveryCodeService = recoveryCodeService;
        this.refreshTokenService = refreshTokenService;
        this.authenticationManager = authenticationManager;
        this.securityAuditService = securityAuditService;
        this.userRepository = userRepository;
    }

    @Transactional
    public TotpEnrollmentResponse startEnrollment(User user) {

        if (totpService.hasEnabledCredential(user)) {
            throw new AuthenticationFailedException(INVALID_CREDENTIALS);
        }

        TotpProvisioningDetails details = totpService.beginEnrollment(user);
        securityAuditService.record(
                user.getId(),
                SecurityAuditEventType.TOTP_ENROLLMENT_STARTED
        );

        return enrollmentResponse(user, details);
    }

    @Transactional
    public TotpEnrollmentResponse startReEnrollment(
            User user,
            TotpStepUpRequest request
    ) {

        requireTotpOrRecoveryCode(user, request);

        TotpProvisioningDetails details = totpService.beginRotation(user);
        securityAuditService.record(
                user.getId(),
                SecurityAuditEventType.TOTP_ENROLLMENT_STARTED
        );

        return enrollmentResponse(user, details);
    }

    @Transactional
    public TotpEnrollmentCompleteResponse confirmEnrollment(
            User user,
            String totpCode
    ) {

        boolean rotation = totpService.confirmEnrollment(user, totpCode);
        List<String> recoveryCodes =
                recoveryCodeService.replaceRecoveryCodes(user);

        securityAuditService.record(
                user.getId(),
                rotation
                        ? SecurityAuditEventType.TOTP_ROTATED
                        : SecurityAuditEventType.TOTP_ENABLED
        );
        securityAuditService.record(
                user.getId(),
                SecurityAuditEventType.RECOVERY_CODES_GENERATED
        );

        if (user.getAuthenticationMethod() == null
                || user.getAuthenticationMethod() == com.researchassistant.identity.entity.AuthenticationMethod.PASSWORD) {
            user.setAuthenticationMethod(com.researchassistant.identity.entity.AuthenticationMethod.PASSWORD_OR_TOTP);
            userRepository.save(user);
        }

        return new TotpEnrollmentCompleteResponse(
                rotation ? "TOTP authenticator rotated." : "TOTP enabled.",
                recoveryCodes
        );
    }

    @Transactional
    public RecoveryCodesResponse regenerateRecoveryCodes(
            User user,
            TotpStepUpRequest request
    ) {

        requireTotpOrRecoveryCode(user, request);

        List<String> recoveryCodes =
                recoveryCodeService.replaceRecoveryCodes(user);

        securityAuditService.record(
                user.getId(),
                SecurityAuditEventType.RECOVERY_CODES_REGENERATED
        );

        return new RecoveryCodesResponse(recoveryCodes);
    }

    @Transactional
    public void disableTotp(User user, TotpStepUpRequest request) {

        requirePassword(user, request.password());

        if (request.totpCode() != null && !request.totpCode().isBlank()) {
            totpService.verifyCode(
                    user,
                    request.totpCode(),
                    TotpVerificationPurpose.SENSITIVE_ACTION
            );
        } else if (request.recoveryCode() != null
                && !request.recoveryCode().isBlank()) {
            recoveryCodeService.consumeRecoveryCode(
                    user,
                    request.recoveryCode()
            );
            securityAuditService.record(
                    user.getId(),
                    SecurityAuditEventType.RECOVERY_CODE_USED
            );
        } else {
            throw new AuthenticationFailedException(INVALID_CREDENTIALS);
        }

        totpService.disable(user);
        recoveryCodeService.revokeAvailableCodes(user);
        refreshTokenService.revokeAllForUser(user);

        user.setAuthenticationMethod(com.researchassistant.identity.entity.AuthenticationMethod.PASSWORD);
        userRepository.save(user);

        securityAuditService.record(
                user.getId(),
                SecurityAuditEventType.TOTP_DISABLED
        );
    }

    private void requireTotpOrRecoveryCode(
            User user,
            TotpStepUpRequest request
    ) {

        if (request.totpCode() != null && !request.totpCode().isBlank()) {
            totpService.verifyCode(
                    user,
                    request.totpCode(),
                    TotpVerificationPurpose.SENSITIVE_ACTION
            );
            return;
        }

        if (request.recoveryCode() != null
                && !request.recoveryCode().isBlank()) {
            recoveryCodeService.consumeRecoveryCode(
                    user,
                    request.recoveryCode()
            );
            securityAuditService.record(
                    user.getId(),
                    SecurityAuditEventType.RECOVERY_CODE_USED
            );
            return;
        }

        if (!totpService.hasEnabledCredential(user)) {
            requirePassword(user, request.password());
            return;
        }

        throw new AuthenticationFailedException(INVALID_CREDENTIALS);
    }

    private void requirePassword(User user, String password) {

        if (password == null || password.isBlank()) {
            throw new AuthenticationFailedException(INVALID_CREDENTIALS);
        }

        try {
            authenticationManager.authenticate(
                    UsernamePasswordAuthenticationToken.unauthenticated(
                            user.getEmail(),
                            password
                    )
            );
        } catch (AuthenticationException exception) {
            throw new AuthenticationFailedException(INVALID_CREDENTIALS);
        }
    }

    private TotpEnrollmentResponse enrollmentResponse(
            User user,
            TotpProvisioningDetails details
    ) {

        return new TotpEnrollmentResponse(
                totpService.issuer(),
                user.getEmail(),
                details.manualSetupKey(),
                details.otpauthUri()
        );
    }
}
