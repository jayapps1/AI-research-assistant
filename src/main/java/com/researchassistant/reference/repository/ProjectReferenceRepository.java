package com.researchassistant.reference.repository;

import com.researchassistant.reference.entity.ProjectReference;
import com.researchassistant.reference.entity.ProjectReferenceStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProjectReferenceRepository extends JpaRepository<ProjectReference, UUID> {
    Page<ProjectReference> findAllByProjectIdAndStatus(UUID projectId, ProjectReferenceStatus status, Pageable pageable);
    List<ProjectReference> findAllByProjectIdAndStatusOrderByCitationKeyAsc(UUID projectId, ProjectReferenceStatus status);
    List<ProjectReference> findAllByProjectId(UUID projectId);
    Optional<ProjectReference> findByProjectIdAndReferenceId(UUID projectId, UUID referenceId);
    boolean existsByProjectIdAndCitationKey(UUID projectId, String citationKey);
    boolean existsByProjectIdAndCitationKeyAndIdNot(UUID projectId, String citationKey, UUID id);
}
