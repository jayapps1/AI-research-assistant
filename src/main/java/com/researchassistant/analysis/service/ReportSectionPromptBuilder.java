package com.researchassistant.analysis.service;

import com.researchassistant.analysis.dto.AnalysisDtos.GenerateSectionRequest;
import com.researchassistant.analysis.entity.ResearchReport;
import com.researchassistant.analysis.entity.ResearchReportSection;
import com.researchassistant.analysis.entity.SectionGenerationPolicy;
import com.researchassistant.analysis.entity.SectionSemanticPurpose;
import com.researchassistant.analysis.service.AcademicSectionPromptService.SectionGenerationContext;
import com.researchassistant.project.entity.AcademicProjectType;
import com.researchassistant.project.entity.AcademicWorkspaceType;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ReportSectionPromptBuilder {

    private final AcademicSectionPromptService academicSectionPromptService;

    public ReportSectionPromptBuilder(AcademicSectionPromptService academicSectionPromptService) {
        this.academicSectionPromptService = academicSectionPromptService;
    }

    public ReportSectionPromptBuilder() {
        this.academicSectionPromptService = new AcademicSectionPromptService();
    }

    public record ProjectContextData(
            String projectTitle,
            AcademicWorkspaceType workspaceType,
            AcademicProjectType projectType,
            String researchAim,
            List<String> objectives,
            List<String> questions,
            String problemStatement,
            String methodologySummary,
            int findingsCount,
            int analysisRunsCount
    ) {}

    public String buildPrompt(ResearchReportSection section, GenerateSectionRequest request, ProjectContextData context) {
        ResearchReport report = section.getChapter().getReport();
        SectionSemanticPurpose purpose = section.resolveSemanticPurpose();
        SectionGenerationPolicy policy = section.resolveGenerationPolicy();
        String heading = section.getHeading() != null ? section.getHeading() : "Section";
        String chapterTitle = section.getChapter() != null ? section.getChapter().getTitle() : "Chapter";

        SectionGenerationContext generationContext = new SectionGenerationContext(
                context.workspaceType(),
                context.projectType(),
                report.getType() != null ? report.getType().name() : "REPORT",
                chapterTitle,
                section.getId(),
                section.getType(),
                purpose,
                policy,
                heading,
                null,
                context.projectTitle(),
                null,
                context.researchAim(),
                context.objectives(),
                context.questions(),
                context.problemStatement(),
                null,
                null,
                context.methodologySummary(),
                context.findingsCount(),
                context.analysisRunsCount(),
                null,
                request != null ? request.instructions() : null,
                request != null && request.sourceScope() != null ? request.sourceScope() : null
        );

        return academicSectionPromptService.build(generationContext);
    }

    public static String buildPrompt(
            AcademicWorkspaceType workspaceType,
            AcademicProjectType projectType,
            String documentType,
            String chapterTitle,
            String sectionTitle,
            SectionSemanticPurpose purpose,
            String templateRequirements,
            String projectTitle,
            String availableEvidence,
            String userInstructions
    ) {
        SectionGenerationContext generationContext = new SectionGenerationContext(
                workspaceType,
                projectType,
                documentType,
                chapterTitle,
                null,
                purpose != null ? purpose.defaultSectionType() : null,
                purpose != null ? purpose : SectionSemanticPurpose.CUSTOM,
                purpose != null ? purpose.defaultPolicy() : SectionGenerationPolicy.CONTEXTUAL_AI,
                sectionTitle,
                null,
                projectTitle,
                null,
                null,
                List.of(),
                List.of(),
                null,
                null,
                null,
                availableEvidence,
                0,
                0,
                templateRequirements,
                userInstructions,
                null
        );

        return new AcademicSectionPromptService().build(generationContext);
    }

    public String buildRetrievalQuery(ResearchReportSection section, GenerateSectionRequest request, ProjectContextData context) {
        SectionSemanticPurpose purpose = section.resolveSemanticPurpose();
        SectionGenerationPolicy policy = section.resolveGenerationPolicy();
        String heading = section.getHeading() != null ? section.getHeading() : "";
        String chapterTitle = section.getChapter() != null ? section.getChapter().getTitle() : "";

        SectionGenerationContext generationContext = new SectionGenerationContext(
                context.workspaceType(),
                context.projectType(),
                null,
                chapterTitle,
                section.getId(),
                section.getType(),
                purpose,
                policy,
                heading,
                null,
                context.projectTitle(),
                null,
                context.researchAim(),
                context.objectives(),
                context.questions(),
                context.problemStatement(),
                null,
                null,
                context.methodologySummary(),
                context.findingsCount(),
                context.analysisRunsCount(),
                null,
                request != null ? request.instructions() : null,
                null
        );

        return academicSectionPromptService.buildRetrievalQuery(generationContext);
    }

    public boolean requiresSourceRetrieval(ResearchReportSection section) {
        SectionGenerationPolicy policy = section.resolveGenerationPolicy();
        SectionSemanticPurpose purpose = section.resolveSemanticPurpose();
        SectionGenerationContext generationContext = new SectionGenerationContext(
                null,
                null,
                null,
                null,
                section.getId(),
                section.getType(),
                purpose,
                policy,
                section.getHeading(),
                null,
                null,
                null,
                null,
                List.of(),
                List.of(),
                null,
                null,
                null,
                null,
                0,
                0,
                null,
                null,
                null
        );
        return academicSectionPromptService.requiresSourceRetrieval(generationContext);
    }
}
