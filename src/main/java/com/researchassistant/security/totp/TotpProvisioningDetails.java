package com.researchassistant.security.totp;

/**
 * Initial authenticator-app provisioning details.
 *
 * <p>The manual setup key is a secret and must only be returned
 * during an explicit enrollment flow before confirmation. It must
 * never appear in ordinary account responses or JWTs.</p>
 *
 * @param manualSetupKey Base32 TOTP secret for manual authenticator setup
 * @param otpauthUri standard otpauth URI suitable for QR-code generation
 */
public record TotpProvisioningDetails(
        String manualSetupKey,
        String otpauthUri
) {
}
