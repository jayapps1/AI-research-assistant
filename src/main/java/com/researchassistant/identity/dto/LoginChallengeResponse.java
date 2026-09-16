package com.researchassistant.identity.dto;

/**
 * Returned after password verification when an enrolled authenticator
 * must complete sign-in.
 */
public record LoginChallengeResponse(
        String status,
        String challengeId,
        String authenticationMethod,
        long expiresIn
) {
}
