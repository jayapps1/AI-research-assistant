package com.researchassistant.collaboration.repository;

import com.researchassistant.collaboration.entity.ArtifactApproval;
import com.researchassistant.collaboration.entity.ArtifactApprovalStatus;
import com.researchassistant.collaboration.entity.CollaborationArtifactType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ArtifactApprovalRepository extends JpaRepository<ArtifactApproval, UUID> {
    List<ArtifactApproval> findAllByProjectIdAndArtifactTypeAndArtifactIdAndStatusNot(UUID projectId, CollaborationArtifactType artifactType, UUID artifactId, ArtifactApprovalStatus status);
    Optional<ArtifactApproval> findByIdAndProjectId(UUID id, UUID projectId);
}
