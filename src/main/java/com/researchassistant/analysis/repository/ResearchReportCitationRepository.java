package com.researchassistant.analysis.repository;

import com.researchassistant.analysis.entity.ResearchReportCitation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ResearchReportCitationRepository extends JpaRepository<ResearchReportCitation, UUID> {
    List<ResearchReportCitation> findAllBySectionIdOrderByCitationOrdinalAsc(UUID sectionId);
    List<ResearchReportCitation> findAllBySectionChapterReportIdOrderByCitationOrdinalAsc(UUID reportId);
    boolean existsByDocumentId(UUID documentId);
}
