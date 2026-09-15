package com.researchassistant.jobs;

import com.researchassistant.common.exception.ResourceNotFoundException;
import com.researchassistant.identity.entity.User;
import com.researchassistant.project.service.ProjectAuthorizationService;
import com.researchassistant.workspace.service.WorkspaceAuthorizationService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
public class BackgroundJobService {
    private final BackgroundJobRepository repository;
    private final WorkspaceAuthorizationService workspaceAuthorizationService;
    private final ProjectAuthorizationService projectAuthorizationService;

    public BackgroundJobService(BackgroundJobRepository repository,
                                WorkspaceAuthorizationService workspaceAuthorizationService,
                                ProjectAuthorizationService projectAuthorizationService) {
        this.repository = repository;
        this.workspaceAuthorizationService = workspaceAuthorizationService;
        this.projectAuthorizationService = projectAuthorizationService;
    }

    public BackgroundJob enqueue(BackgroundJob job) {
        job.setStatus(BackgroundJobStatus.QUEUED);
        return repository.save(job);
    }

    public Optional<BackgroundJob> claimNext() {
        Optional<BackgroundJob> claimed = repository.claimNextForUpdateSkipLocked();
        claimed.ifPresent(job -> {
            job.setStatus(BackgroundJobStatus.RUNNING);
            job.setAttempts(job.getAttempts() + 1);
            job.setStartedAt(OffsetDateTime.now());
        });
        return claimed;
    }

    public void fail(BackgroundJob job, boolean transientFailure, String code, String safeMessage) {
        job.setFailureCode(code);
        job.setFailureMessageSafe(safeMessage);
        if (transientFailure && job.getAttempts() < job.getMaxAttempts()) {
            job.setStatus(BackgroundJobStatus.RETRY_SCHEDULED);
            job.setNextAttemptAt(OffsetDateTime.now().plus(backoff(job.getAttempts())));
        } else {
            job.setStatus(BackgroundJobStatus.DEAD_LETTER);
            job.setCompletedAt(OffsetDateTime.now());
        }
    }

    public void succeed(BackgroundJob job) {
        job.setStatus(BackgroundJobStatus.SUCCEEDED);
        job.setProgressPercent(100);
        job.setCompletedAt(OffsetDateTime.now());
    }

    @Transactional(readOnly = true)
    public BackgroundJob getAuthorized(UUID jobId, User user) {
        BackgroundJob job = repository.findById(jobId)
                .orElseThrow(() -> new ResourceNotFoundException("Background job not found."));
        if (job.getProject() != null) {
            projectAuthorizationService.requireProjectViewer(job.getProject().getId(), user);
        } else if (job.getWorkspace() != null) {
            workspaceAuthorizationService.requireActiveMembership(job.getWorkspace().getId(), user);
        } else if (job.getRequestedBy() != null && !job.getRequestedBy().getId().equals(user.getId())) {
            throw new com.researchassistant.workspace.exception.WorkspaceAccessDeniedException("Background job access denied.");
        }
        return job;
    }

    private Duration backoff(int attempts) {
        return Duration.ofMinutes(Math.min(60, Math.max(1, attempts * attempts)));
    }
}
