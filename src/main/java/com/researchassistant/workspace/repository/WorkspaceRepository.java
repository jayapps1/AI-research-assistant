package com.researchassistant.workspace.repository;

import com.researchassistant.workspace.entity.Workspace;
import com.researchassistant.workspace.entity.WorkspaceStatus;
import com.researchassistant.workspace.entity.WorkspaceType;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface WorkspaceRepository extends JpaRepository<Workspace, UUID> {

    Optional<Workspace> findFirstByOwnerIdAndTypeAndStatus(
            UUID ownerId,
            WorkspaceType type,
            WorkspaceStatus status
    );

    List<Workspace> findAllByOwnerIdAndTypeAndStatus(
            UUID ownerId,
            WorkspaceType type,
            WorkspaceStatus status
    );

    boolean existsByOwnerIdAndTypeAndStatus(
            UUID ownerId,
            WorkspaceType type,
            WorkspaceStatus status
    );

    long countByStatus(WorkspaceStatus status);

    @org.springframework.data.jpa.repository.Query("""
            select w from Workspace w
            where w.status = com.researchassistant.workspace.entity.WorkspaceStatus.ACTIVE
              and not exists (
                  select s.id from WorkspaceSubscription s
                  where s.workspace.id = w.id
                    and s.status in (com.researchassistant.subscription.WorkspaceSubscriptionStatus.TRIALING,
                                     com.researchassistant.subscription.WorkspaceSubscriptionStatus.ACTIVE,
                                     com.researchassistant.subscription.WorkspaceSubscriptionStatus.PAST_DUE,
                                     com.researchassistant.subscription.WorkspaceSubscriptionStatus.SUSPENDED)
              )
            """)
    List<Workspace> findWorkspacesWithoutActiveSubscription();
}
