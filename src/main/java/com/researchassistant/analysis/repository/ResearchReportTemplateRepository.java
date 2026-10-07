package com.researchassistant.analysis.repository;

import com.researchassistant.analysis.entity.ResearchReportTemplate;
import com.researchassistant.analysis.entity.ResearchReportType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ResearchReportTemplateRepository extends JpaRepository<ResearchReportTemplate, UUID> {
    Optional<ResearchReportTemplate> findFirstByTypeAndSystemTemplateTrueOrderByCreatedAtAsc(ResearchReportType type);

    @Query("""
            select t
            from ResearchReportTemplate t
            where t.type = :type
              and t.systemTemplate = true
              and (
                  t.supportedWorkspaceTypes is null
                  or t.supportedWorkspaceTypes = ''
                  or t.supportedWorkspaceTypes like concat('%', :workspaceType, '%')
              )
            order by t.createdAt asc
            """)
    List<ResearchReportTemplate> findSystemTemplatesForWorkspaceType(
            @Param("type") ResearchReportType type,
            @Param("workspaceType") String workspaceType,
            Pageable pageable
    );
}
