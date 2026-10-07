package com.researchassistant.analysis.service;

import com.researchassistant.analysis.entity.ReportSectionType;
import com.researchassistant.analysis.entity.SectionGenerationPolicy;
import com.researchassistant.analysis.entity.SectionSemanticPurpose;
import com.researchassistant.analysis.service.AcademicSectionPromptService.SectionGenerationContext;
import com.researchassistant.project.entity.AcademicProjectType;
import com.researchassistant.project.entity.AcademicWorkspaceType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class AcademicSectionPromptServiceTest {

    private final AcademicSectionPromptService promptService = new AcademicSectionPromptService();

    @Test
    @DisplayName("Centralized AcademicSectionPromptService produces specialized prompts with no generic literature bias")
    void testSpecializedSectionPrompts() {
        SectionGenerationContext bgContext = new SectionGenerationContext(
                AcademicWorkspaceType.ACADEMIC_RESEARCH,
                AcademicProjectType.GENERAL_ACADEMIC_PROJECT,
                "Research Report",
                "Chapter One: Introduction",
                UUID.randomUUID(),
                ReportSectionType.BACKGROUND,
                SectionSemanticPurpose.BACKGROUND,
                SectionGenerationPolicy.SOURCE_GROUNDED_AI,
                "Background of the Study",
                null,
                "Digital Agriculture Adoption",
                null,
                "To investigate adoption rates",
                List.of("Examine baseline adoption", "Identify barriers"),
                List.of("What is the current baseline?", "What barriers exist?"),
                "Low digital tool adoption among smallholder farmers",
                "Sub-Saharan Africa",
                "Empirical Quantitative",
                "Survey Design",
                0,
                0,
                null,
                "Focus on smallholder farmers",
                "ALL"
        );

        SectionGenerationContext problemContext = new SectionGenerationContext(
                AcademicWorkspaceType.ACADEMIC_RESEARCH,
                AcademicProjectType.GENERAL_ACADEMIC_PROJECT,
                "Research Report",
                "Chapter One: Introduction",
                UUID.randomUUID(),
                ReportSectionType.PROBLEM_STATEMENT,
                SectionSemanticPurpose.PROBLEM_STATEMENT,
                SectionGenerationPolicy.SOURCE_GROUNDED_AI,
                "Statement of the Problem",
                null,
                "Digital Agriculture Adoption",
                null,
                null,
                List.of(),
                List.of(),
                "Critical shortfall in productivity due to lack of digital tool access",
                null,
                null,
                null,
                0,
                0,
                null,
                null,
                "ALL"
        );

        SectionGenerationContext litContext = new SectionGenerationContext(
                AcademicWorkspaceType.ACADEMIC_RESEARCH,
                AcademicProjectType.GENERAL_ACADEMIC_PROJECT,
                "Research Report",
                "Chapter Two: Literature Review",
                UUID.randomUUID(),
                ReportSectionType.LITERATURE_REVIEW,
                SectionSemanticPurpose.LITERATURE_REVIEW,
                SectionGenerationPolicy.SOURCE_GROUNDED_AI,
                "Literature Review",
                null,
                "Digital Agriculture Adoption",
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
                "ALL"
        );

        SectionGenerationContext methodologyContext = new SectionGenerationContext(
                AcademicWorkspaceType.ACADEMIC_RESEARCH,
                AcademicProjectType.GENERAL_ACADEMIC_PROJECT,
                "Research Report",
                "Chapter Three: Methodology",
                UUID.randomUUID(),
                ReportSectionType.METHODOLOGY,
                SectionSemanticPurpose.METHODOLOGY,
                SectionGenerationPolicy.PROJECT_DERIVED_AI,
                "Research Methodology",
                null,
                "Digital Agriculture Adoption",
                null,
                null,
                List.of(),
                List.of(),
                null,
                null,
                null,
                "Cross-sectional survey with stratified random sampling",
                0,
                0,
                null,
                null,
                null
        );

        SectionGenerationContext designContext = new SectionGenerationContext(
                AcademicWorkspaceType.ACADEMIC_PROJECT,
                AcademicProjectType.SOFTWARE_SYSTEM_DEVELOPMENT,
                "Project Report",
                "Chapter Three: System Design",
                UUID.randomUUID(),
                ReportSectionType.SYSTEM_DESIGN,
                SectionSemanticPurpose.SYSTEM_DESIGN,
                SectionGenerationPolicy.PROJECT_DERIVED_AI,
                "System Architecture & Design",
                null,
                "AgriSmart Platform",
                null,
                "Build cloud farm management system",
                List.of("Design microservices", "Implement sensor ingestion"),
                List.of("How to achieve real-time ingestion?"),
                null,
                null,
                null,
                null,
                0,
                0,
                null,
                "Specify PostgreSQL schema",
                null
        );

        SectionGenerationContext testingContext = new SectionGenerationContext(
                AcademicWorkspaceType.ACADEMIC_PROJECT,
                AcademicProjectType.SOFTWARE_SYSTEM_DEVELOPMENT,
                "Project Report",
                "Chapter Four: Testing and Evaluation",
                UUID.randomUUID(),
                ReportSectionType.TESTING,
                SectionSemanticPurpose.TESTING,
                SectionGenerationPolicy.PROJECT_EVIDENCE_REQUIRED,
                "System Testing and Evaluation",
                null,
                "AgriSmart Platform",
                null,
                null,
                List.of(),
                List.of(),
                null,
                null,
                null,
                null,
                0,
                5,
                null,
                null,
                null
        );

        SectionGenerationContext customContext = new SectionGenerationContext(
                AcademicWorkspaceType.ACADEMIC_PROJECT,
                AcademicProjectType.SOFTWARE_SYSTEM_DEVELOPMENT,
                "Project Report",
                "Chapter Six: Deployment and Maintenance",
                UUID.randomUUID(),
                ReportSectionType.CUSTOM,
                SectionSemanticPurpose.CUSTOM,
                SectionGenerationPolicy.CONTEXTUAL_AI,
                "Deployment Strategy",
                null,
                "AgriSmart Platform",
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
                "Detail Kubernetes Helm charts and CI/CD pipelines",
                null
        );

        String bgPrompt = promptService.build(bgContext);
        String problemPrompt = promptService.build(problemContext);
        String litPrompt = promptService.build(litContext);
        String methodologyPrompt = promptService.build(methodologyContext);
        String designPrompt = promptService.build(designContext);
        String testingPrompt = promptService.build(testingContext);
        String customPrompt = promptService.build(customContext);

        // Section-specific requirements
        assertThat(bgPrompt).contains("scholarly and contextual foundation");
        assertThat(bgPrompt).contains("CRITICAL SECTION PURPOSE - BACKGROUND");
        assertThat(bgPrompt).doesNotContain("comprehensive multi-paper literature synthesis");

        assertThat(problemPrompt).contains("what is currently happening");
        assertThat(problemPrompt).contains("CRITICAL SECTION PURPOSE - PROBLEM STATEMENT");
        assertThat(problemPrompt).doesNotContain("comprehensive multi-paper literature synthesis");

        assertThat(litPrompt).contains("comprehensive multi-paper literature synthesis");
        assertThat(litPrompt).contains("CRITICAL SECTION PURPOSE - LITERATURE REVIEW");
        assertThat(litPrompt).contains("agreements, disagreements, methodological patterns");

        assertThat(methodologyPrompt).contains("PROPOSED METHODOLOGY DRAFT");
        assertThat(methodologyPrompt).contains("CRITICAL SECTION PURPOSE - METHODOLOGY");

        assertThat(designPrompt).contains("CRITICAL SECTION PURPOSE - SYSTEM DESIGN & ARCHITECTURE");
        assertThat(designPrompt).contains("architecture/design information");
        assertThat(designPrompt).contains("PostgreSQL schema");

        assertThat(testingPrompt).contains("CRITICAL SECTION PURPOSE - SYSTEM TESTING");
        assertThat(testingPrompt).contains("Never invent PASS/FAIL results");

        assertThat(customPrompt).contains("CRITICAL SECTION PURPOSE - CUSTOM DOCUMENT SECTION");
        assertThat(customPrompt).contains("Deployment Strategy");
        assertThat(customPrompt).contains("Deployment and Maintenance");
        assertThat(customPrompt).contains("Kubernetes Helm charts");

        // Pairwise inequality checks
        assertThat(bgPrompt).isNotEqualTo(litPrompt);
        assertThat(designPrompt).isNotEqualTo(litPrompt);
        assertThat(problemPrompt).isNotEqualTo(bgPrompt);
        assertThat(litPrompt).isNotEqualTo(designPrompt);
        assertThat(designPrompt).isNotEqualTo(testingPrompt);
        assertThat(testingPrompt).isNotEqualTo(customPrompt);
    }

    @Test
    @DisplayName("Retrieval query is cleanly separated from generation prompt")
    void testSeparationOfRetrievalQuery() {
        SectionGenerationContext bgContext = new SectionGenerationContext(
                AcademicWorkspaceType.ACADEMIC_RESEARCH,
                AcademicProjectType.GENERAL_ACADEMIC_PROJECT,
                "Research Report",
                "Chapter One: Introduction",
                UUID.randomUUID(),
                ReportSectionType.BACKGROUND,
                SectionSemanticPurpose.BACKGROUND,
                SectionGenerationPolicy.SOURCE_GROUNDED_AI,
                "Background of the Study",
                null,
                "Digital Agriculture Adoption",
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

        SectionGenerationContext designContext = new SectionGenerationContext(
                AcademicWorkspaceType.ACADEMIC_PROJECT,
                AcademicProjectType.SOFTWARE_SYSTEM_DEVELOPMENT,
                "Project Report",
                "Chapter Three: System Design",
                UUID.randomUUID(),
                ReportSectionType.SYSTEM_DESIGN,
                SectionSemanticPurpose.SYSTEM_DESIGN,
                SectionGenerationPolicy.PROJECT_DERIVED_AI,
                "System Architecture",
                null,
                "AgriSmart Platform",
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

        String bgQuery = promptService.buildRetrievalQuery(bgContext);
        String designQuery = promptService.buildRetrievalQuery(designContext);

        assertThat(bgQuery).contains("scholarly foundation contextual evidence");
        assertThat(designQuery).contains("system design architecture component modules");
        assertThat(designQuery).doesNotContain("thematic literature review");
        assertThat(designQuery).doesNotContain("research gaps");
    }
}
