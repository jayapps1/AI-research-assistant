package com.researchassistant.analysis.repository;

import com.researchassistant.analysis.entity.ResearchConclusion;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ResearchConclusionRepository extends JpaRepository<ResearchConclusion, UUID> {
    Page<ResearchConclusion> findAllByProjectIdOrderByDisplayOrderAsc(UUID projectId, Pageable pageable);
    long countByProjectId(UUID projectId);
    long countByProjectIdAndStatus(UUID projectId, com.researchassistant.analysis.entity.ResearchConclusionStatus status);
}
