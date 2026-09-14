package com.researchassistant.analysis.repository;

import com.researchassistant.analysis.entity.ResearchReportChapter;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ResearchReportChapterRepository extends JpaRepository<ResearchReportChapter, UUID> {
    List<ResearchReportChapter> findAllByReportIdOrderByDisplayOrderAsc(UUID reportId);
}
