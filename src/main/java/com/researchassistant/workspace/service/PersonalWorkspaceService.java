package com.researchassistant.workspace.service;

import com.researchassistant.identity.entity.User;
import com.researchassistant.security.audit.SecurityAuditEventType;
import com.researchassistant.security.audit.SecurityAuditService;
import com.researchassistant.subscription.EntitlementService;
import com.researchassistant.workspace.dto.WorkspaceResponse;
import com.researchassistant.workspace.entity.Workspace;
import com.researchassistant.workspace.entity.WorkspaceMembership;
import com.researchassistant.workspace.entity.WorkspaceMembershipStatus;
import com.researchassistant.workspace.entity.WorkspaceRole;
import com.researchassistant.workspace.entity.WorkspaceStatus;
import com.researchassistant.workspace.entity.WorkspaceType;
import com.researchassistant.workspace.repository.WorkspaceMembershipRepository;
import com.researchassistant.workspace.repository.WorkspaceRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
public class PersonalWorkspaceService {

    private static final Logger log = LoggerFactory.getLogger(PersonalWorkspaceService.class);

    private final WorkspaceRepository workspaceRepository;
    private final WorkspaceMembershipRepository membershipRepository;
    private final EntitlementService entitlementService;
    private final SecurityAuditService auditService;
    private final com.researchassistant.identity.repository.UserRepository userRepository;

    public PersonalWorkspaceService(
            WorkspaceRepository workspaceRepository,
            WorkspaceMembershipRepository membershipRepository,
            EntitlementService entitlementService,
            SecurityAuditService auditService,
            com.researchassistant.identity.repository.UserRepository userRepository
    ) {
        this.workspaceRepository = workspaceRepository;
        this.membershipRepository = membershipRepository;
        this.entitlementService = entitlementService;
        this.auditService = auditService;
        this.userRepository = userRepository;
    }

    /**
     * Idempotently ensures that an active PERSONAL workspace exists for the specified user.
     * If one already exists, it is returned; otherwise, exactly one is created with an
     * ACTIVE OWNER membership and free subscription entitlement.
     *
     * @param user the authenticated or newly created user
     * @return the active personal workspace entity
     */
    public Workspace ensurePersonalWorkspace(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new com.researchassistant.common.exception.ResourceNotFoundException("User not found: " + userId));
        return ensurePersonalWorkspace(user);
    }

    public Workspace ensurePersonalWorkspace(User user) {
        Optional<Workspace> existing = workspaceRepository.findFirstByOwnerIdAndTypeAndStatus(
                user.getId(),
                WorkspaceType.PERSONAL,
                WorkspaceStatus.ACTIVE
        );

        if (existing.isPresent()) {
            Workspace workspace = existing.get();
            ensureOwnerMembership(workspace, user);
            try {
                entitlementService.ensureFreeSubscription(workspace.getId());
            } catch (Exception ex) {
                log.warn("Could not ensure free subscription for existing personal workspace {}: {}", workspace.getId(), ex.getMessage());
            }
            return workspace;
        }

        // Build a friendly name
        String name = resolveWorkspaceName(user);

        Workspace workspace = new Workspace();
        workspace.setName(name);
        workspace.setType(WorkspaceType.PERSONAL);
        workspace.setOwner(user);
        workspace.setStatus(WorkspaceStatus.ACTIVE);

        Workspace saved = workspaceRepository.save(workspace);

        WorkspaceMembership membership = new WorkspaceMembership();
        membership.setWorkspace(saved);
        membership.setUser(user);
        membership.setRole(WorkspaceRole.OWNER);
        membership.setStatus(WorkspaceMembershipStatus.ACTIVE);
        membership.setJoinedAt(OffsetDateTime.now());
        membershipRepository.save(membership);

        try {
            entitlementService.ensureFreeSubscription(saved.getId());
        } catch (Exception ex) {
            log.warn("Could not initialize free subscription for personal workspace {}: {}", saved.getId(), ex.getMessage());
        }

        auditService.record(user.getId(), SecurityAuditEventType.WORKSPACE_CREATED);
        log.info("Ensured personal workspace {} for user {}", saved.getId(), user.getEmail());

        return saved;
    }

    public WorkspaceResponse ensurePersonalWorkspaceResponse(User user) {
        Workspace workspace = ensurePersonalWorkspace(user);
        WorkspaceMembership membership = membershipRepository
                .findByWorkspaceIdAndUserIdAndStatus(workspace.getId(), user.getId(), WorkspaceMembershipStatus.ACTIVE)
                .orElseGet(() -> {
                    WorkspaceMembership m = new WorkspaceMembership();
                    m.setWorkspace(workspace);
                    m.setUser(user);
                    m.setRole(WorkspaceRole.OWNER);
                    m.setStatus(WorkspaceMembershipStatus.ACTIVE);
                    m.setJoinedAt(OffsetDateTime.now());
                    return membershipRepository.save(m);
                });

        return new WorkspaceResponse(
                workspace.getId(),
                workspace.getName(),
                workspace.getType(),
                workspace.getStatus(),
                workspace.getOwner().getId(),
                membership.getRole(),
                workspace.getCreatedAt(),
                workspace.getUpdatedAt()
        );
    }

    private void ensureOwnerMembership(Workspace workspace, User user) {
        Optional<WorkspaceMembership> membershipOpt = membershipRepository
                .findByWorkspaceIdAndUserId(workspace.getId(), user.getId());

        if (membershipOpt.isEmpty()) {
            WorkspaceMembership membership = new WorkspaceMembership();
            membership.setWorkspace(workspace);
            membership.setUser(user);
            membership.setRole(WorkspaceRole.OWNER);
            membership.setStatus(WorkspaceMembershipStatus.ACTIVE);
            membership.setJoinedAt(OffsetDateTime.now());
            membershipRepository.save(membership);
        } else {
            WorkspaceMembership m = membershipOpt.get();
            if (m.getStatus() != WorkspaceMembershipStatus.ACTIVE || m.getRole() != WorkspaceRole.OWNER) {
                m.setStatus(WorkspaceMembershipStatus.ACTIVE);
                m.setRole(WorkspaceRole.OWNER);
                membershipRepository.save(m);
            }
        }
    }

    private String resolveWorkspaceName(User user) {
        String firstName = user.getFirstName();
        if (firstName != null && !firstName.isBlank()) {
            return firstName.trim() + "'s Workspace";
        }
        return "My Workspace";
    }
}
