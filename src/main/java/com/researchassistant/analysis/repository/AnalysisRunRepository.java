package com.researchassistant.analysis.repository;

import com.researchassistant.analysis.entity.AnalysisRun;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface AnalysisRunRepository extends JpaRepository<AnalysisRun, UUID> {
    Page<AnalysisRun> findAllByProjectId(UUID projectId, Pageable pageable);
    java.util.List<AnalysisRun> findAllByProjectId(UUID projectId);
    long countByProjectId(UUID projectId);
}
