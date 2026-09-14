package com.researchassistant.analysis.repository;

import com.researchassistant.analysis.entity.ResearchFinding;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ResearchFindingRepository extends JpaRepository<ResearchFinding, UUID> {
    Page<ResearchFinding> findAllByProjectIdOrderByDisplayOrderAsc(UUID projectId, Pageable pageable);
    long countByProjectId(UUID projectId);
    long countByProjectIdAndStatus(UUID projectId, com.researchassistant.analysis.entity.ResearchFindingStatus status);
}
