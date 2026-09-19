package com.researchassistant;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;

import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.UUID;

/**
 * Shared base for integration tests that need real JWT Bearer tokens.
 *
 * <p>The application uses HMAC-SHA256 JWTs; {@link #bearerToken(UUID, String)}
 * generates a valid short-lived token signed with the same key that is
 * supplied to {@code @SpringBootTest(properties = {…})} in subclasses.</p>
 *
 * <p>The test JWT secret must be the same Base64-encoded value that is
 * passed as {@code app.security.jwt.secret} in the test's
 * {@code @SpringBootTest} properties block.</p>
 */
public abstract class IntegrationTestSupport {

    /**
     * Base64-encoded HMAC-SHA256 secret used by all integration tests.
     * This must match the value supplied via
     * {@code @SpringBootTest(properties = {"app.security.jwt.secret=…"})}.
     */
    protected static final String TEST_JWT_SECRET_BASE64 =
            "MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=";

    /**
     * The JWT issuer that the decoder validates.
     * Must match {@code app.security.jwt.issuer}.
     */
    protected static final String JWT_ISSUER = "research-assistant-api";

    /**
     * Generates a signed JWT bearer token for use in MockMvc requests.
     *
     * @param userId the user's UUID (placed in the {@code sub} claim)
     * @param email  the user's email (placed in the {@code email} claim)
     * @return a {@code Bearer <token>} string ready for the
     *         {@code Authorization} header
     */
    protected String bearerToken(UUID userId, String email) {
        try {
            byte[] secretBytes = Base64.getDecoder().decode(TEST_JWT_SECRET_BASE64);
            JWSSigner signer = new MACSigner(secretBytes);

            Instant now = Instant.now();
            JWTClaimsSet claims = new JWTClaimsSet.Builder()
                    .issuer(JWT_ISSUER)
                    .subject(userId.toString())
                    .issueTime(Date.from(now))
                    .expirationTime(Date.from(now.plusSeconds(900)))
                    .jwtID(UUID.randomUUID().toString())
                    .claim("email", email)
                    .build();

            SignedJWT signedJWT = new SignedJWT(
                    new JWSHeader(JWSAlgorithm.HS256),
                    claims
            );
            signedJWT.sign(signer);

            return "Bearer " + signedJWT.serialize();

        } catch (Exception e) {
            throw new RuntimeException("Failed to generate test JWT", e);
        }
    }
}
