package com.researchassistant.collaboration.repository;

import com.researchassistant.collaboration.entity.CollaborationArtifactType;
import com.researchassistant.collaboration.entity.ProjectActivity;
import com.researchassistant.collaboration.entity.ProjectActivityType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ProjectActivityRepository extends JpaRepository<ProjectActivity, UUID> {
    Page<ProjectActivity> findAllByProjectIdOrderByOccurredAtDesc(UUID projectId, Pageable pageable);
    Page<ProjectActivity> findAllByProjectIdAndActorIdOrderByOccurredAtDesc(UUID projectId, UUID actorId, Pageable pageable);
    Page<ProjectActivity> findAllByProjectIdAndTypeOrderByOccurredAtDesc(UUID projectId, ProjectActivityType type, Pageable pageable);
    Page<ProjectActivity> findAllByProjectIdAndArtifactTypeOrderByOccurredAtDesc(UUID projectId, CollaborationArtifactType artifactType, Pageable pageable);
    long countByProjectIdAndActorIdAndType(UUID projectId, UUID actorId, ProjectActivityType type);
}
