package com.researchassistant.analysis.repository;

import com.researchassistant.analysis.entity.ResearchReport;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;
import java.util.UUID;

public interface ResearchReportRepository extends JpaRepository<ResearchReport, UUID> {
    Page<ResearchReport> findAllByProjectIdOrderByUpdatedAtDesc(UUID projectId, Pageable pageable);
    Optional<ResearchReport> findFirstByProjectIdOrderByUpdatedAtDesc(UUID projectId);
    Optional<ResearchReport> findByProjectId(UUID projectId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select report from ResearchReport report where report.id = :id")
    Optional<ResearchReport> findByIdForUpdate(UUID id);
}
