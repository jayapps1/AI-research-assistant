package com.researchassistant.collaboration.repository;

import com.researchassistant.collaboration.entity.ProjectCollaborationPolicy;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ProjectCollaborationPolicyRepository extends JpaRepository<ProjectCollaborationPolicy, UUID> {
    Optional<ProjectCollaborationPolicy> findByProjectId(UUID projectId);
}
