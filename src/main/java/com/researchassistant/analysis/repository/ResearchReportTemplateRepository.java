package com.researchassistant.analysis.repository;

import com.researchassistant.analysis.entity.ResearchReportTemplate;
import com.researchassistant.analysis.entity.ResearchReportType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ResearchReportTemplateRepository extends JpaRepository<ResearchReportTemplate, UUID> {
    Optional<ResearchReportTemplate> findFirstByTypeAndSystemTemplateTrueOrderByCreatedAtAsc(ResearchReportType type);
}
