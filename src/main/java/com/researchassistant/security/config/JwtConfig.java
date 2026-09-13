package com.researchassistant.security.config;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.Base64;

/**
 * Configures JWT signing and verification for the
 * AI Research Assistant API.
 *
 * <p>The signing secret is supplied externally through the
 * {@code JWT_SECRET} environment variable. Externalizing the
 * secret keeps deploy-time credentials out of source control and
 * allows each environment to rotate keys independently.</p>
 *
 * <p>The API currently uses HMAC SHA-256 for signing access
 * tokens. HS256 keeps the first authentication architecture
 * simple while this service is both token issuer and verifier.
 * The design can later be upgraded to asymmetric signing if
 * authentication is separated into its own service.</p>
 */
@Configuration
public class JwtConfig {

    /**
     * Creates the cryptographic key used for JWT signing
     * and validation.
     *
     * @param base64Secret Base64 encoded secret from configuration
     * @return HMAC SHA-256 secret key
     */
    @Bean
    public SecretKey jwtSecretKey(
            @Value("${app.security.jwt.secret}") String base64Secret
    ) {

        byte[] secretBytes;

        try {
            secretBytes = Base64.getDecoder().decode(base64Secret);
        } catch (IllegalArgumentException exception) {
            throw new IllegalStateException(
                    "JWT secret must be valid Base64.",
                    exception
            );
        }

        if (secretBytes.length < 32) {
            throw new IllegalStateException(
                    "JWT secret must decode to at least 256 bits."
            );
        }

        return new SecretKeySpec(
                secretBytes,
                "HmacSHA256"
        );
    }


    /**
     * Creates the encoder responsible for issuing signed JWTs.
     */
    @Bean
    public JwtEncoder jwtEncoder(SecretKey jwtSecretKey) {

        return NimbusJwtEncoder
                .withSecretKey(jwtSecretKey)
                .algorithm(MacAlgorithm.HS256)
                .build();
    }


    /**
     * Creates the decoder responsible for validating incoming
     * JWT Bearer tokens.
     */
    @Bean
    public JwtDecoder jwtDecoder(
            SecretKey jwtSecretKey,
            @Value("${app.security.jwt.issuer}") String issuer
    ) {

        NimbusJwtDecoder jwtDecoder = NimbusJwtDecoder
                .withSecretKey(jwtSecretKey)
                .macAlgorithm(MacAlgorithm.HS256)
                .build();

        OAuth2TokenValidator<Jwt> validator =
                JwtValidators.createDefaultWithIssuer(
                        issuer
                );

        jwtDecoder.setJwtValidator(validator);

        return jwtDecoder;
    }
}
