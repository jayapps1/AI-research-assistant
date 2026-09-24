package com.researchassistant.analysis.repository;

import com.researchassistant.analysis.entity.ResearchReportSection;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ResearchReportSectionRepository extends JpaRepository<ResearchReportSection, UUID> {
    List<ResearchReportSection> findAllByChapterIdOrderByDisplayOrderAsc(UUID chapterId);
    List<ResearchReportSection> findAllByChapterIdAndParentSectionIsNullOrderByDisplayOrderAsc(UUID chapterId);
    List<ResearchReportSection> findAllByParentSectionIdOrderByDisplayOrderAsc(UUID parentSectionId);
    long countByChapterIdAndParentSectionIsNull(UUID chapterId);
    long countByParentSectionId(UUID parentSectionId);
    List<ResearchReportSection> findAllByChapterReportId(UUID reportId);
    List<ResearchReportSection> findAllByChapterReportProjectId(UUID projectId);

    @Query("""
            select section
            from ResearchReportSection section
            join fetch section.chapter chapter
            join fetch chapter.report report
            join fetch report.project project
            where section.id = :id
            """)
    Optional<ResearchReportSection> findWithChapterReportProjectById(UUID id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select section from ResearchReportSection section where section.id = :id")
    Optional<ResearchReportSection> findByIdForUpdate(UUID id);
}
