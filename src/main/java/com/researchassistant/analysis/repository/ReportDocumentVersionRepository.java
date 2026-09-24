package com.researchassistant.analysis.repository;

import com.researchassistant.analysis.entity.ReportDocumentVersion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;
import java.util.UUID;

public interface ReportDocumentVersionRepository extends JpaRepository<ReportDocumentVersion, UUID> {
    Optional<ReportDocumentVersion> findFirstByReportIdOrderByVersionNumberDesc(UUID reportId);

    @Query("select coalesce(max(version.versionNumber), 0) from ReportDocumentVersion version where version.report.id = :reportId")
    int maxVersionNumber(UUID reportId);
}
