package com.researchassistant.subscription;

import com.researchassistant.audit.AuditEventService;
import com.researchassistant.audit.AuditEventType;
import com.researchassistant.common.exception.ResourceNotFoundException;
import com.researchassistant.identity.entity.User;
import com.researchassistant.identity.repository.UserRepository;
import com.researchassistant.notification.NotificationPriority;
import com.researchassistant.notification.NotificationService;
import com.researchassistant.notification.NotificationType;
import com.researchassistant.workspace.entity.Workspace;
import com.researchassistant.workspace.repository.WorkspaceRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.UUID;

@Service
@Transactional
public class ComplimentaryAccessService {
    private final ComplimentaryAccessGrantRepository grantRepository;
    private final WorkspaceRepository workspaceRepository;
    private final UserRepository userRepository;
    private final SubscriptionPlanRepository planRepository;
    private final AuditEventService auditEventService;
    private final NotificationService notificationService;

    public ComplimentaryAccessService(ComplimentaryAccessGrantRepository grantRepository,
                                      WorkspaceRepository workspaceRepository,
                                      UserRepository userRepository,
                                      SubscriptionPlanRepository planRepository,
                                      AuditEventService auditEventService,
                                      NotificationService notificationService) {
        this.grantRepository = grantRepository;
        this.workspaceRepository = workspaceRepository;
        this.userRepository = userRepository;
        this.planRepository = planRepository;
        this.auditEventService = auditEventService;
        this.notificationService = notificationService;
    }

    @CacheEvict(cacheNames = "subscription:workspace-entitlements", allEntries = true)
    public ComplimentaryAccessGrant grant(GrantRequest request, User admin) {
        if (request.workspaceId() == null && request.userId() == null) {
            throw new IllegalArgumentException("workspaceId or userId is required.");
        }
        Workspace workspace = request.workspaceId() == null ? null : workspaceRepository.findById(request.workspaceId())
                .orElseThrow(() -> new ResourceNotFoundException("Workspace not found."));
        User user = request.userId() == null ? null : userRepository.findById(request.userId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found."));
        SubscriptionPlan plan = request.planCode() == null ? null : planRepository.findByCodeIgnoreCase(request.planCode())
                .orElseThrow(() -> new ResourceNotFoundException("Subscription plan not found."));
        ComplimentaryAccessGrant grant = new ComplimentaryAccessGrant();
        grant.setScope(workspace == null ? ComplimentaryGrantScope.USER : ComplimentaryGrantScope.WORKSPACE);
        grant.setWorkspace(workspace);
        grant.setUser(user);
        grant.setPlan(plan);
        grant.setType(request.type() == null ? ComplimentaryAccessType.OTHER : request.type());
        grant.setStatus(statusFor(request.startsAt(), request.expiresAt()));
        grant.setReason(bound(request.reason(), 1000));
        grant.setGrantedBy(admin);
        grant.setStartsAt(request.startsAt() == null ? OffsetDateTime.now() : request.startsAt());
        grant.setExpiresAt(request.expiresAt());
        ComplimentaryAccessGrant saved = grantRepository.save(grant);
        auditEventService.record(admin.getId(), "USER", workspace, null, AuditEventType.COMPLIMENTARY_ACCESS_GRANTED,
                "ComplimentaryAccessGrant", saved.getId(), "{}");
        User recipient = user != null ? user : workspace == null ? null : workspace.getOwner();
        if (recipient != null) {
            notificationService.create(recipient, workspace, null, NotificationType.COMPLIMENTARY_ACCESS_GRANTED,
                    "Complimentary access granted", "Complimentary access has been granted.", null, NotificationPriority.NORMAL);
        }
        return saved;
    }

    @CacheEvict(cacheNames = "subscription:workspace-entitlements", allEntries = true)
    public ComplimentaryAccessGrant revoke(UUID grantId, String reason, User admin) {
        ComplimentaryAccessGrant grant = grantRepository.findById(grantId)
                .orElseThrow(() -> new ResourceNotFoundException("Complimentary access grant not found."));
        grant.setStatus(ComplimentaryAccessStatus.REVOKED);
        grant.setRevokedAt(OffsetDateTime.now());
        grant.setRevokedBy(admin);
        grant.setRevocationReason(bound(reason, 1000));
        auditEventService.record(admin.getId(), "USER", grant.getWorkspace(), null, AuditEventType.COMPLIMENTARY_ACCESS_REVOKED,
                "ComplimentaryAccessGrant", grant.getId(), "{}");
        User recipient = grant.getUser() != null ? grant.getUser() : grant.getWorkspace() == null ? null : grant.getWorkspace().getOwner();
        if (recipient != null) {
            notificationService.create(recipient, grant.getWorkspace(), null, NotificationType.COMPLIMENTARY_ACCESS_REVOKED,
                    "Complimentary access revoked", "Complimentary access has been revoked.", null, NotificationPriority.NORMAL);
        }
        return grant;
    }

    @Transactional(readOnly = true)
    public Page<ComplimentaryAccessGrant> list(Pageable pageable) {
        return grantRepository.findAllByOrderByCreatedAtDesc(pageable);
    }

    @Transactional(readOnly = true)
    public ComplimentaryAccessGrant get(UUID grantId) {
        return grantRepository.findById(grantId)
                .orElseThrow(() -> new ResourceNotFoundException("Complimentary access grant not found."));
    }

    private ComplimentaryAccessStatus statusFor(OffsetDateTime startsAt, OffsetDateTime expiresAt) {
        OffsetDateTime now = OffsetDateTime.now();
        if (startsAt != null && startsAt.isAfter(now)) return ComplimentaryAccessStatus.SCHEDULED;
        if (expiresAt != null && !expiresAt.isAfter(now)) return ComplimentaryAccessStatus.EXPIRED;
        return ComplimentaryAccessStatus.ACTIVE;
    }

    private String bound(String value, int max) {
        String normalized = value == null ? "" : value.trim();
        if (normalized.isBlank()) throw new IllegalArgumentException("reason is required.");
        return normalized.length() <= max ? normalized : normalized.substring(0, max);
    }

    public record GrantRequest(UUID workspaceId, UUID userId, String planCode, ComplimentaryAccessType type,
                               String reason, OffsetDateTime startsAt, OffsetDateTime expiresAt) {}
}
