package com.researchassistant.identity.dto;

import com.researchassistant.identity.entity.AuthenticationMethod;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Request body for interactive login.
 *
 * <p>The authentication method controls which credential fields
 * are required. Omitting the method preserves the initial
 * password-login behavior for existing clients.</p>
 *
 * @param email user's registered email address
 * @param authenticationMethod requested authentication method
 * @param password raw password supplied for authentication
 * @param totpCode authenticator-app TOTP code supplied for authentication
 */
public record LoginRequest(

        @NotBlank(message = "Email is required")
        @Email(message = "Email must be valid")
        @Size(max = 255, message = "Email must not exceed 255 characters")
        String email,

        AuthenticationMethod authenticationMethod,

        @Size(max = 128, message = "Password must not exceed 128 characters")
        String password,

        @Pattern(
                regexp = "^\\d{6}$",
                message = "TOTP code must contain exactly 6 digits"
        )
        String totpCode,

        @Size(max = 64, message = "Recovery code must not exceed 64 characters")
        String recoveryCode
) {

    public AuthenticationMethod requestedAuthenticationMethod() {
        return authenticationMethod == null
                ? AuthenticationMethod.PASSWORD
                : authenticationMethod;
    }

    @AssertTrue(message = "Password is required for this authentication method")
    public boolean isPasswordValidForAuthenticationMethod() {

        AuthenticationMethod method = requestedAuthenticationMethod();

        if (method == AuthenticationMethod.PASSWORD
                || method == AuthenticationMethod.PASSWORD_AND_TOTP) {
            return password != null && !password.isBlank();
        }

        return true;
    }

    @AssertTrue(message = "TOTP code or recovery code is required for this authentication method")
    public boolean isTotpCodeValidForAuthenticationMethod() {

        AuthenticationMethod method = requestedAuthenticationMethod();

        if (method == AuthenticationMethod.TOTP) {
            boolean hasTotp = totpCode != null && !totpCode.isBlank();
            boolean hasRecovery = recoveryCode != null && !recoveryCode.isBlank();
            return hasTotp || hasRecovery;
        }

        if (method == AuthenticationMethod.PASSWORD_AND_TOTP) {
            return (totpCode != null && !totpCode.isBlank()) || (recoveryCode != null && !recoveryCode.isBlank());
        }

        return true;
    }

    @AssertTrue(message = "Either password, TOTP code, or recovery code is required for this authentication method")
    public boolean isCredentialPresentForAlternativeMethod() {

        AuthenticationMethod method = requestedAuthenticationMethod();

        if (method == AuthenticationMethod.PASSWORD_OR_TOTP) {
            boolean hasPassword = password != null && !password.isBlank();
            boolean hasTotp = totpCode != null && !totpCode.isBlank();
            boolean hasRecovery = recoveryCode != null && !recoveryCode.isBlank();
            return hasPassword || hasTotp || hasRecovery;
        }

        return true;
    }
}
