package com.researchassistant.analysis.repository;

import com.researchassistant.analysis.entity.ReportExportJob;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.UUID;

public interface ReportExportJobRepository extends JpaRepository<ReportExportJob, UUID> {
    @Query("""
            select coalesce(sum(j.fileSizeBytes), 0)
            from ReportExportJob j
            where j.report.project.workspace.id = :workspaceId
              and j.fileSizeBytes is not null
            """)
    long sumFileSizeBytesByWorkspaceId(@Param("workspaceId") UUID workspaceId);

    @Query("""
            select count(j)
            from ReportExportJob j
            where j.report.project.workspace.id = :workspaceId
              and j.requestedAt >= :start
              and j.requestedAt < :end
            """)
    long countByWorkspaceIdBetween(@Param("workspaceId") UUID workspaceId,
                                   @Param("start") OffsetDateTime start,
                                   @Param("end") OffsetDateTime end);
}
