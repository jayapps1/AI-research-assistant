package com.researchassistant.analysis.service;

import com.researchassistant.analysis.dto.AnalysisDtos.GenerateSectionRequest;
import com.researchassistant.analysis.entity.ReportSectionType;
import com.researchassistant.analysis.entity.ResearchReport;
import com.researchassistant.analysis.entity.ResearchReportSection;
import com.researchassistant.analysis.entity.SectionGenerationPolicy;
import com.researchassistant.analysis.entity.SectionSemanticPurpose;
import com.researchassistant.project.entity.AcademicProjectType;
import com.researchassistant.project.entity.AcademicWorkspaceType;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

/**
 * Single source of truth for academic report section generation instructions and retrieval queries.
 * Centralizes proven section-specific guidance for both AI Assistant and Project Report AI Draft.
 */
@Service
public class AcademicSectionPromptService {

    public record StructuredFigureContext(
            UUID figureId,
            String figureLabel,
            String caption,
            String evidenceType,
            String description,
            String aiVisualAnalysis,
            String metadataJson
    ) {}

    public record SectionGenerationContext(
            AcademicWorkspaceType workspaceType,
            AcademicProjectType projectType,
            String documentType,
            String chapterTitle,
            UUID sectionId,
            ReportSectionType sectionType,
            SectionSemanticPurpose semanticPurpose,
            SectionGenerationPolicy generationPolicy,
            String sectionTitle,
            String parentSectionTitle,
            String projectTitle,
            String projectDescription,
            String researchAim,
            List<String> objectives,
            List<String> researchQuestions,
            String problemStatement,
            String studyArea,
            String researchType,
            String methodologySummary,
            int findingsCount,
            int analysisRunsCount,
            String templateContext,
            String userInstructions,
            String sourceScope,
            List<StructuredFigureContext> figures
    ) {
        public SectionGenerationContext(
                AcademicWorkspaceType workspaceType,
                AcademicProjectType projectType,
                String documentType,
                String chapterTitle,
                UUID sectionId,
                ReportSectionType sectionType,
                SectionSemanticPurpose semanticPurpose,
                SectionGenerationPolicy generationPolicy,
                String sectionTitle,
                String parentSectionTitle,
                String projectTitle,
                String projectDescription,
                String researchAim,
                List<String> objectives,
                List<String> researchQuestions,
                String problemStatement,
                String studyArea,
                String researchType,
                String methodologySummary,
                int findingsCount,
                int analysisRunsCount,
                String templateContext,
                String userInstructions,
                String sourceScope
        ) {
            this(workspaceType, projectType, documentType, chapterTitle, sectionId, sectionType,
                    semanticPurpose, generationPolicy, sectionTitle, parentSectionTitle, projectTitle,
                    projectDescription, researchAim, objectives, researchQuestions, problemStatement,
                    studyArea, researchType, methodologySummary, findingsCount, analysisRunsCount,
                    templateContext, userInstructions, sourceScope, List.of());
        }
    }

    /**
     * Builds the section-specific generation prompt and instructions.
     */
    public String build(SectionGenerationContext context) {
        StringBuilder sb = new StringBuilder();
        sb.append("============================================================\n");
        sb.append("SECTION DRAFT SPECIFICATION\n");
        sb.append("============================================================\n");
        sb.append("WORKSPACE TYPE: ").append(context.workspaceType() != null ? context.workspaceType().name() : "ACADEMIC_RESEARCH").append("\n");
        sb.append("PROJECT TYPE: ").append(context.projectType() != null ? context.projectType().name() : "GENERAL_ACADEMIC_PROJECT").append("\n");
        sb.append("DOCUMENT TYPE: ").append(valueOrDefault(context.documentType(), "REPORT")).append("\n");
        sb.append("TARGET CHAPTER: ").append(valueOrDefault(context.chapterTitle(), "Chapter")).append("\n");
        sb.append("TARGET SECTION: ").append(valueOrDefault(context.sectionTitle(), "Section")).append("\n");
        if (context.parentSectionTitle() != null && !context.parentSectionTitle().isBlank()) {
            sb.append("PARENT SECTION: ").append(context.parentSectionTitle().trim()).append("\n");
        }
        sb.append("SEMANTIC PURPOSE: ").append(context.semanticPurpose() != null ? context.semanticPurpose().name() : "CUSTOM").append("\n");
        sb.append("GENERATION POLICY: ").append(context.generationPolicy() != null ? context.generationPolicy().name() : "CONTEXTUAL_AI").append("\n\n");

        sb.append("============================================================\n");
        sb.append("PROJECT CONTEXT & METADATA\n");
        sb.append("============================================================\n");
        sb.append("Project Title: ").append(valueOrDefault(context.projectTitle(), "Academic Project")).append("\n");
        appendIfPresent(sb, "Project Description", context.projectDescription());
        appendIfPresent(sb, "Core Project Aim / Objective", context.researchAim());
        appendList(sb, "Established Objectives", context.objectives(), false);
        appendList(sb, "Research / Investigation Questions", context.researchQuestions(), true);
        appendIfPresent(sb, "Core Problem", context.problemStatement());
        appendIfPresent(sb, "Study Area", context.studyArea());
        appendIfPresent(sb, "Research Type", context.researchType());
        appendIfPresent(sb, "Methodology Profile", context.methodologySummary());
        sb.append("Stored Findings Count: ").append(context.findingsCount()).append("\n");
        sb.append("Stored Analysis/Test Run Count: ").append(context.analysisRunsCount()).append("\n\n");

        if (context.templateContext() != null && !context.templateContext().isBlank()) {
            sb.append("============================================================\n");
            sb.append("TEMPLATE REQUIREMENTS\n");
            sb.append("============================================================\n");
            sb.append("Specific Section Requirement / Guideline:\n");
            sb.append(context.templateContext().trim()).append("\n");
            sb.append("Draft specifically to satisfy these institutional / template instructions for '")
                    .append(valueOrDefault(context.sectionTitle(), "this section"))
                    .append("'. Do not substitute generic research methodology or off-topic boilerplate.\n\n");
        }

        if (context.figures() != null && !context.figures().isEmpty()) {
            sb.append("============================================================\n");
            sb.append("PROJECT FIGURES & REAL EVIDENCE\n");
            sb.append("============================================================\n");
            sb.append("The following verified project figures, screenshots, and test records are assigned to this section:\n\n");
            for (StructuredFigureContext fig : context.figures()) {
                sb.append("• ").append(fig.figureLabel() != null ? fig.figureLabel() : "Figure")
                        .append(": ").append(fig.caption() != null ? fig.caption() : "Untitled")
                        .append(" (Type: ").append(fig.evidenceType() != null ? fig.evidenceType() : "FIGURE").append(")\n");
                if (fig.description() != null && !fig.description().isBlank()) {
                    sb.append("  - Description: ").append(fig.description().trim()).append("\n");
                }
                if (fig.aiVisualAnalysis() != null && !fig.aiVisualAnalysis().isBlank()) {
                    sb.append("  - Observable Visual Elements: ").append(fig.aiVisualAnalysis().trim()).append("\n");
                }
                if (fig.metadataJson() != null && !fig.metadataJson().isBlank()) {
                    sb.append("  - Execution / Test / Table Data: ").append(fig.metadataJson().trim()).append("\n");
                }
            }
            sb.append("\nFIGURE & EVIDENCE INTEGRATION INSTRUCTIONS:\n");
            sb.append("- You MUST explicitly refer to and discuss these figures in your prose using their exact labels (e.g., 'As shown in ")
                    .append(context.figures().get(0).figureLabel() != null ? context.figures().get(0).figureLabel() : "the figure")
                    .append("...').\n");
            sb.append("- Integrate the observable interface components, workflow steps, or test outputs into the academic discussion of results/system implementation.\n");
            sb.append("- CRITICAL CONSTRAINT: Report and discuss ONLY observed or verified project evidence. Do NOT fabricate invisible backend features, encryption mechanisms, or unverified claims.\n");
            sb.append("- If test records are included, accurately summarize PASS/FAIL outcomes without inventing successful results.\n\n");
        }

        sb.append("============================================================\n");
        sb.append("PURPOSE-SPECIFIC DRAFTING INSTRUCTIONS\n");
        sb.append("============================================================\n");
        sb.append(getPurposeSpecificGuidance(context)).append("\n\n");
        sb.append(getFigurePolicyGuidance(context)).append("\n\n");

        sb.append("============================================================\n");
        sb.append("FORMATTING AND CONSTRAINTS\n");
        sb.append("============================================================\n");
        sb.append("- Write only the body prose for '").append(valueOrDefault(context.sectionTitle(), "the section")).append("'. Do not repeat the outer section or chapter title as a top-level heading.\n");
        sb.append("- Do not output preliminary pages, title page lines, table of contents, list of figures, list of tables, references, or bibliography content.\n");
        sb.append("- Write in professional, natural academic prose with varied transitions. Avoid formulaic phrases such as 'Evidence from X demonstrates...', 'Further contextual evaluation reveals...', or 'Supplementary corroborating evidence...'.\n");
        sb.append("- Do not repeat the project title in every paragraph.\n");
        sb.append("- Do not invent artificial subheadings like 'Contextual Foundation and Domain Background' unless requested by the template or user.\n");
        sb.append("- Use Markdown subheadings (### or ####) and lists only when genuine internal structure is helpful.\n");

        SectionGenerationPolicy policy = context.generationPolicy() != null
                ? context.generationPolicy()
                : (context.semanticPurpose() != null ? context.semanticPurpose().defaultPolicy() : SectionGenerationPolicy.CONTEXTUAL_AI);

        if (policy == SectionGenerationPolicy.SOURCE_GROUNDED_AI) {
            sb.append("- Ground source-derived factual claims in retrieved evidence and cite with supplied markers like [E1].\n");
            sb.append("- Do not invent authors, years, journals, DOIs, document codes, page numbers, findings, or theories.\n");
            if (context.semanticPurpose() == SectionSemanticPurpose.LITERATURE_REVIEW || context.semanticPurpose() == SectionSemanticPurpose.RESEARCH_GAP) {
                sb.append("- Synthesize across multiple sources into thematic academic narrative; compare findings, methods, agreements, disagreements, and gaps. Do not summarize sources one-by-one.\n");
            } else {
                sb.append("- Use retrieved sources only for support relevant to this section's exact purpose; do not turn this into a general literature review.\n");
            }
        } else if (policy == SectionGenerationPolicy.PROJECT_DERIVED_AI || policy == SectionGenerationPolicy.CONTEXTUAL_AI) {
            sb.append("- Strictly align with project metadata, stored objectives/questions, template context, and user instructions.\n");
            sb.append("- Do not fabricate literature, empirical results, implementation status, test outcomes, or unsupported factual claims.\n");
        } else if (policy == SectionGenerationPolicy.PROJECT_EVIDENCE_REQUIRED) {
            sb.append("- Strictly ground all statements in verified project evidence, test executions, or recorded findings.\n");
            sb.append("- Never fabricate synthetic empirical findings, pass/fail test runs, or benchmark metrics.\n");
        }

        if (context.userInstructions() != null && !context.userInstructions().isBlank()) {
            sb.append("\n============================================================\n");
            sb.append("USER-SPECIFIED INSTRUCTIONS\n");
            sb.append("============================================================\n");
            sb.append(context.userInstructions().trim()).append("\n");
            sb.append("User instructions may refine style, focus, and scope, but they do not override authorization, evidence, citation, or factual-support requirements.\n");
        }

        return sb.toString();
    }

    /**
     * Builds the retrieval query cleanly separated from the generation instruction.
     */
    public String buildRetrievalQuery(SectionGenerationContext context) {
        SectionSemanticPurpose purpose = context.semanticPurpose() != null ? context.semanticPurpose() : SectionSemanticPurpose.CUSTOM;
        StringBuilder query = new StringBuilder();
        String heading = valueOrDefault(context.sectionTitle(), "");
        String projectTitle = valueOrDefault(context.projectTitle(), "");

        switch (purpose) {
            case LITERATURE_REVIEW ->
                    query.append(projectTitle).append(" ")
                            .append(heading).append(" research objectives methodology findings limitations theory research gaps themes literature review");
            case BACKGROUND ->
                    query.append(projectTitle).append(" ")
                            .append(heading).append(" background scholarly foundation contextual evidence domain motivation");
            case PROBLEM_STATEMENT ->
                    query.append(projectTitle).append(" ")
                            .append(heading).append(" problem statement operational context practical gap evidence of problem shortcomings");
            case RESEARCH_GAP ->
                    query.append(projectTitle).append(" empirical gap methodological gap contextual gap geographical gap theoretical gap contradictions limitations");
            case THEORETICAL_FRAMEWORK ->
                    query.append(projectTitle).append(" theory theoretical framework model explanatory constructs paradigms");
            case CONCEPTUAL_FRAMEWORK ->
                    query.append(projectTitle).append(" concepts constructs variables hypothesized relationships conceptual framework");
            case OBJECTIVES ->
                    query.append(projectTitle).append(" project aim general objective specific objectives scope");
            case RESEARCH_QUESTIONS ->
                    query.append(projectTitle).append(" research questions investigation questions objectives alignment");
            case METHODOLOGY ->
                    query.append(projectTitle).append(" ").append(heading).append(" methodology research design procedure sampling tools analysis");
            case SYSTEM_REQUIREMENTS ->
                    query.append(projectTitle).append(" system requirements functional non-functional specifications constraints actors use cases");
            case SYSTEM_DESIGN ->
                    query.append(projectTitle).append(" system design architecture component modules data flow database UI process design");
            case IMPLEMENTATION ->
                    query.append(projectTitle).append(" implementation modules technology stack API libraries development code");
            case TESTING ->
                    query.append(projectTitle).append(" testing test cases execution results evaluation defects validation QA");
            case RELATED_SYSTEMS ->
                    query.append(projectTitle).append(" existing systems software platforms architecture comparison solutions");
            case DISCUSSION ->
                    query.append(projectTitle).append(" ").append(heading).append(" literature implications findings discussion");
            case CUSTOM -> {
                query.append(projectTitle);
                if (context.chapterTitle() != null && !context.chapterTitle().isBlank()) {
                    query.append(" ").append(context.chapterTitle().trim());
                }
                query.append(" ").append(heading);
            }
            default ->
                    query.append(projectTitle).append(" ").append(heading);
        }

        if (context.userInstructions() != null && !context.userInstructions().isBlank()) {
            query.append(" ").append(context.userInstructions().trim());
        }
        return query.toString().trim();
    }

    /**
     * Determines whether this section requires grounding in external source documents.
     */
    public boolean requiresSourceRetrieval(SectionGenerationContext context) {
        SectionGenerationPolicy policy = context.generationPolicy() != null
                ? context.generationPolicy()
                : (context.semanticPurpose() != null ? context.semanticPurpose().defaultPolicy() : SectionGenerationPolicy.CONTEXTUAL_AI);
        SectionSemanticPurpose purpose = context.semanticPurpose();

        return policy == SectionGenerationPolicy.SOURCE_GROUNDED_AI
                || purpose == SectionSemanticPurpose.BACKGROUND
                || purpose == SectionSemanticPurpose.PROBLEM_STATEMENT
                || purpose == SectionSemanticPurpose.RESEARCH_GAP
                || purpose == SectionSemanticPurpose.THEORETICAL_FRAMEWORK
                || purpose == SectionSemanticPurpose.DISCUSSION;
    }

    private String getPurposeSpecificGuidance(SectionGenerationContext context) {
        boolean isSoftwareProject = context.workspaceType() == AcademicWorkspaceType.ACADEMIC_PROJECT
                || context.projectType() == AcademicProjectType.SOFTWARE_SYSTEM_DEVELOPMENT;
        String projectTitle = valueOrDefault(context.projectTitle(), "the project");
        String heading = valueOrDefault(context.sectionTitle(), "Section");
        String chapterTitle = valueOrDefault(context.chapterTitle(), "Chapter");
        SectionSemanticPurpose purpose = context.semanticPurpose() != null ? context.semanticPurpose() : SectionSemanticPurpose.CUSTOM;

        return switch (purpose) {
            case BACKGROUND -> """
                    CRITICAL SECTION PURPOSE - BACKGROUND:
                    Write a proper academic background specific to the project topic. Establish a scholarly and contextual foundation, moving logically from broad contextual setting and scholarly foundation toward the specific project and problem. Use project context plus credible source evidence to explain why the domain matters. Do NOT produce artificial headings such as 'Contextual Foundation and Domain Background' unless that is the exact section title. Do not invent headings the template did not request.
                    """;
            case PROBLEM_STATEMENT -> """
                    CRITICAL SECTION PURPOSE - PROBLEM STATEMENT:
                    Write a focused Problem Statement. Contrast what is currently happening with what ought to happen, explaining the actual problem context, concrete evidence that the problem exists, the practical or research gap, affected stakeholders, and why the current project or study intervention is necessary. Do not write another literature review.
                    """;
            case OBJECTIVES -> """
                    CRITICAL SECTION PURPOSE - OBJECTIVES:
                    Formulate one primary General Objective and 3-5 Specific Objectives derived from the study topic, problem statement, and project context. Strictly preserve and use the stored project aim and objectives if provided above. Do not invent new objectives when accepted objectives already exist.
                    """;
            case RESEARCH_QUESTIONS -> """
                    CRITICAL SECTION PURPOSE - RESEARCH QUESTIONS:
                    Draft empirically answerable research or investigation questions directly derived from the topic, problem, and stored objectives. Ensure clear alignment between each specific objective and research question. Preserve existing questions if provided. Do not write thematic review prose.
                    """;
            case HYPOTHESES -> """
                    CRITICAL SECTION PURPOSE - HYPOTHESES:
                    Draft directional or null hypotheses only when the project design supports hypothesis testing. Align them with objectives, research questions, constructs, and supported theory. If the study is qualitative or exploratory, state clearly that hypotheses are Not Applicable. Do not invent variables or statistical tests that are absent from the project context.
                    """;
            case SIGNIFICANCE ->
                    "CRITICAL SECTION PURPOSE - SIGNIFICANCE:\nExplain the significance, beneficiaries, and practical or academic contributions of " + projectTitle + ". Detail specific stakeholders and anticipated real-world impacts.";
            case SCOPE ->
                    "CRITICAL SECTION PURPOSE - SCOPE:\nDefine the boundaries and delimitations of " + projectTitle + ": what is covered, what is excluded, assumptions, constraints, and the project setting.";
            case LITERATURE_REVIEW -> """
                    CRITICAL SECTION PURPOSE - LITERATURE REVIEW:
                    Provide a comprehensive multi-paper literature synthesis. Synthesize across multiple sources into thematic academic narrative; compare findings, methods, agreements, disagreements, and gaps, highlighting agreements, disagreements, methodological patterns, and conceptual themes. Do not summarize sources one-by-one. Do not repeat the project title in every paragraph. Cite source-grounded claims with supplied markers [E#].
                    """;
            case RESEARCH_GAP -> """
                    CRITICAL SECTION PURPOSE - RESEARCH GAP:
                    Identify only research gaps reasonably supported by the uploaded literature (empirical, methodological, population, geographic, theoretical, contextual, or contradictory findings). Explain how these gaps justify the current project. Do not manufacture gaps unsupported by the literature.
                    """;
            case CONCEPTUAL_FRAMEWORK -> """
                    CRITICAL SECTION PURPOSE - CONCEPTUAL FRAMEWORK:
                    Map key constructs, variables (independent, dependent, moderating/mediating), and hypothesized relationships grounded in the literature. Use relevant literature only to support constructs and relationships. Do not produce general literature-review prose and do not invent validated causal relationships.
                    """;
            case THEORETICAL_FRAMEWORK -> """
                    CRITICAL SECTION PURPOSE - THEORETICAL FRAMEWORK:
                    Synthesize theoretical paradigms and explanatory models explicitly found in the uploaded literature. Explain how each theory frames the project variables, system, or research problem. Do not invent theory citations. If literature is insufficient, state that more theoretical sources are required.
                    """;
            case RELATED_SYSTEMS ->
                    "CRITICAL SECTION PURPOSE - RELATED SYSTEMS:\nCritically review and evaluate comparable existing systems, software products, or platforms. Contrast features, architectures, strengths, and deficiencies to justify the proposed solution.";
            case METHODOLOGY -> isSoftwareProject
                    ? """
                    CRITICAL SECTION PURPOSE - METHODOLOGY:
                    Detail the actual software/project methodology, workspace type, project type, development process, requirements approach, architectural design approach, tools, and quality assurance procedures. Do not automatically generate population, sample, sampling, questionnaire, or survey methodology unless the project/template actually requires empirical research methodology.
                    """
                    : """
                    CRITICAL SECTION PURPOSE - METHODOLOGY:
                    Classify this draft clearly as a 'PROPOSED METHODOLOGY DRAFT'. Propose research design, paradigm, target population, sampling technique, and data collection approach based on study aims and literature patterns. State clearly that this proposal requires researcher review; never present proposed methods as completed fieldwork.
                    """;
            case POPULATION_SAMPLING -> """
                    CRITICAL SECTION PURPOSE - POPULATION & SAMPLING:
                    Propose target population considerations, sampling techniques, and sample-size determination approaches based on literature conventions. Do NOT fabricate actual population numbers, sample sizes, or participant counts.
                    """;
            case DATA_COLLECTION_METHOD -> """
                    CRITICAL SECTION PURPOSE - DATA COLLECTION METHODS:
                    Propose primary/secondary data collection procedures based on objectives and research design. Clearly distinguish proposed methods from completed fieldwork.
                    """;
            case RESEARCH_INSTRUMENT -> """
                    CRITICAL SECTION PURPOSE - RESEARCH INSTRUMENT:
                    Draft sample questionnaire items, interview guides, or observation protocols aligned with research questions. Do not claim instrument validation or pilot testing has occurred.
                    """;
            case SYSTEM_REQUIREMENTS ->
                    "CRITICAL SECTION PURPOSE - SYSTEM REQUIREMENTS:\nDetail actual project requirements, functional specifications, non-functional requirements, constraints, actors, and use cases. Do not use the literature-review generator as the primary strategy.";
            case SYSTEM_DESIGN -> """
                    CRITICAL SECTION PURPOSE - SYSTEM DESIGN & ARCHITECTURE:
                    Write a system/software design section using actual project requirements, system context, architectural design, component interactions, data flow, architecture/design information, and UI/process specifications. Do not force literature synthesis and do not frame this as a thematic literature review. Use relevant scholarly papers only when architectural or methodological support is appropriate.
                    """;
            case IMPLEMENTATION -> """
                    CRITICAL SECTION PURPOSE - IMPLEMENTATION:
                    Discuss implementation context, modules, technology stack, APIs, libraries, algorithms, and development decisions that are actually present in project evidence or user instructions. Never invent completed functionality.
                    """;
            case TESTING -> """
                    CRITICAL SECTION PURPOSE - SYSTEM TESTING:
                    Document actual stored test evidence, test plans, test cases, execution records, evaluation findings, and limitations. Require actual testing evidence; never invent test outcomes, PASS/FAIL counts, or coverage metrics. Never invent PASS/FAIL results without verified execution records.
                    """;
            case FINDINGS ->
                    "CRITICAL SECTION PURPOSE - FINDINGS:\nPresent and interpret actual findings, dataset results, measurements, or recorded result artifacts from the project. Ground strictly in analysis results without inventing synthetic numbers.";
            case DISCUSSION ->
                    "CRITICAL SECTION PURPOSE - DISCUSSION:\nDiscuss actual observed outcomes in relation to objectives and literature. Do not create new findings.";
            case CONCLUSIONS ->
                    "CRITICAL SECTION PURPOSE - CONCLUSION:\nConclude from actual project or research work. Address objectives and achieved outcomes without introducing new findings.";
            case RECOMMENDATIONS ->
                    "CRITICAL SECTION PURPOSE - RECOMMENDATIONS:\nProvide realistic recommendations derived from findings, limitations, and project context. Do not invent conclusions or unsupported policy claims.";
            case ABSTRACT ->
                    "CRITICAL SECTION PURPOSE - ABSTRACT:\nDraft an executive abstract summarizing the project background, problem, objectives, methodology, key findings/system capabilities, and conclusion. Do not cite external literature papers.";
            case DECLARATION, CERTIFICATION, DEDICATION, ACKNOWLEDGEMENTS ->
                    "CRITICAL SECTION PURPOSE - FRONT MATTER:\nDraft concise, formal front-matter prose appropriate for an academic report. Do not cite research papers.";
            case CUSTOM ->
                    "CRITICAL SECTION PURPOSE - CUSTOM DOCUMENT SECTION:\nWrite targeted body prose specifically addressing the actual section heading '" + heading + "' within the chapter '" + chapterTitle + "', template requirements, and project context. The actual title must materially shape the draft. Do not default to generic literature review.";
            default ->
                    "CRITICAL SECTION PURPOSE - CONTENT:\nDraft professional academic prose specifically aligned with '" + heading + "' for '" + chapterTitle + "'.";
        };
    }

    private String getFigurePolicyGuidance(SectionGenerationContext context) {
        SectionSemanticPurpose purpose = context.semanticPurpose() != null ? context.semanticPurpose() : SectionSemanticPurpose.CUSTOM;
        return switch (purpose) {
            case LITERATURE_REVIEW -> """
                    INTEGRATED FIGURE POLICY - OPTIONAL:
                    Add a figure only if it materially improves academic synthesis and the relationships are supported by the retrieved literature. Allowed figure types: CONCEPTUAL_SYNTHESIS, THEMATIC_RELATIONSHIP, THEORETICAL_MODEL. Surround the figure with cited prose explaining that it synthesizes the reviewed literature. Do not copy source figures. Do not invent unsupported relationships.
                    If a figure is justified, insert it exactly where it belongs using one fenced block:
                    ```academic_figure
                    {"figureType":"CONCEPTUAL_SYNTHESIS","title":"Short title","caption":"Academic caption without figure number","definition":"flowchart LR\\nA[Supported concept] --> B[Supported relationship]","evidenceIds":["E1","E2"]}
                    ```
                    Do not write literal figure numbers such as Figure 2.1; use the phrase "the figure" before the academic_figure block.
                    """;
            case THEORETICAL_FRAMEWORK, CONCEPTUAL_FRAMEWORK -> """
                    INTEGRATED FIGURE POLICY - OPTIONAL:
                    A theoretical or conceptual model may be generated when it clarifies constructs, variables, or relationships supported by the section evidence/context. Use an original Mermaid-style definition in an academic_figure block. Never hardcode figure numbers in prose; numbering is resolved by the document engine.
                    """;
            case METHODOLOGY, POPULATION_SAMPLING, DATA_COLLECTION_METHOD -> """
                    INTEGRATED FIGURE POLICY - OPTIONAL:
                    A research process, sampling workflow, or data-collection flowchart may be generated only when it clarifies the actual proposed method. Mark proposed steps as proposed when fieldwork is not completed. Use an academic_figure block with FLOWCHART or RESEARCH_WORKFLOW. Do not create result charts.
                    """;
            case SYSTEM_REQUIREMENTS, SYSTEM_DESIGN, RELATED_SYSTEMS -> """
                    INTEGRATED FIGURE POLICY - RECOMMENDED WHEN USEFUL:
                    Create a use-case, context, architecture, ERD, DFD, sequence, or activity figure only when it helps explain the system design in this exact section. Use an academic_figure block and keep the structured definition editable. Do not create a separate diagram workspace or standalone diagram product.
                    """;
            case IMPLEMENTATION -> """
                    INTEGRATED FIGURE POLICY - OPTIONAL:
                    Include deployment or implementation architecture only when supported by project context/evidence. Use an academic_figure block. Do not invent implemented services, integrations, or infrastructure not present in the project context.
                    """;
            case FINDINGS, DISCUSSION, TESTING -> """
                    INTEGRATED FIGURE POLICY - DATA REQUIRED:
                    Do not generate charts, heatmaps, benchmark graphs, survey percentages, coverage maps, RSSI maps, or statistical plots unless real supplied data or stored result evidence is explicitly present. Without real data, write prose only. Never fabricate empirical visuals.
                    """;
            default -> """
                    INTEGRATED FIGURE POLICY - NO FORCED FIGURE:
                    Generate prose only unless a figure would materially improve academic understanding. Any generated figure must be introduced, inserted in-place, captioned without a hardcoded number, and explained immediately after.
                    """;
        };
    }

    private static String valueOrDefault(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    private static void appendIfPresent(StringBuilder sb, String label, String value) {
        if (value != null && !value.isBlank()) {
            sb.append(label).append(": ").append(value.trim()).append("\n");
        }
    }

    private static void appendList(StringBuilder sb, String label, List<String> values, boolean questionLabels) {
        if (values == null || values.isEmpty()) {
            return;
        }
        sb.append(label).append(":\n");
        for (int i = 0; i < values.size(); i++) {
            sb.append("  ").append(questionLabels ? "Q" + (i + 1) + ": " : (i + 1) + ". ")
                    .append(values.get(i))
                    .append("\n");
        }
    }
}
