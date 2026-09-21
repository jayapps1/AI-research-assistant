package com.researchassistant.dataset.repository;

import com.researchassistant.dataset.model.ResearchDataset;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ResearchDatasetRepository extends JpaRepository<ResearchDataset, UUID> {
    Page<ResearchDataset> findAllByProjectId(UUID projectId, Pageable pageable);
    long countByProjectId(UUID projectId);
}
