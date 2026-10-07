package com.researchassistant.evidence.repository;

import com.researchassistant.evidence.entity.EvidenceType;
import com.researchassistant.evidence.entity.ProjectEvidence;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ProjectEvidenceRepository extends JpaRepository<ProjectEvidence, UUID> {

    List<ProjectEvidence> findAllByProjectIdOrderByDisplayOrderAscCreatedAtAsc(UUID projectId);

    List<ProjectEvidence> findAllByReportIdOrderByDisplayOrderAscCreatedAtAsc(UUID reportId);

    List<ProjectEvidence> findAllBySectionIdOrderByDisplayOrderAscCreatedAtAsc(UUID sectionId);

    List<ProjectEvidence> findAllByProjectIdAndEvidenceTypeOrderByDisplayOrderAscCreatedAtAsc(
            UUID projectId, EvidenceType evidenceType
    );

    @Query("""
        SELECT e FROM ProjectEvidence e
        WHERE e.report.id = :reportId
          AND e.section IS NOT NULL
        ORDER BY e.section.chapter.displayOrder ASC, e.section.displayOrder ASC, e.displayOrder ASC, e.createdAt ASC
    """)
    List<ProjectEvidence> findAllAssignedToReportHierarchy(@Param("reportId") UUID reportId);

    @Query("""
        SELECT e FROM ProjectEvidence e
        WHERE e.project.id = :projectId
          AND e.section IS NOT NULL
        ORDER BY e.section.chapter.displayOrder ASC, e.section.displayOrder ASC, e.displayOrder ASC, e.createdAt ASC
    """)
    List<ProjectEvidence> findAllAssignedToProjectHierarchy(@Param("projectId") UUID projectId);

    long countByProjectId(UUID projectId);

    long countBySectionId(UUID sectionId);
}
