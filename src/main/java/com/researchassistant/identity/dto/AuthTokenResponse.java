package com.researchassistant.identity.dto;

/**
 * Token response returned after login or refresh.
 *
 * @param accessToken short-lived JWT access token
 * @param refreshToken opaque refresh token returned only once
 * @param tokenType authorization scheme for the access token
 * @param expiresIn access-token lifetime in seconds
 */
public record AuthTokenResponse(
        String accessToken,
        String refreshToken,
        String tokenType,
        long expiresIn
) {
}
