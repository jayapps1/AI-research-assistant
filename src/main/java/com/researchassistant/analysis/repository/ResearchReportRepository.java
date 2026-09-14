package com.researchassistant.analysis.repository;

import com.researchassistant.analysis.entity.ResearchReport;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ResearchReportRepository extends JpaRepository<ResearchReport, UUID> {
    Page<ResearchReport> findAllByProjectIdOrderByUpdatedAtDesc(UUID projectId, Pageable pageable);
}
