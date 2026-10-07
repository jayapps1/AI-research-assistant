package com.researchassistant.template.service;

import com.researchassistant.analysis.entity.ResearchReportTemplate;
import com.researchassistant.analysis.repository.ResearchReportTemplateRepository;
import com.researchassistant.project.entity.AcademicProjectType;
import com.researchassistant.project.entity.AcademicWorkspaceType;
import com.researchassistant.template.dto.AcademicTemplateDtos.RecommendedTemplateResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
public class TemplateRelevanceService {

    private final ResearchReportTemplateRepository templateRepository;

    public TemplateRelevanceService(ResearchReportTemplateRepository templateRepository) {
        this.templateRepository = templateRepository;
    }

    /**
     * Ranks system and custom templates based on contextual relevance:
     * - workspaceType match
     * - projectType match (e.g. software/system development vs general empirical research)
     * - department & institution alignment
     * Irrelevant templates (e.g. nursing, business, lab) are pushed to the bottom.
     */
    @Transactional(readOnly = true)
    public List<RecommendedTemplateResponse> getRecommendedTemplates(
            AcademicWorkspaceType workspaceType,
            AcademicProjectType projectType,
            String institution,
            String department,
            String documentType
    ) {
        List<ResearchReportTemplate> allTemplates = templateRepository.findAll();
        List<ScoredTemplate> scoredList = new ArrayList<>();

        for (ResearchReportTemplate tpl : allTemplates) {
            int score = calculateRelevanceScore(tpl, workspaceType, projectType, institution, department, documentType);
            String reason = determineReason(tpl, workspaceType, projectType, score);
            scoredList.add(new ScoredTemplate(tpl, score, reason));
        }

        // Sort by score descending, then by name
        scoredList.sort(Comparator.comparingInt(ScoredTemplate::score).reversed()
                .thenComparing(st -> st.template().getName()));

        return scoredList.stream()
                .map(st -> new RecommendedTemplateResponse(
                        st.template().getId(),
                        st.template().getName(),
                        st.template().getType() != null ? st.template().getType().name() : "CUSTOM",
                        st.template().getInstitution(),
                        st.template().getDepartment(),
                        st.template().getSupportedWorkspaceTypes(),
                        st.template().getDescription(),
                        st.template().isSystemTemplate(),
                        st.score(),
                        st.reason()
                ))
                .toList();
    }

    private int calculateRelevanceScore(
            ResearchReportTemplate tpl,
            AcademicWorkspaceType wsType,
            AcademicProjectType projType,
            String institution,
            String department,
            String docType
    ) {
        int score = 0;

        // 1. Workspace type match (+50 points)
        if (wsType != null && tpl.getSupportedWorkspaceTypes() != null) {
            if (tpl.getSupportedWorkspaceTypes().contains(wsType.name())) {
                score += 50;
            } else {
                score -= 30; // Penalize mismatched workspace type
            }
        }

        // 2. Project type specific match (+40 points)
        String tplNameLower = tpl.getName().toLowerCase(Locale.ROOT);
        String descLower = tpl.getDescription() != null ? tpl.getDescription().toLowerCase(Locale.ROOT) : "";

        if (projType == AcademicProjectType.SOFTWARE_SYSTEM_DEVELOPMENT) {
            if (tplNameLower.contains("software") || tplNameLower.contains("system") || tplNameLower.contains("computer science") || descLower.contains("software")) {
                score += 40;
            } else if (tplNameLower.contains("nursing") || tplNameLower.contains("health") || tplNameLower.contains("laboratory") || tplNameLower.contains("business")) {
                score -= 50; // Heavy penalty for irrelevant domains
            }
        } else if (projType == AcademicProjectType.RESEARCH_BASED_PROJECT) {
            if (tplNameLower.contains("research") || tplNameLower.contains("empirical") || tplNameLower.contains("thesis")) {
                score += 30;
            }
        }

        // 3. Institution alignment (+25 points)
        if (institution != null && !institution.isBlank() && tpl.getInstitution() != null) {
            if (tpl.getInstitution().toLowerCase(Locale.ROOT).contains(institution.toLowerCase(Locale.ROOT))) {
                score += 25;
            }
        }

        // 4. Department alignment (+25 points)
        if (department != null && !department.isBlank() && tpl.getDepartment() != null) {
            if (tpl.getDepartment().toLowerCase(Locale.ROOT).contains(department.toLowerCase(Locale.ROOT))) {
                score += 25;
            }
        }

        // 5. System template baseline (+10 points)
        if (tpl.isSystemTemplate()) {
            score += 10;
        }

        return score;
    }

    private String determineReason(
            ResearchReportTemplate tpl,
            AcademicWorkspaceType wsType,
            AcademicProjectType projType,
            int score
    ) {
        if (score >= 80) return "Direct match for " + (projType != null ? projType.name() : "project") + " and " + (wsType != null ? wsType.name() : "workspace");
        if (score >= 50) return "Compatible template for " + (wsType != null ? wsType.name() : "workspace");
        if (score < 0) return "Domain not recommended for current project profile";
        return "General academic template";
    }

    private record ScoredTemplate(ResearchReportTemplate template, int score, String reason) {}
}
