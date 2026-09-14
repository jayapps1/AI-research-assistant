package com.researchassistant.collaboration.repository;

import com.researchassistant.collaboration.entity.ArtifactComment;
import com.researchassistant.collaboration.entity.CollaborationArtifactType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ArtifactCommentRepository extends JpaRepository<ArtifactComment, UUID> {
    Page<ArtifactComment> findAllByProjectId(UUID projectId, Pageable pageable);
    Page<ArtifactComment> findAllByProjectIdAndArtifactTypeAndArtifactId(UUID projectId, CollaborationArtifactType artifactType, UUID artifactId, Pageable pageable);
    List<ArtifactComment> findAllByProjectIdAndAuthorId(UUID projectId, UUID authorId);
    Optional<ArtifactComment> findByIdAndProjectId(UUID id, UUID projectId);
}
