package com.researchassistant.security.jwt;

import com.researchassistant.identity.entity.User;
import com.researchassistant.security.service.AuthenticatedUser;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

/**
 * Issues short-lived JWT access tokens.
 *
 * <p>Access tokens are intentionally short lived so a stolen token
 * has a limited window of usefulness. Longer-lived continuity is
 * handled through server-side refresh-session rotation.</p>
 */
@Service
public class JwtTokenService {

    private final JwtEncoder jwtEncoder;
    private final String issuer;
    private final long accessTokenMinutes;

    public JwtTokenService(
            JwtEncoder jwtEncoder,
            @Value("${app.security.jwt.issuer}") String issuer,
            @Value("${app.security.jwt.access-token-minutes}")
            long accessTokenMinutes
    ) {
        this.jwtEncoder = jwtEncoder;
        this.issuer = issuer;
        this.accessTokenMinutes = accessTokenMinutes;
    }

    public IssuedAccessToken issueAccessToken(
            AuthenticatedUser authenticatedUser
    ) {

        return issueAccessToken(
                authenticatedUser.getUserId(),
                authenticatedUser.getEmail()
        );
    }

    public IssuedAccessToken issueAccessToken(User user) {
        return issueAccessToken(user.getId(), user.getEmail());
    }

    private IssuedAccessToken issueAccessToken(UUID userId, String email) {

        Instant issuedAt = Instant.now();
        Instant expiresAt = issuedAt.plusSeconds(expiresInSeconds());

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(issuer)
                .subject(userId.toString())
                .issuedAt(issuedAt)
                .expiresAt(expiresAt)
                .id(UUID.randomUUID().toString())
                .claim("email", email)
                .build();

        JwsHeader header = JwsHeader
                .with(MacAlgorithm.HS256)
                .build();

        String tokenValue = jwtEncoder
                .encode(JwtEncoderParameters.from(header, claims))
                .getTokenValue();

        return new IssuedAccessToken(tokenValue, expiresInSeconds());
    }

    public long expiresInSeconds() {
        return accessTokenMinutes * 60;
    }

    public record IssuedAccessToken(
            String tokenValue,
            long expiresInSeconds
    ) {
    }
}
