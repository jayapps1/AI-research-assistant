package com.researchassistant.literature.repository;

import com.researchassistant.literature.entity.LiteratureMatrix;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LiteratureMatrixRepository extends JpaRepository<LiteratureMatrix, UUID> {
    Optional<LiteratureMatrix> findFirstByProjectIdOrderByCreatedAtDesc(UUID projectId);
    List<LiteratureMatrix> findAllByProjectIdOrderByCreatedAtDesc(UUID projectId);
    Optional<LiteratureMatrix> findByIdAndProjectId(UUID id, UUID projectId);
}
