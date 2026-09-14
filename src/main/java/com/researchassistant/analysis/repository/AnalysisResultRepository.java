package com.researchassistant.analysis.repository;

import com.researchassistant.analysis.entity.AnalysisResult;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AnalysisResultRepository extends JpaRepository<AnalysisResult, UUID> {
    List<AnalysisResult> findAllByAnalysisRunIdOrderByCreatedAtDesc(UUID analysisRunId);
    long countByProjectId(UUID projectId);
}
