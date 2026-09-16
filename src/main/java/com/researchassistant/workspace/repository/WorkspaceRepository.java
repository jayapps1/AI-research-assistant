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
}
