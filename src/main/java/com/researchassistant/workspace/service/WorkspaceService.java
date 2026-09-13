package com.researchassistant.workspace.service;

import com.researchassistant.common.exception.DuplicateResourceException;
import com.researchassistant.common.exception.ResourceNotFoundException;
import com.researchassistant.identity.entity.User;
import com.researchassistant.identity.repository.UserRepository;
import com.researchassistant.security.audit.SecurityAuditEventType;
import com.researchassistant.security.audit.SecurityAuditService;
import com.researchassistant.workspace.dto.AddWorkspaceMemberRequest;
import com.researchassistant.workspace.dto.ChangeWorkspaceMemberRoleRequest;
import com.researchassistant.workspace.dto.CreateWorkspaceRequest;
import com.researchassistant.workspace.dto.UpdateWorkspaceRequest;
import com.researchassistant.workspace.dto.WorkspaceMemberResponse;
import com.researchassistant.workspace.dto.WorkspaceResponse;
import com.researchassistant.workspace.entity.Workspace;
import com.researchassistant.workspace.entity.WorkspaceMembership;
import com.researchassistant.workspace.entity.WorkspaceMembershipStatus;
import com.researchassistant.workspace.entity.WorkspaceRole;
import com.researchassistant.workspace.entity.WorkspaceStatus;
import com.researchassistant.workspace.exception.InvalidWorkspaceOperationException;
import com.researchassistant.workspace.exception.WorkspaceNotFoundException;
import com.researchassistant.workspace.repository.WorkspaceMembershipRepository;
import com.researchassistant.workspace.repository.WorkspaceRepository;

import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
@Transactional
public class WorkspaceService {

    private final WorkspaceRepository workspaceRepository;
    private final WorkspaceMembershipRepository membershipRepository;
    private final UserRepository userRepository;
    private final WorkspaceAuthorizationService authorizationService;
    private final SecurityAuditService auditService;

    public WorkspaceService(
            WorkspaceRepository workspaceRepository,
            WorkspaceMembershipRepository membershipRepository,
            UserRepository userRepository,
            WorkspaceAuthorizationService authorizationService,
            SecurityAuditService auditService
    ) {
        this.workspaceRepository = workspaceRepository;
        this.membershipRepository = membershipRepository;
        this.userRepository = userRepository;
        this.authorizationService = authorizationService;
        this.auditService = auditService;
    }

    public WorkspaceResponse createWorkspace(
            User currentUser,
            CreateWorkspaceRequest request
    ) {
        Workspace workspace = new Workspace();
        workspace.setName(normalizeRequiredName(request.name()));
        workspace.setType(request.type());
        workspace.setOwner(currentUser);
        workspace.setStatus(WorkspaceStatus.ACTIVE);

        Workspace savedWorkspace = workspaceRepository.save(workspace);

        WorkspaceMembership membership = new WorkspaceMembership();
        membership.setWorkspace(savedWorkspace);
        membership.setUser(currentUser);
        membership.setRole(WorkspaceRole.OWNER);
        membership.setStatus(WorkspaceMembershipStatus.ACTIVE);
        membership.setJoinedAt(OffsetDateTime.now());
        membershipRepository.save(membership);

        auditService.record(currentUser.getId(), SecurityAuditEventType.WORKSPACE_CREATED);

        return toWorkspaceResponse(savedWorkspace, membership);
    }

    @Transactional(readOnly = true)
    public List<WorkspaceResponse> listWorkspaces(User currentUser) {
        return membershipRepository
                .findAllByUserIdAndStatus(
                        currentUser.getId(),
                        WorkspaceMembershipStatus.ACTIVE
                )
                .stream()
                .map(membership ->
                        toWorkspaceResponse(
                                membership.getWorkspace(),
                                membership
                        )
                )
                .toList();
    }

    @Transactional(readOnly = true)
    public WorkspaceResponse getWorkspace(UUID workspaceId, User currentUser) {
        WorkspaceMembership membership =
                authorizationService.requireActiveMembership(
                        workspaceId,
                        currentUser
                );
        return toWorkspaceResponse(membership.getWorkspace(), membership);
    }

    public WorkspaceResponse updateWorkspace(
            UUID workspaceId,
            User currentUser,
            UpdateWorkspaceRequest request
    ) {
        WorkspaceMembership membership =
                authorizationService.requireAdminOrOwner(
                        workspaceId,
                        currentUser
                );

        Workspace workspace = membership.getWorkspace();

        if (request.name() != null) {
            workspace.setName(normalizeRequiredName(request.name()));
        }

        auditService.record(currentUser.getId(), SecurityAuditEventType.WORKSPACE_UPDATED);

        return toWorkspaceResponse(workspace, membership);
    }

    public WorkspaceResponse archiveWorkspace(
            UUID workspaceId,
            User currentUser
    ) {
        WorkspaceMembership membership =
                authorizationService.requireOwner(workspaceId, currentUser);

        Workspace workspace = membership.getWorkspace();
        workspace.setStatus(WorkspaceStatus.ARCHIVED);

        auditService.record(currentUser.getId(), SecurityAuditEventType.WORKSPACE_ARCHIVED);

        return toWorkspaceResponse(workspace, membership);
    }

    @Transactional(readOnly = true)
    public List<WorkspaceMemberResponse> listMembers(
            UUID workspaceId,
            User currentUser,
            Pageable pageable
    ) {
        authorizationService.requireActiveMembership(workspaceId, currentUser);

        return membershipRepository
                .findAllByWorkspaceIdAndStatus(
                        workspaceId,
                        WorkspaceMembershipStatus.ACTIVE,
                        pageable
                )
                .stream()
                .map(this::toMemberResponse)
                .toList();
    }

    public WorkspaceMemberResponse addMember(
            UUID workspaceId,
            User currentUser,
            AddWorkspaceMemberRequest request
    ) {
        WorkspaceMembership actorMembership =
                authorizationService.requireAdminOrOwner(
                        workspaceId,
                        currentUser
                );

        WorkspaceRole requestedRole = request.role();

        if (requestedRole == WorkspaceRole.OWNER) {
            throw new InvalidWorkspaceOperationException(
                    "Owner role cannot be assigned through member addition."
            );
        }

        if (requestedRole == WorkspaceRole.ADMIN
                && actorMembership.getRole() != WorkspaceRole.OWNER) {
            throw new InvalidWorkspaceOperationException(
                    "Only the workspace owner may assign admin role."
            );
        }

        String normalizedEmail =
                request.email().trim().toLowerCase(Locale.ROOT);

        User targetUser = userRepository
                .findByEmailIgnoreCase(normalizedEmail)
                .orElseThrow(() ->
                        new ResourceNotFoundException("User not found.")
                );

        if (membershipRepository.findByWorkspaceIdAndUserId(
                workspaceId,
                targetUser.getId()
        ).isPresent()) {
            throw new DuplicateResourceException(
                    "User already has a workspace membership."
            );
        }

        WorkspaceMembership membership = new WorkspaceMembership();
        membership.setWorkspace(actorMembership.getWorkspace());
        membership.setUser(targetUser);
        membership.setRole(requestedRole);
        membership.setStatus(WorkspaceMembershipStatus.ACTIVE);
        membership.setJoinedAt(OffsetDateTime.now());

        WorkspaceMembership savedMembership =
                membershipRepository.save(membership);

        auditService.record(currentUser.getId(), SecurityAuditEventType.WORKSPACE_MEMBER_ADDED);

        return toMemberResponse(savedMembership);
    }

    public WorkspaceMemberResponse changeMemberRole(
            UUID workspaceId,
            UUID targetUserId,
            User currentUser,
            ChangeWorkspaceMemberRoleRequest request
    ) {
        authorizationService.requireOwner(workspaceId, currentUser);

        if (request.role() == WorkspaceRole.OWNER) {
            throw new InvalidWorkspaceOperationException(
                    "Owner role cannot be assigned through this endpoint."
            );
        }

        WorkspaceMembership targetMembership =
                membershipRepository.findByWorkspaceIdAndUserIdAndStatus(
                                workspaceId,
                                targetUserId,
                                WorkspaceMembershipStatus.ACTIVE
                        )
                        .orElseThrow(WorkspaceNotFoundException::new);

        if (targetMembership.getRole() == WorkspaceRole.OWNER) {
            throw new InvalidWorkspaceOperationException(
                    "Owner membership cannot be changed through this endpoint."
            );
        }

        targetMembership.setRole(request.role());

        auditService.record(currentUser.getId(), SecurityAuditEventType.WORKSPACE_MEMBER_ROLE_CHANGED);

        return toMemberResponse(targetMembership);
    }

    public void removeMember(
            UUID workspaceId,
            UUID targetUserId,
            User currentUser
    ) {
        WorkspaceMembership actorMembership =
                authorizationService.requireAdminOrOwner(
                        workspaceId,
                        currentUser
                );

        WorkspaceMembership targetMembership =
                membershipRepository.findByWorkspaceIdAndUserIdAndStatus(
                                workspaceId,
                                targetUserId,
                                WorkspaceMembershipStatus.ACTIVE
                        )
                        .orElseThrow(WorkspaceNotFoundException::new);

        if (targetMembership.getRole() == WorkspaceRole.OWNER) {
            throw new InvalidWorkspaceOperationException(
                    "Workspace owner cannot be removed."
            );
        }

        if (actorMembership.getRole() == WorkspaceRole.ADMIN
                && targetMembership.getRole() == WorkspaceRole.ADMIN) {
            throw new InvalidWorkspaceOperationException(
                    "Admins cannot remove other admins."
            );
        }

        targetMembership.setStatus(WorkspaceMembershipStatus.REMOVED);

        auditService.record(currentUser.getId(), SecurityAuditEventType.WORKSPACE_MEMBER_REMOVED);
    }

    private WorkspaceResponse toWorkspaceResponse(
            Workspace workspace,
            WorkspaceMembership currentUserMembership
    ) {
        return new WorkspaceResponse(
                workspace.getId(),
                workspace.getName(),
                workspace.getType(),
                workspace.getStatus(),
                workspace.getOwner().getId(),
                currentUserMembership.getRole(),
                workspace.getCreatedAt(),
                workspace.getUpdatedAt()
        );
    }

    private WorkspaceMemberResponse toMemberResponse(
            WorkspaceMembership membership
    ) {
        User user = membership.getUser();
        return new WorkspaceMemberResponse(
                user.getId(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                membership.getRole(),
                membership.getStatus(),
                membership.getJoinedAt()
        );
    }

    private String normalizeRequiredName(String name) {
        String normalized = name == null ? "" : name.trim();
        if (normalized.isEmpty()) {
            throw new InvalidWorkspaceOperationException(
                    "Workspace name is required."
            );
        }
        return normalized;
    }
}
