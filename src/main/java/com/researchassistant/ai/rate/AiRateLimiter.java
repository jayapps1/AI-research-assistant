package com.researchassistant.ai.rate;

import com.researchassistant.ai.config.AiProperties;
import com.researchassistant.ai.exception.AiRateLimitExceededException;
import com.researchassistant.ai.usage.AiRequestRepository;

import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.UUID;

@Component
public class AiRateLimiter {

    private final AiProperties properties;
    private final AiRequestRepository requestRepository;

    public AiRateLimiter(AiProperties properties, AiRequestRepository requestRepository) {
        this.properties = properties;
        this.requestRepository = requestRepository;
    }

    public void checkRateLimit(UUID userId, UUID workspaceId) {
        OffsetDateTime oneMinuteAgo = OffsetDateTime.now().minusMinutes(1);

        long userRequestsLastMinute = requestRepository.countByUserIdAndCreatedAtAfter(userId, oneMinuteAgo);
        if (userRequestsLastMinute >= properties.rateLimit().userRequestsPerMinute()) {
            throw new AiRateLimitExceededException(
                    "User AI request rate limit exceeded. Please wait before retrying."
            );
        }

        if (workspaceId != null) {
            long workspaceRequestsLastMinute = requestRepository.countByWorkspaceIdAndCreatedAtAfter(workspaceId, oneMinuteAgo);
            if (workspaceRequestsLastMinute >= properties.rateLimit().workspaceRequestsPerMinute()) {
                throw new AiRateLimitExceededException(
                        "Workspace AI request rate limit exceeded. Please wait before retrying."
                );
            }
        }
    }
}
