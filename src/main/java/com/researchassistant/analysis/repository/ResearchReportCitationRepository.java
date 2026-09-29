package com.researchassistant.analysis.repository;

import com.researchassistant.analysis.entity.ResearchReportCitation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface ResearchReportCitationRepository extends JpaRepository<ResearchReportCitation, UUID> {
    List<ResearchReportCitation> findAllBySectionIdOrderByCitationOrdinalAsc(UUID sectionId);

    @Query("""
            select citation
            from ResearchReportCitation citation
            join citation.section section
            join section.chapter chapter
            where chapter.report.id = :reportId
            order by chapter.displayOrder asc, section.displayOrder asc, citation.citationOrdinal asc, citation.createdAt asc
            """)
    List<ResearchReportCitation> findAllBySectionChapterReportIdOrderByCitationOrdinalAsc(UUID reportId);
    boolean existsByDocumentId(UUID documentId);

    @Modifying(flushAutomatically = true)
    @Query(value = "delete from research_report_citations where section_id = :sectionId", nativeQuery = true)
    int deleteAllBySectionId(@Param("sectionId") UUID sectionId);
}
