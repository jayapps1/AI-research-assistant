package com.researchassistant.collaboration.repository;

import com.researchassistant.collaboration.entity.ArtifactReview;
import com.researchassistant.collaboration.entity.ArtifactReviewStatus;
import com.researchassistant.collaboration.entity.CollaborationArtifactType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ArtifactReviewRepository extends JpaRepository<ArtifactReview, UUID> {
    Page<ArtifactReview> findAllByProjectId(UUID projectId, Pageable pageable);
    Page<ArtifactReview> findAllByProjectIdAndStatus(UUID projectId, ArtifactReviewStatus status, Pageable pageable);
    List<ArtifactReview> findAllByProjectIdAndArtifactTypeAndArtifactIdAndStatusNot(UUID projectId, CollaborationArtifactType artifactType, UUID artifactId, ArtifactReviewStatus status);
    List<ArtifactReview> findAllByProjectIdAndReviewerId(UUID projectId, UUID reviewerId);
    Optional<ArtifactReview> findByIdAndProjectId(UUID id, UUID projectId);
}
