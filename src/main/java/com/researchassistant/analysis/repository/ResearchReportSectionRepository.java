package com.researchassistant.analysis.repository;

import com.researchassistant.analysis.entity.ResearchReportSection;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ResearchReportSectionRepository extends JpaRepository<ResearchReportSection, UUID> {
    List<ResearchReportSection> findAllByChapterIdOrderByDisplayOrderAsc(UUID chapterId);
    List<ResearchReportSection> findAllByChapterReportId(UUID reportId);
    List<ResearchReportSection> findAllByChapterReportProjectId(UUID projectId);
}
