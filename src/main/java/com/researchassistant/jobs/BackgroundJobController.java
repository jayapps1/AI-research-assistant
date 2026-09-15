package com.researchassistant.jobs;

import com.researchassistant.identity.entity.User;
import com.researchassistant.identity.service.AuthenticatedUserResolver;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/jobs")
public class BackgroundJobController {
    private final BackgroundJobService service;
    private final AuthenticatedUserResolver userResolver;

    public BackgroundJobController(BackgroundJobService service, AuthenticatedUserResolver userResolver) {
        this.service = service;
        this.userResolver = userResolver;
    }

    @GetMapping("/{jobId}")
    public BackgroundJobResponse get(@PathVariable UUID jobId, Authentication authentication) {
        User user = userResolver.requireActiveUser(authentication);
        BackgroundJob job = service.getAuthorized(jobId, user);
        return new BackgroundJobResponse(job.getId(), job.getType(), job.getStatus(), job.getProgressPercent(),
                job.getFailureCode(), job.getFailureMessageSafe(), job.getCreatedAt(), job.getStartedAt(), job.getCompletedAt());
    }

    public record BackgroundJobResponse(UUID id, BackgroundJobType type, BackgroundJobStatus status, Integer progressPercent,
                                        String failureCode, String failureMessageSafe, OffsetDateTime createdAt,
                                        OffsetDateTime startedAt, OffsetDateTime completedAt) {}
}
