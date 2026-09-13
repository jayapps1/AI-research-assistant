package com.researchassistant.workspace.service;

import com.researchassistant.identity.entity.User;
import com.researchassistant.workspace.entity.WorkspaceMembership;
import com.researchassistant.workspace.entity.WorkspaceMembershipStatus;
import com.researchassistant.workspace.entity.WorkspaceRole;
import com.researchassistant.workspace.exception.WorkspaceAccessDeniedException;
import com.researchassistant.workspace.exception.WorkspaceNotFoundException;
import com.researchassistant.workspace.repository.WorkspaceMembershipRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Central workspace authorization boundary.
 *
 * <p>Workspace IDs are tenant-boundary identifiers. Future services
 * for projects, documents, RAG indexes and reports should call this
 * component before accessing workspace-scoped data, or use repository
 * queries that include the authorized workspace ID explicitly.</p>
 */
@Service
@Transactional(readOnly = true)
public class WorkspaceAuthorizationService {

    private final WorkspaceMembershipRepository membershipRepository;

    public WorkspaceAuthorizationService(
            WorkspaceMembershipRepository membershipRepository
    ) {
        this.membershipRepository = membershipRepository;
    }

    public WorkspaceMembership requireActiveMembership(
            UUID workspaceId,
            User user
    ) {
        return membershipRepository
                .findByWorkspaceIdAndUserIdAndStatus(
                        workspaceId,
                        user.getId(),
                        WorkspaceMembershipStatus.ACTIVE
                )
                .orElseThrow(WorkspaceNotFoundException::new);
    }

    public WorkspaceMembership requireAdminOrOwner(
            UUID workspaceId,
            User user
    ) {
        WorkspaceMembership membership =
                requireActiveMembership(workspaceId, user);

        if (membership.getRole() != WorkspaceRole.OWNER
                && membership.getRole() != WorkspaceRole.ADMIN) {
            throw new WorkspaceAccessDeniedException(
                    "Workspace administrator access is required."
            );
        }

        return membership;
    }

    public WorkspaceMembership requireOwner(UUID workspaceId, User user) {
        WorkspaceMembership membership =
                requireActiveMembership(workspaceId, user);

        if (membership.getRole() != WorkspaceRole.OWNER) {
            throw new WorkspaceAccessDeniedException(
                    "Workspace owner access is required."
            );
        }

        return membership;
    }
}
