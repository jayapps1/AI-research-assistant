package com.researchassistant.workspace.repository;

import com.researchassistant.workspace.entity.Workspace;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface WorkspaceRepository extends JpaRepository<Workspace, UUID> {
}
