package com.researchassistant.identity.service;

import com.researchassistant.common.exception.AuthenticationFailedException;
import com.researchassistant.identity.entity.User;

import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class TotpLoginChallengeService {
    private static final Duration TTL = Duration.ofMinutes(5);
    private static final String INVALID_CREDENTIALS = "Invalid credentials.";

    private final Clock clock;
    private final Map<String, Challenge> challenges = new ConcurrentHashMap<>();

    public TotpLoginChallengeService(Clock clock) {
        this.clock = clock;
    }

    public CreatedChallenge create(User user) {
        cleanupExpired();
        String id = UUID.randomUUID().toString();
        challenges.put(id, new Challenge(user.getId(), clock.instant().plus(TTL)));
        return new CreatedChallenge(id, TTL.toSeconds());
    }

    public UUID consume(String challengeId) {
        Challenge challenge = challenges.remove(challengeId);
        if (challenge == null || !challenge.expiresAt().isAfter(clock.instant())) {
            throw new AuthenticationFailedException(INVALID_CREDENTIALS);
        }
        return challenge.userId();
    }

    private void cleanupExpired() {
        Instant now = clock.instant();
        challenges.entrySet().removeIf(entry -> !entry.getValue().expiresAt().isAfter(now));
    }

    private record Challenge(UUID userId, Instant expiresAt) {
    }

    public record CreatedChallenge(String challengeId, long expiresInSeconds) {
    }
}
