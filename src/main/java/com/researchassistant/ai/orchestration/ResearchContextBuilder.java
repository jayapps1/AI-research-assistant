package com.researchassistant.ai.orchestration;

import com.researchassistant.project.entity.ResearchProject;
import com.researchassistant.project.service.ResearchProjectService;

import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class ResearchContextBuilder {

    private final ResearchProjectService projectService;

    public ResearchContextBuilder(ResearchProjectService projectService) {
        this.projectService = projectService;
    }

    public String buildProjectContext(UUID projectId) {
        if (projectId == null) {
            return "";
        }
        try {
            ResearchProject project = projectService.getProjectEntity(projectId);
            StringBuilder sb = new StringBuilder();
            sb.append("PROJECT TOPIC / TITLE: ").append(project.getTitle()).append("\n");
            if (project.getDescription() != null && !project.getDescription().isBlank()) {
                sb.append("PROJECT DESCRIPTION: ").append(project.getDescription()).append("\n");
            }
            if (project.getResearchAim() != null && !project.getResearchAim().isBlank()) {
                sb.append("RESEARCH AIM: ").append(project.getResearchAim()).append("\n");
            }
            if (project.getStudyArea() != null && !project.getStudyArea().isBlank()) {
                sb.append("STUDY AREA: ").append(project.getStudyArea()).append("\n");
            }
            if (project.getResearchType() != null && !project.getResearchType().isBlank()) {
                sb.append("RESEARCH TYPE: ").append(project.getResearchType()).append("\n");
            }
            if (project.getKeywords() != null && !project.getKeywords().isBlank()) {
                sb.append("KEYWORDS: ").append(project.getKeywords()).append("\n");
            }
            return sb.toString();
        } catch (Exception e) {
            return "";
        }
    }
}
