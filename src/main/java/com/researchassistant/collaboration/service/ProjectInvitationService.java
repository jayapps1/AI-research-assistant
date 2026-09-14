package com.researchassistant.collaboration.service;

import com.researchassistant.cache.CacheInvalidationService;
import com.researchassistant.collaboration.dto.CollaborationDtos.*;
import com.researchassistant.collaboration.entity.ProjectActivityType;
import com.researchassistant.collaboration.entity.ProjectInvitation;
import com.researchassistant.collaboration.entity.ProjectInvitationStatus;
import com.researchassistant.collaboration.repository.ProjectInvitationRepository;
import com.researchassistant.common.exception.DuplicateResourceException;
import com.researchassistant.common.exception.ResourceNotFoundException;
import com.researchassistant.identity.entity.User;
import com.researchassistant.identity.repository.UserRepository;
import com.researchassistant.project.dto.ProjectMemberResponse;
import com.researchassistant.project.entity.*;
import com.researchassistant.project.exception.InvalidProjectOperationException;
import com.researchassistant.project.exception.ResearchProjectNotFoundException;
import com.researchassistant.project.repository.ProjectMembershipRepository;
import com.researchassistant.project.service.ProjectAuthorizationContext;
import com.researchassistant.project.service.ProjectAuthorizationService;
import com.researchassistant.security.audit.SecurityAuditEventType;
import com.researchassistant.security.audit.SecurityAuditService;
import com.researchassistant.workspace.entity.WorkspaceMembershipStatus;
import com.researchassistant.workspace.repository.WorkspaceMembershipRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
@Transactional
public class ProjectInvitationService {
    private final ProjectInvitationRepository invitationRepository;
    private final ProjectMembershipRepository membershipRepository;
    private final UserRepository userRepository;
    private final WorkspaceMembershipRepository workspaceMembershipRepository;
    private final ProjectAuthorizationService authorizationService;
    private final SecurityAuditService auditService;
    private final CacheInvalidationService cacheInvalidationService;
    private final CollaborationProperties properties;
    private final ProjectActivityService activityService;
    private final SecureRandom random = new SecureRandom();

    public ProjectInvitationService(ProjectInvitationRepository invitationRepository, ProjectMembershipRepository membershipRepository, UserRepository userRepository, WorkspaceMembershipRepository workspaceMembershipRepository, ProjectAuthorizationService authorizationService, SecurityAuditService auditService, CacheInvalidationService cacheInvalidationService, CollaborationProperties properties, ProjectActivityService activityService) {
        this.invitationRepository = invitationRepository;
        this.membershipRepository = membershipRepository;
        this.userRepository = userRepository;
        this.workspaceMembershipRepository = workspaceMembershipRepository;
        this.authorizationService = authorizationService;
        this.auditService = auditService;
        this.cacheInvalidationService = cacheInvalidationService;
        this.properties = properties;
        this.activityService = activityService;
    }

    public ProjectInvitationResponse invite(UUID projectId, User actor, CreateProjectInvitationRequest request) {
        ProjectAuthorizationContext context = authorizationService.requireProjectAdminAccess(projectId, actor);
        ProjectRole role = request.role();
        if (role == ProjectRole.LEAD && !authorizationService.isWorkspaceAdmin(context.workspaceMembership())) {
            throw new InvalidProjectOperationException("Only workspace owners or admins may invite a project lead.");
        }
        String email = normalizeEmail(request.email());
        userRepository.findByEmailIgnoreCase(email).ifPresent(user -> {
            if (membershipRepository.existsByProjectIdAndUserIdAndStatus(projectId, user.getId(), ProjectMembershipStatus.ACTIVE)) {
                throw new DuplicateResourceException("User already has active project membership.");
            }
        });
        if (invitationRepository.existsByProjectIdAndInvitedEmailNormalizedAndStatus(projectId, email, ProjectInvitationStatus.PENDING)) {
            throw new DuplicateResourceException("A pending invitation already exists for this project and email.");
        }
        String rawToken = newToken();
        ProjectInvitation invitation = new ProjectInvitation();
        invitation.setProject(context.project());
        invitation.setInvitedEmailNormalized(email);
        invitation.setRole(role);
        invitation.setInvitedBy(actor);
        invitation.setTokenHash(hash(rawToken));
        invitation.setExpiresAt(OffsetDateTime.now().plus(properties.invitationTtl()));
        invitation = invitationRepository.save(invitation);
        auditService.record(actor.getId(), SecurityAuditEventType.PROJECT_INVITATION_CREATED);
        activityService.record(context.project(), actor, ProjectActivityType.MEMBER_INVITED, null, invitation.getId(), "Project member invited.");
        return toInvitation(invitation, rawToken);
    }

    @Transactional(readOnly = true)
    public List<ProjectInvitationResponse> list(UUID projectId, User actor) {
        authorizationService.requireProjectAdminAccess(projectId, actor);
        return invitationRepository.findAllByProjectIdOrderByCreatedAtDesc(projectId).stream()
                .map(invitation -> toInvitation(invitation, null))
                .toList();
    }

    public ProjectInvitationResponse accept(UUID invitationId, User actor) {
        ProjectInvitation invitation = pending(invitationId);
        if (invitation.getExpiresAt().isBefore(OffsetDateTime.now())) {
            invitation.setStatus(ProjectInvitationStatus.EXPIRED);
            throw new InvalidProjectOperationException("Invitation has expired.");
        }
        String actorEmail = normalizeEmail(actor.getEmail());
        if (!actorEmail.equals(invitation.getInvitedEmailNormalized())) {
            throw new InvalidProjectOperationException("Invitation can only be accepted by the invited email account.");
        }
        workspaceMembershipRepository.findByWorkspaceIdAndUserIdAndStatus(invitation.getProject().getWorkspace().getId(), actor.getId(), WorkspaceMembershipStatus.ACTIVE)
                .orElseThrow(() -> new InvalidProjectOperationException("User must belong to the workspace before accepting this project invitation."));

        ProjectMembership membership = membershipRepository.findByProjectIdAndUserId(invitation.getProject().getId(), actor.getId()).orElseGet(ProjectMembership::new);
        membership.setProject(invitation.getProject());
        membership.setUser(actor);
        membership.setRole(invitation.getRole());
        membership.setStatus(ProjectMembershipStatus.ACTIVE);
        membership.setAddedBy(invitation.getInvitedBy());
        membership.setJoinedAt(OffsetDateTime.now());
        membershipRepository.save(membership);

        invitation.setStatus(ProjectInvitationStatus.ACCEPTED);
        invitation.setAcceptedAt(OffsetDateTime.now());
        auditService.record(actor.getId(), SecurityAuditEventType.PROJECT_INVITATION_ACCEPTED);
        activityService.record(invitation.getProject(), actor, ProjectActivityType.MEMBER_JOINED, null, membership.getId(), "Project member joined.");
        cacheInvalidationService.evictProjectMetadata(invitation.getProject().getId());
        return toInvitation(invitation, null);
    }

    public ProjectInvitationResponse decline(UUID invitationId, User actor) {
        ProjectInvitation invitation = pending(invitationId);
        if (!normalizeEmail(actor.getEmail()).equals(invitation.getInvitedEmailNormalized())) {
            throw new InvalidProjectOperationException("Invitation can only be declined by the invited email account.");
        }
        invitation.setStatus(ProjectInvitationStatus.DECLINED);
        invitation.setDeclinedAt(OffsetDateTime.now());
        auditService.record(actor.getId(), SecurityAuditEventType.PROJECT_INVITATION_DECLINED);
        return toInvitation(invitation, null);
    }

    public ProjectInvitationResponse revoke(UUID invitationId, User actor) {
        ProjectInvitation invitation = invitationRepository.findById(invitationId).orElseThrow(() -> new ResourceNotFoundException("Invitation not found."));
        authorizationService.requireProjectAdminAccess(invitation.getProject().getId(), actor);
        if (invitation.getStatus() != ProjectInvitationStatus.PENDING) {
            throw new InvalidProjectOperationException("Only pending invitations can be revoked.");
        }
        invitation.setStatus(ProjectInvitationStatus.REVOKED);
        invitation.setRevokedAt(OffsetDateTime.now());
        auditService.record(actor.getId(), SecurityAuditEventType.PROJECT_INVITATION_REVOKED);
        activityService.record(invitation.getProject(), actor, ProjectActivityType.MEMBER_REMOVED, null, invitation.getId(), "Project invitation revoked.");
        return toInvitation(invitation, null);
    }

    public ProjectInvitationResponse resend(UUID invitationId, User actor) {
        ProjectInvitation invitation = invitationRepository.findById(invitationId).orElseThrow(() -> new ResourceNotFoundException("Invitation not found."));
        authorizationService.requireProjectAdminAccess(invitation.getProject().getId(), actor);
        if (invitation.getStatus() != ProjectInvitationStatus.PENDING) {
            throw new InvalidProjectOperationException("Only pending invitations can be resent.");
        }
        String rawToken = newToken();
        invitation.setTokenHash(hash(rawToken));
        invitation.setExpiresAt(OffsetDateTime.now().plus(properties.invitationTtl()));
        return toInvitation(invitation, rawToken);
    }

    public ProjectMemberResponse suspend(UUID projectId, UUID memberId, User actor) {
        ProjectAuthorizationContext context = authorizationService.requireProjectAdminAccess(projectId, actor);
        ProjectMembership membership = membership(memberId, projectId);
        membershipRepository.findActiveByProjectIdForUpdate(projectId);
        if (membership.getRole() == ProjectRole.LEAD && activeLeadCount(projectId) <= 1) {
            throw new InvalidProjectOperationException("Project must retain at least one active lead.");
        }
        membership.setStatus(ProjectMembershipStatus.SUSPENDED);
        auditService.record(actor.getId(), SecurityAuditEventType.PROJECT_MEMBER_SUSPENDED);
        activityService.record(context.project(), actor, ProjectActivityType.MEMBER_SUSPENDED, null, memberId, "Project member suspended.");
        cacheInvalidationService.evictProjectMetadata(projectId);
        return toMember(membership);
    }

    public ProjectMemberResponse changeRole(UUID projectId, UUID memberId, User actor, ChangeMemberRoleRequest request) {
        ProjectAuthorizationContext context = authorizationService.requireProjectAdminAccess(projectId, actor);
        ProjectMembership membership = membership(memberId, projectId);
        membershipRepository.findActiveByProjectIdForUpdate(projectId);
        if (membership.getRole() == ProjectRole.LEAD && request.role() != ProjectRole.LEAD && activeLeadCount(projectId) <= 1) {
            throw new InvalidProjectOperationException("Project must retain at least one active lead.");
        }
        if (request.role() == ProjectRole.LEAD && !authorizationService.isWorkspaceAdmin(context.workspaceMembership())) {
            throw new InvalidProjectOperationException("Only workspace owners or admins may assign project lead.");
        }
        membership.setRole(request.role());
        auditService.record(actor.getId(), SecurityAuditEventType.PROJECT_MEMBER_ROLE_CHANGED);
        activityService.record(context.project(), actor, ProjectActivityType.MEMBER_ROLE_CHANGED, null, memberId, "Project member role changed.");
        cacheInvalidationService.evictProjectMetadata(projectId);
        return toMember(membership);
    }

    public void remove(UUID projectId, UUID memberId, User actor) {
        ProjectAuthorizationContext context = authorizationService.requireProjectAdminAccess(projectId, actor);
        ProjectMembership membership = membership(memberId, projectId);
        membershipRepository.findActiveByProjectIdForUpdate(projectId);
        if (membership.getRole() == ProjectRole.LEAD && activeLeadCount(projectId) <= 1) {
            throw new InvalidProjectOperationException("Project must retain at least one active lead.");
        }
        membership.setStatus(ProjectMembershipStatus.REMOVED);
        auditService.record(actor.getId(), SecurityAuditEventType.PROJECT_MEMBER_REMOVED);
        activityService.record(context.project(), actor, ProjectActivityType.MEMBER_REMOVED, null, memberId, "Project member removed.");
        cacheInvalidationService.evictProjectMetadata(projectId);
    }

    public ProjectMemberResponse reactivate(UUID projectId, UUID memberId, User actor) {
        ProjectAuthorizationContext context = authorizationService.requireProjectAdminAccess(projectId, actor);
        ProjectMembership membership = membershipRepository.findById(memberId).filter(m -> m.getProject().getId().equals(projectId)).orElseThrow(ResearchProjectNotFoundException::new);
        if (membership.getStatus() == ProjectMembershipStatus.REMOVED) {
            throw new InvalidProjectOperationException("Removed project members cannot be reactivated directly.");
        }
        membership.setStatus(ProjectMembershipStatus.ACTIVE);
        auditService.record(actor.getId(), SecurityAuditEventType.PROJECT_MEMBER_REACTIVATED);
        activityService.record(context.project(), actor, ProjectActivityType.MEMBER_REACTIVATED, null, memberId, "Project member reactivated.");
        cacheInvalidationService.evictProjectMetadata(projectId);
        return toMember(membership);
    }

    public ProjectMemberResponse transferLead(UUID projectId, UUID memberId, User actor, TransferLeadRequest request) {
        ProjectAuthorizationContext context = authorizationService.requireProjectAdminAccess(projectId, actor);
        ProjectMembership target = membership(memberId, projectId);
        membershipRepository.findActiveByProjectIdForUpdate(projectId);
        target.setRole(ProjectRole.LEAD);
        if (request.demoteCurrentLead()) {
            ProjectMembership current = context.projectMembership().orElse(null);
            if (current != null && !current.getId().equals(target.getId())) {
                current.setRole(ProjectRole.EDITOR);
            }
        }
        auditService.record(actor.getId(), SecurityAuditEventType.PROJECT_MEMBER_ROLE_CHANGED);
        activityService.record(context.project(), actor, ProjectActivityType.MEMBER_ROLE_CHANGED, null, memberId, "Project lead transferred.");
        cacheInvalidationService.evictProjectMetadata(projectId);
        return toMember(target);
    }

    private ProjectInvitation pending(UUID invitationId) {
        return invitationRepository.findByIdAndStatus(invitationId, ProjectInvitationStatus.PENDING)
                .orElseThrow(() -> new InvalidProjectOperationException("Pending invitation not found."));
    }

    private ProjectMembership membership(UUID memberId, UUID projectId) {
        return membershipRepository.findByIdAndProjectIdAndStatus(memberId, projectId, ProjectMembershipStatus.ACTIVE)
                .orElseThrow(ResearchProjectNotFoundException::new);
    }

    private long activeLeadCount(UUID projectId) {
        return membershipRepository.countByProjectIdAndRoleAndStatus(projectId, ProjectRole.LEAD, ProjectMembershipStatus.ACTIVE);
    }

    private String newToken() {
        byte[] token = new byte[32];
        random.nextBytes(token);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(token);
    }

    private String hash(String token) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            throw new IllegalStateException("Invitation token hashing failed.", exception);
        }
    }

    private String normalizeEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
    }

    private ProjectInvitationResponse toInvitation(ProjectInvitation invitation, String deliveryToken) {
        return new ProjectInvitationResponse(invitation.getId(), invitation.getProject().getId(), invitation.getInvitedEmailNormalized(), invitation.getRole(), invitation.getStatus(), invitation.getInvitedBy().getId(), invitation.getCreatedAt(), invitation.getExpiresAt(), invitation.getAcceptedAt(), invitation.getDeclinedAt(), invitation.getRevokedAt(), deliveryToken);
    }

    private ProjectMemberResponse toMember(ProjectMembership membership) {
        User user = membership.getUser();
        return new ProjectMemberResponse(user.getId(), user.getEmail(), user.getFirstName(), user.getLastName(), membership.getRole(), membership.getStatus(), membership.getJoinedAt());
    }
}
