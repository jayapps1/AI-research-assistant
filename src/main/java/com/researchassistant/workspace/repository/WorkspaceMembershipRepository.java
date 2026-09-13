package com.researchassistant.workspace.repository;

import com.researchassistant.workspace.entity.WorkspaceMembership;
import com.researchassistant.workspace.entity.WorkspaceMembershipStatus;
import com.researchassistant.workspace.entity.WorkspaceRole;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface WorkspaceMembershipRepository
        extends JpaRepository<WorkspaceMembership, UUID> {

    Optional<WorkspaceMembership> findByWorkspaceIdAndUserId(
            UUID workspaceId,
            UUID userId
    );

    Optional<WorkspaceMembership> findByWorkspaceIdAndUserIdAndStatus(
            UUID workspaceId,
            UUID userId,
            WorkspaceMembershipStatus status
    );

    List<WorkspaceMembership> findAllByUserIdAndStatus(
            UUID userId,
            WorkspaceMembershipStatus status
    );

    boolean existsByWorkspaceIdAndUserIdAndStatus(
            UUID workspaceId,
            UUID userId,
            WorkspaceMembershipStatus status
    );

    List<WorkspaceMembership> findAllByWorkspaceIdAndStatus(
            UUID workspaceId,
            WorkspaceMembershipStatus status,
            Pageable pageable
    );

    Optional<WorkspaceMembership> findByWorkspaceIdAndRole(
            UUID workspaceId,
            WorkspaceRole role
    );
}
