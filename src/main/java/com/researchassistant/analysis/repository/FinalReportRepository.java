package com.researchassistant.analysis.repository;

import com.researchassistant.analysis.entity.FinalReport;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface FinalReportRepository extends JpaRepository<FinalReport, UUID> {
    Page<FinalReport> findAllByProjectIdOrderByUpdatedAtDesc(UUID projectId, Pageable pageable);
}
