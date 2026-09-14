package com.researchassistant.collaboration.repository;

import com.researchassistant.collaboration.entity.ProjectTask;
import com.researchassistant.collaboration.entity.ProjectTaskPriority;
import com.researchassistant.collaboration.entity.ProjectTaskStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ProjectTaskRepository extends JpaRepository<ProjectTask, UUID> {
    Page<ProjectTask> findAllByProjectId(UUID projectId, Pageable pageable);
    Page<ProjectTask> findAllByProjectIdAndStatus(UUID projectId, ProjectTaskStatus status, Pageable pageable);
    Page<ProjectTask> findAllByProjectIdAndPriority(UUID projectId, ProjectTaskPriority priority, Pageable pageable);
    Optional<ProjectTask> findByIdAndProjectId(UUID id, UUID projectId);
}
