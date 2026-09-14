package com.researchassistant.collaboration.repository;

import com.researchassistant.collaboration.entity.ProjectTaskArtifactLink;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ProjectTaskArtifactLinkRepository extends JpaRepository<ProjectTaskArtifactLink, UUID> {
    List<ProjectTaskArtifactLink> findAllByTaskId(UUID taskId);
}
