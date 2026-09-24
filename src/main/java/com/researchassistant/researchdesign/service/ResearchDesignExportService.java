package com.researchassistant.researchdesign.service;

import com.researchassistant.analysis.entity.CitationStyle;
import com.researchassistant.common.exception.ResourceNotFoundException;
import com.researchassistant.identity.entity.User;
import com.researchassistant.methodology.entity.*;
import com.researchassistant.methodology.repository.*;
import com.researchassistant.project.entity.ResearchProject;
import com.researchassistant.project.repository.ResearchProjectRepository;
import com.researchassistant.project.service.ProjectAuthorizationService;
import com.researchassistant.reference.dto.ReferenceDtos.CitationContext;
import com.researchassistant.reference.entity.ProjectReference;
import com.researchassistant.reference.entity.ProjectReferenceStatus;
import com.researchassistant.reference.entity.ReferenceAuthor;
import com.researchassistant.reference.entity.ReferenceEntry;
import com.researchassistant.reference.repository.ProjectReferenceRepository;
import com.researchassistant.reference.repository.ReferenceAuthorRepository;
import com.researchassistant.reference.service.CitationFormattingService;
import com.researchassistant.researchdesign.entity.ResearchHypothesis;
import com.researchassistant.researchdesign.entity.ResearchObjective;
import com.researchassistant.researchdesign.entity.ResearchProblem;
import com.researchassistant.researchdesign.entity.ResearchQuestion;
import com.researchassistant.researchdesign.repository.ResearchHypothesisRepository;
import com.researchassistant.researchdesign.repository.ResearchObjectiveRepository;
import com.researchassistant.researchdesign.repository.ResearchProblemRepository;
import com.researchassistant.researchdesign.repository.ResearchQuestionRepository;
import org.apache.poi.xwpf.usermodel.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class ResearchDesignExportService {

    private final ResearchProjectRepository projectRepository;
    private final ProjectAuthorizationService authorizationService;
    private final ResearchProblemRepository problemRepository;
    private final ResearchObjectiveRepository objectiveRepository;
    private final ResearchQuestionRepository questionRepository;
    private final ResearchHypothesisRepository hypothesisRepository;
    private final MethodologyRepository methodologyRepository;
    private final StudyPopulationRepository populationRepository;
    private final SamplingPlanRepository samplingPlanRepository;
    private final DataCollectionMethodRepository dataMethodRepository;
    private final ProjectReferenceRepository projectReferenceRepository;
    private final ReferenceAuthorRepository authorRepository;
    private final CitationFormattingService citationFormattingService;

    public ResearchDesignExportService(
            ResearchProjectRepository projectRepository,
            ProjectAuthorizationService authorizationService,
            ResearchProblemRepository problemRepository,
            ResearchObjectiveRepository objectiveRepository,
            ResearchQuestionRepository questionRepository,
            ResearchHypothesisRepository hypothesisRepository,
            MethodologyRepository methodologyRepository,
            StudyPopulationRepository populationRepository,
            SamplingPlanRepository samplingPlanRepository,
            DataCollectionMethodRepository dataMethodRepository,
            ProjectReferenceRepository projectReferenceRepository,
            ReferenceAuthorRepository authorRepository,
            CitationFormattingService citationFormattingService
    ) {
        this.projectRepository = projectRepository;
        this.authorizationService = authorizationService;
        this.problemRepository = problemRepository;
        this.objectiveRepository = objectiveRepository;
        this.questionRepository = questionRepository;
        this.hypothesisRepository = hypothesisRepository;
        this.methodologyRepository = methodologyRepository;
        this.populationRepository = populationRepository;
        this.samplingPlanRepository = samplingPlanRepository;
        this.dataMethodRepository = dataMethodRepository;
        this.projectReferenceRepository = projectReferenceRepository;
        this.authorRepository = authorRepository;
        this.citationFormattingService = citationFormattingService;
    }

    public record ExportResult(String filename, String contentType, byte[] content) {}

    @Transactional(readOnly = true)
    public ExportResult exportResearchDesign(UUID projectId, String format, User user) {
        authorizationService.requireProjectViewer(projectId, user);
        ResearchProject project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found."));

        String safeTitle = (project.getTitle() == null ? "research_protocol" : project.getTitle().toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "_")).trim();
        if (safeTitle.length() > 40) safeTitle = safeTitle.substring(0, 40);

        if ("markdown".equalsIgnoreCase(format) || "md".equalsIgnoreCase(format)) {
            byte[] md = buildMarkdown(project);
            return new ExportResult(safeTitle + "_protocol.md", "text/markdown; charset=utf-8", md);
        } else {
            byte[] docx = buildDocx(project);
            return new ExportResult(safeTitle + "_protocol.docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document", docx);
        }
    }

    private byte[] buildDocx(ResearchProject project) {
        UUID projectId = project.getId();
        List<ResearchObjective> objectives = objectiveRepository.findAllByProjectId(projectId).stream()
                .sorted(Comparator.comparingInt(ResearchObjective::getDisplayOrder)).toList();
        List<ResearchQuestion> questions = questionRepository.findAllByProjectId(projectId).stream()
                .sorted(Comparator.comparingInt(ResearchQuestion::getDisplayOrder)).toList();
        List<ResearchHypothesis> hypotheses = hypothesisRepository.findAllByProjectId(projectId).stream()
                .sorted(Comparator.comparing(ResearchHypothesis::getCreatedAt, Comparator.nullsLast(Comparator.naturalOrder()))).toList();
        String problem = problemRepository.findAllByProjectId(projectId).stream()
                .max(Comparator.comparing(ResearchProblem::getCreatedAt, Comparator.nullsLast(Comparator.naturalOrder())))
                .map(ResearchProblem::getStatement)
                .orElse(project.getDescription() != null ? project.getDescription() : "");
        List<Methodology> methodologies = methodologyRepository.findAllByProjectIdOrderByRevisionNumberDesc(projectId);
        Methodology methodology = methodologies.isEmpty() ? null : methodologies.get(0);
        List<ProjectReference> references = projectReferenceRepository.findAllByProjectIdAndStatusOrderByCitationKeyAsc(projectId, ProjectReferenceStatus.ACTIVE).stream()
                .filter(ProjectReference::isAvailableForResearchAi)
                .toList();

        try (XWPFDocument doc = new XWPFDocument(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            // Header / Title Block
            addHeading(doc, "RESEARCH PROTOCOL & METHODOLOGY DOSSIER", 1, false, 20, true);
            addParagraph(doc, project.getTitle() != null ? project.getTitle() : "Untitled Research Project", ParagraphAlignment.LEFT, true, 14, false);
            addParagraph(doc, "Generated: " + OffsetDateTime.now().format(DateTimeFormatter.ofPattern("dd MMMM yyyy HH:mm")) + " | Research Type: " + (project.getResearchType() != null ? project.getResearchType() : "Standard Academic Research"), ParagraphAlignment.LEFT, false, 10, true);

            addDivider(doc);

            // SECTION 1: PROBLEM STATEMENT & AIMS
            addHeading(doc, "1. Research Conceptualization & Problem Formulation", 2, false, 14, true);
            if (project.getResearchAim() != null && !project.getResearchAim().isBlank()) {
                addParagraph(doc, "General Aim / Overarching Purpose:", ParagraphAlignment.LEFT, true, 11, false);
                addParagraph(doc, project.getResearchAim(), ParagraphAlignment.LEFT, false, 11, false);
            }
            if (project.getStudyArea() != null && !project.getStudyArea().isBlank()) {
                addParagraph(doc, "Study Setting / Domain Context:", ParagraphAlignment.LEFT, true, 11, false);
                addParagraph(doc, project.getStudyArea(), ParagraphAlignment.LEFT, false, 11, false);
            }
            addParagraph(doc, "Problem Statement:", ParagraphAlignment.LEFT, true, 11, false);
            addParagraph(doc, problem.isBlank() ? "Problem statement has not been formally specified." : problem, ParagraphAlignment.LEFT, false, 11, false);

            // SECTION 2: OBJECTIVES, QUESTIONS, HYPOTHESES
            addHeading(doc, "2. Objectives, Research Questions & Hypotheses", 2, false, 14, true);
            addParagraph(doc, "Specific Objectives:", ParagraphAlignment.LEFT, true, 11, false);
            if (objectives.isEmpty()) {
                addParagraph(doc, "• Objectives pending formulation.", ParagraphAlignment.LEFT, false, 11, false);
            } else {
                for (int i = 0; i < objectives.size(); i++) {
                    addParagraph(doc, (i + 1) + ". " + objectives.get(i).getText(), ParagraphAlignment.LEFT, false, 11, false);
                }
            }

            addParagraph(doc, "Research Questions:", ParagraphAlignment.LEFT, true, 11, false);
            if (questions.isEmpty()) {
                addParagraph(doc, "• Research questions pending formulation.", ParagraphAlignment.LEFT, false, 11, false);
            } else {
                for (int i = 0; i < questions.size(); i++) {
                    addParagraph(doc, "RQ" + (i + 1) + ": " + questions.get(i).getText(), ParagraphAlignment.LEFT, false, 11, false);
                }
            }

            if (!hypotheses.isEmpty()) {
                addParagraph(doc, "Research Hypotheses / Technical Propositions:", ParagraphAlignment.LEFT, true, 11, false);
                for (int i = 0; i < hypotheses.size(); i++) {
                    addParagraph(doc, "H" + (i + 1) + ": " + hypotheses.get(i).getText(), ParagraphAlignment.LEFT, false, 11, false);
                }
            }

            // SECTION 3: METHODOLOGICAL FRAMEWORK & DESIGN
            addHeading(doc, "3. Methodological Framework & Empirical Design", 2, false, 14, true);
            if (methodology != null) {
                addParagraph(doc, "Methodological Paradigm / Approach: " + methodology.getApproach(), ParagraphAlignment.LEFT, true, 11, false);
                addParagraph(doc, "Research Design Type: " + methodology.getDesignType(), ParagraphAlignment.LEFT, true, 11, false);
                if (methodology.getDesignDescription() != null && !methodology.getDesignDescription().isBlank()) {
                    addParagraph(doc, "Design Narrative & Operational Workflow:", ParagraphAlignment.LEFT, true, 11, false);
                    addParagraph(doc, methodology.getDesignDescription(), ParagraphAlignment.LEFT, false, 11, false);
                }
                if (methodology.getStudySetting() != null && !methodology.getStudySetting().isBlank()) {
                    addParagraph(doc, "Study Setting & Geographic Scope: " + methodology.getStudySetting(), ParagraphAlignment.LEFT, false, 11, false);
                }
                if (methodology.getRationale() != null && !methodology.getRationale().isBlank()) {
                    addParagraph(doc, "Methodological Rationale: " + methodology.getRationale(), ParagraphAlignment.LEFT, false, 11, false);
                }

                // Population & Sampling
                List<StudyPopulation> populations = populationRepository.findAllByMethodologyId(methodology.getId());
                if (!populations.isEmpty()) {
                    StudyPopulation pop = populations.get(0);
                    addParagraph(doc, "Target Population: " + pop.getTargetPopulationDescription() + (pop.getTargetPopulationSize() != null ? " (N ≈ " + pop.getTargetPopulationSize() + ")" : ""), ParagraphAlignment.LEFT, false, 11, false);
                    if (pop.getInclusionCriteria() != null) addParagraph(doc, "Inclusion Criteria: " + pop.getInclusionCriteria(), ParagraphAlignment.LEFT, false, 10, false);
                }

                List<SamplingPlan> samplingPlans = samplingPlanRepository.findAllByMethodologyId(methodology.getId());
                if (!samplingPlans.isEmpty()) {
                    SamplingPlan sp = samplingPlans.get(0);
                    addParagraph(doc, "Sampling Strategy: " + sp.getTechnique() + " (Sample Size: " + (sp.getPlannedSampleSize() != null ? sp.getPlannedSampleSize() : "TBD") + ")", ParagraphAlignment.LEFT, false, 11, false);
                    if (sp.getRationale() != null) addParagraph(doc, "Sampling Rationale: " + sp.getRationale(), ParagraphAlignment.LEFT, false, 10, false);
                }

                List<DataCollectionMethod> dataMethods = dataMethodRepository.findAllByMethodologyIdOrderByDisplayOrderAsc(methodology.getId());
                if (!dataMethods.isEmpty()) {
                    addParagraph(doc, "Data Collection Protocols & Instruments:", ParagraphAlignment.LEFT, true, 11, false);
                    for (DataCollectionMethod dm : dataMethods) {
                        addParagraph(doc, "• " + dm.getName() + " (" + dm.getType() + "): " + (dm.getDescription() != null ? dm.getDescription() : "Standard academic administration"), ParagraphAlignment.LEFT, false, 10, false);
                    }
                }
            } else {
                addParagraph(doc, "Detailed methodological specifications can be completed in the Research Design workspace.", ParagraphAlignment.LEFT, false, 11, false);
            }

            // SECTION 4: NUMBERED REFERENCES
            addHeading(doc, "4. Scholarly References & Evidence Base", 2, true, 14, true);
            if (references.isEmpty()) {
                addParagraph(doc, "No source papers currently registered in the project reference library.", ParagraphAlignment.LEFT, false, 11, false);
            } else {
                int ordinal = 1;
                for (ProjectReference pr : references) {
                    ReferenceEntry entry = pr.getReference();
                    String citationStr = formatReferenceEntry(ordinal++, pr, entry, project.getCitationStyle());
                    addParagraph(doc, citationStr, ParagraphAlignment.LEFT, false, 10, false);
                }
            }

            doc.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException("Failed to generate DOCX research protocol: " + e.getMessage(), e);
        }
    }

    private byte[] buildMarkdown(ResearchProject project) {
        UUID projectId = project.getId();
        List<ResearchObjective> objectives = objectiveRepository.findAllByProjectId(projectId).stream()
                .sorted(Comparator.comparingInt(ResearchObjective::getDisplayOrder)).toList();
        List<ResearchQuestion> questions = questionRepository.findAllByProjectId(projectId).stream()
                .sorted(Comparator.comparingInt(ResearchQuestion::getDisplayOrder)).toList();
        List<ResearchHypothesis> hypotheses = hypothesisRepository.findAllByProjectId(projectId).stream()
                .sorted(Comparator.comparing(ResearchHypothesis::getCreatedAt, Comparator.nullsLast(Comparator.naturalOrder()))).toList();
        String problem = problemRepository.findAllByProjectId(projectId).stream()
                .max(Comparator.comparing(ResearchProblem::getCreatedAt, Comparator.nullsLast(Comparator.naturalOrder())))
                .map(ResearchProblem::getStatement)
                .orElse(project.getDescription() != null ? project.getDescription() : "");
        List<Methodology> methodologies = methodologyRepository.findAllByProjectIdOrderByRevisionNumberDesc(projectId);
        Methodology methodology = methodologies.isEmpty() ? null : methodologies.get(0);
        List<ProjectReference> references = projectReferenceRepository.findAllByProjectIdAndStatusOrderByCitationKeyAsc(projectId, ProjectReferenceStatus.ACTIVE).stream()
                .filter(ProjectReference::isAvailableForResearchAi)
                .toList();

        StringBuilder sb = new StringBuilder();
        sb.append("# RESEARCH PROTOCOL & METHODOLOGY SPECIFICATION\n\n");
        sb.append("## ").append(project.getTitle() != null ? project.getTitle() : "Untitled Research Project").append("\n\n");
        sb.append("**Generated:** ").append(OffsetDateTime.now().format(DateTimeFormatter.ofPattern("dd MMMM yyyy HH:mm"))).append("\n");
        sb.append("**Research Type:** `").append(project.getResearchType() != null ? project.getResearchType() : "Standard Academic Research").append("`\n\n");
        sb.append("---\n\n");

        sb.append("### 1. Research Conceptualization & Problem Formulation\n\n");
        if (project.getResearchAim() != null && !project.getResearchAim().isBlank()) {
            sb.append("**General Aim / Overarching Purpose:**\n").append(project.getResearchAim()).append("\n\n");
        }
        if (project.getStudyArea() != null && !project.getStudyArea().isBlank()) {
            sb.append("**Study Setting / Domain Context:**\n").append(project.getStudyArea()).append("\n\n");
        }
        sb.append("**Problem Statement:**\n").append(problem.isBlank() ? "Problem statement has not been formally specified." : problem).append("\n\n");

        sb.append("### 2. Objectives, Research Questions & Hypotheses\n\n");
        sb.append("#### Specific Objectives\n");
        if (objectives.isEmpty()) {
            sb.append("- Objectives pending formulation.\n\n");
        } else {
            for (int i = 0; i < objectives.size(); i++) {
                sb.append(i + 1).append(". ").append(objectives.get(i).getText()).append("\n");
            }
            sb.append("\n");
        }

        sb.append("#### Research Questions\n");
        if (questions.isEmpty()) {
            sb.append("- Research questions pending formulation.\n\n");
        } else {
            for (int i = 0; i < questions.size(); i++) {
                sb.append("- **RQ").append(i + 1).append(":** ").append(questions.get(i).getText()).append("\n");
            }
            sb.append("\n");
        }

        if (!hypotheses.isEmpty()) {
            sb.append("#### Hypotheses / Technical Propositions\n");
            for (int i = 0; i < hypotheses.size(); i++) {
                sb.append("- **H").append(i + 1).append(":** ").append(hypotheses.get(i).getText()).append("\n");
            }
            sb.append("\n");
        }

        sb.append("### 3. Methodological Framework & Empirical Design\n\n");
        if (methodology != null) {
            sb.append("- **Approach:** ").append(methodology.getApproach()).append("\n");
            sb.append("- **Design Type:** ").append(methodology.getDesignType()).append("\n");
            if (methodology.getDesignDescription() != null && !methodology.getDesignDescription().isBlank()) {
                sb.append("\n**Design Narrative:**\n").append(methodology.getDesignDescription()).append("\n\n");
            }
            if (methodology.getStudySetting() != null && !methodology.getStudySetting().isBlank()) {
                sb.append("- **Study Setting:** ").append(methodology.getStudySetting()).append("\n");
            }
            if (methodology.getRationale() != null && !methodology.getRationale().isBlank()) {
                sb.append("- **Methodological Rationale:** ").append(methodology.getRationale()).append("\n");
            }
            sb.append("\n");
        }

        sb.append("### 4. Scholarly References & Evidence Base\n\n");
        if (references.isEmpty()) {
            sb.append("No source papers currently registered in the project reference library.\n");
        } else {
            int ordinal = 1;
            for (ProjectReference pr : references) {
                ReferenceEntry entry = pr.getReference();
                sb.append(formatReferenceEntry(ordinal++, pr, entry, project.getCitationStyle())).append("\n\n");
            }
        }

        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    private String formatReferenceEntry(int ordinal, ProjectReference pr, ReferenceEntry entry, CitationStyle style) {
        try {
            CitationStyle targetStyle = style != null ? style : CitationStyle.IEEE;
            var formatted = citationFormattingService.format(entry, targetStyle, CitationContext.REFERENCE_LIST, ordinal);
            if (formatted != null && formatted.text() != null && !formatted.text().isBlank()) {
                return formatted.text();
            }
        } catch (Exception ignored) {
            // Fallback manual formatting
        }

        List<ReferenceAuthor> authors = authorRepository.findAllByReferenceIdOrderByDisplayOrderAsc(entry.getId());
        StringBuilder authorStr = new StringBuilder();
        if (!authors.isEmpty()) {
            for (int i = 0; i < authors.size(); i++) {
                if (i > 0) authorStr.append(", ");
                ReferenceAuthor a = authors.get(i);
                if (a.getFamilyName() != null && a.getGivenName() != null) {
                    authorStr.append(a.getFamilyName()).append(", ").append(a.getGivenName().charAt(0)).append(".");
                } else if (a.getLiteralName() != null) {
                    authorStr.append(a.getLiteralName());
                }
            }
        } else {
            authorStr.append("Unknown Author");
        }

        String yearStr = entry.getPublicationYear() != null ? "(" + entry.getPublicationYear() + ")" : "(n.d.)";
        String titleStr = entry.getTitle() != null ? entry.getTitle() : "Untitled";
        String container = entry.getContainerTitle() != null ? entry.getContainerTitle() : (entry.getPublisher() != null ? entry.getPublisher() : "");
        String doiStr = entry.getDoi() != null ? " https://doi.org/" + entry.getDoi() : "";

        return "[" + ordinal + "] " + authorStr + " " + yearStr + ". " + titleStr + (container.isBlank() ? "" : ". " + container) + doiStr;
    }

    private void addHeading(XWPFDocument doc, String text, int level, boolean pageBreak, int fontSize, boolean bold) {
        XWPFParagraph p = doc.createParagraph();
        if (pageBreak) p.setPageBreak(true);
        p.setSpacingBefore(200);
        p.setSpacingAfter(100);
        XWPFRun r = p.createRun();
        r.setFontFamily("Calibri");
        r.setFontSize(fontSize);
        r.setBold(bold);
        r.setColor(level == 1 ? "1E3A8A" : "2563EB");
        r.setText(text);
    }

    private void addParagraph(XWPFDocument doc, String text, ParagraphAlignment align, boolean bold, int fontSize, boolean italic) {
        XWPFParagraph p = doc.createParagraph();
        p.setAlignment(align);
        p.setSpacingAfter(80);
        XWPFRun r = p.createRun();
        r.setFontFamily("Calibri");
        r.setFontSize(fontSize);
        r.setBold(bold);
        r.setItalic(italic);
        r.setText(text);
    }

    private void addDivider(XWPFDocument doc) {
        XWPFParagraph p = doc.createParagraph();
        p.setBorderBottom(Borders.SINGLE);
        p.setSpacingAfter(150);
    }
}
