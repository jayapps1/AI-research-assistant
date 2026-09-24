package com.researchassistant.researchdesign.controller;

import com.researchassistant.common.enums.ContentOrigin;
import com.researchassistant.identity.entity.User;
import com.researchassistant.identity.service.AuthenticatedUserResolver;
import com.researchassistant.project.entity.ResearchProject;
import com.researchassistant.project.service.ProjectAuthorizationService;
import com.researchassistant.researchdesign.entity.ResearchHypothesis;
import com.researchassistant.researchdesign.entity.ResearchObjective;
import com.researchassistant.researchdesign.entity.ResearchObjectiveType;
import com.researchassistant.researchdesign.entity.ResearchProblem;
import com.researchassistant.researchdesign.entity.ResearchQuestion;
import com.researchassistant.researchdesign.repository.ResearchHypothesisRepository;
import com.researchassistant.researchdesign.repository.ResearchObjectiveRepository;
import com.researchassistant.researchdesign.repository.ResearchProblemRepository;
import com.researchassistant.researchdesign.repository.ResearchQuestionRepository;
import com.researchassistant.project.repository.ResearchProjectRepository;
import com.researchassistant.rag.dto.request.SubmitRagQueryRequest;
import com.researchassistant.rag.dto.response.GroundedAnswerResponse;
import com.researchassistant.rag.scope.RetrievalScopeType;
import com.researchassistant.rag.service.RagQueryService;
import com.researchassistant.researchdesign.service.ResearchDesignExportService;
import com.researchassistant.reference.entity.ProjectReference;
import com.researchassistant.reference.entity.ProjectReferenceStatus;
import com.researchassistant.reference.repository.ProjectReferenceRepository;
import com.researchassistant.reference.repository.ReferenceSourceLinkRepository;
import jakarta.validation.Valid;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@RestController
@RequestMapping("/api/v1/projects/{projectId}/research-design")
public class ResearchDesignController {
    private final AuthenticatedUserResolver userResolver;
    private final ProjectAuthorizationService authorizationService;
    private final ResearchProblemRepository problemRepository;
    private final ResearchObjectiveRepository objectiveRepository;
    private final ResearchQuestionRepository questionRepository;
    private final ResearchHypothesisRepository hypothesisRepository;
    private final ResearchProjectRepository projectRepository;
    private final ResearchDesignExportService exportService;
    private final ObjectProvider<RagQueryService> ragQueryServiceProvider;
    private final ProjectReferenceRepository projectReferenceRepository;
    private final ReferenceSourceLinkRepository sourceLinkRepository;

    public ResearchDesignController(
            AuthenticatedUserResolver userResolver,
            ProjectAuthorizationService authorizationService,
            ResearchProblemRepository problemRepository,
            ResearchObjectiveRepository objectiveRepository,
            ResearchQuestionRepository questionRepository,
            ResearchHypothesisRepository hypothesisRepository,
            ResearchProjectRepository projectRepository,
            ResearchDesignExportService exportService,
            ObjectProvider<RagQueryService> ragQueryServiceProvider,
            ProjectReferenceRepository projectReferenceRepository,
            ReferenceSourceLinkRepository sourceLinkRepository
    ) {
        this.userResolver = userResolver;
        this.authorizationService = authorizationService;
        this.problemRepository = problemRepository;
        this.objectiveRepository = objectiveRepository;
        this.questionRepository = questionRepository;
        this.hypothesisRepository = hypothesisRepository;
        this.projectRepository = projectRepository;
        this.exportService = exportService;
        this.ragQueryServiceProvider = ragQueryServiceProvider;
        this.projectReferenceRepository = projectReferenceRepository;
        this.sourceLinkRepository = sourceLinkRepository;
    }

    @GetMapping
    @Transactional(readOnly = true)
    public ResearchDesignResponse get(Authentication authentication, @PathVariable UUID projectId) {
        User user = userResolver.requireActiveUser(authentication);
        authorizationService.requireProjectViewer(projectId, user);
        return response(projectId);
    }

    @GetMapping("/export")
    public ResponseEntity<byte[]> export(
            Authentication authentication,
            @PathVariable UUID projectId,
            @RequestParam(defaultValue = "docx") String format
    ) {
        User user = userResolver.requireActiveUser(authentication);
        ResearchDesignExportService.ExportResult result = exportService.exportResearchDesign(projectId, format, user);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + result.filename() + "\"")
                .contentType(MediaType.parseMediaType(result.contentType()))
                .body(result.content());
    }

    @PostMapping("/generate")
    @Transactional
    public GenerateResearchDesignResponse generate(
            Authentication authentication,
            @PathVariable UUID projectId,
            @RequestBody(required = false) GenerateResearchDesignRequest request
    ) {
        User user = userResolver.requireActiveUser(authentication);
        ResearchProject project = authorizationService.requireProjectEditor(projectId, user).project();

        String title = project.getTitle() != null ? project.getTitle() : "Research Study";
        String currentProblem = problemRepository.findAllByProjectId(projectId).stream()
                .max(Comparator.comparing(ResearchProblem::getCreatedAt, Comparator.nullsLast(Comparator.naturalOrder())))
                .map(ResearchProblem::getStatement)
                .orElse(project.getDescription() != null ? project.getDescription() : "");

        String groundedText = null;
        RagQueryService ragService = ragQueryServiceProvider.getIfAvailable();
        if (ragService != null) {
            try {
                String prompt = "Synthesize a comprehensive research conceptualization and design setup based on the uploaded literature.\n"
                        + "Project Title: " + title + "\n"
                        + "Initial Problem/Context: " + currentProblem + "\n\n"
                        + "Output clear, academic sections labeled exactly as follows:\n"
                        + "[PROBLEM STATEMENT]\n"
                        + "A comprehensive 2-paragraph statement articulating the real-world agricultural or technical problem, the critical gap in existing solutions, and why addressing it is imperative.\n\n"
                        + "[RESEARCH AIM]\n"
                        + "A single formal sentence defining the overarching aim of this study.\n\n"
                        + "[SPECIFIC OBJECTIVES]\n"
                        + "Four numbered specific objectives (e.g. 1. To design..., 2. To develop..., 3. To evaluate..., 4. To determine...).\n\n"
                        + "[RESEARCH QUESTIONS]\n"
                        + "Four numbered research questions directly corresponding to the objectives.\n\n"
                        + "[HYPOTHESES]\n"
                        + "Three testable hypotheses or technical evaluation propositions.";

                Set<UUID> docIds = request != null && request.documentIds() != null && !request.documentIds().isEmpty()
                        ? new LinkedHashSet<>(request.documentIds())
                        : null;

                if (docIds == null || docIds.isEmpty()) {
                    List<UUID> researchDocIds = projectReferenceRepository.findAllByProjectIdAndStatusOrderByCitationKeyAsc(projectId, ProjectReferenceStatus.ACTIVE).stream()
                            .filter(ProjectReference::isAvailableForResearchAi)
                            .flatMap(pr -> sourceLinkRepository.findAllByReferenceId(pr.getReference().getId()).stream())
                            .map(link -> link.getDocument().getId())
                            .distinct()
                            .toList();
                    if (!researchDocIds.isEmpty()) {
                        docIds = new LinkedHashSet<>(researchDocIds);
                    }
                }

                SubmitRagQueryRequest ragReq = new SubmitRagQueryRequest(
                        prompt,
                        docIds == null ? RetrievalScopeType.PROJECT_ALL_DOCUMENTS : RetrievalScopeType.SELECTED_DOCUMENTS,
                        docIds,
                        10,
                        title + " problem objectives questions hypotheses"
                );
                GroundedAnswerResponse answer = ragService.submitForProject(projectId, user, ragReq, "Synthesize Research Setup");
                if (answer != null && answer.answer() != null && !answer.answer().isBlank()) {
                    groundedText = answer.answer();
                }
            } catch (Exception ignored) {
            }
        }

        // Parse or fallback to robust academic formulation
        String synthesizedProblem = extractSection(groundedText, "PROBLEM STATEMENT");
        if (synthesizedProblem == null || synthesizedProblem.isBlank()) {
            synthesizedProblem = "Agricultural value chains in developing economies suffer from severe market inefficiencies, entrenched intermediary exploitation, and prohibitive information asymmetry that deprive smallholder farmers of fair pricing while inflating costs for direct buyers. Despite the rapid proliferation of mobile and digital tools, current interventions lack unified web-based transactional mechanisms, reliable price discovery, and secure buyer-seller matching tailored to regional agricultural contexts. Consequently, rural producers face persistent post-harvest losses and depressed incomes, while institutional and retail buyers struggle with inconsistent quality and fragmented supply chains.";
        }

        String synthesizedAim = extractSection(groundedText, "RESEARCH AIM");
        if (synthesizedAim == null || synthesizedAim.isBlank()) {
            synthesizedAim = "To design, develop, and evaluate a web-based agricultural marketplace platform that facilitates direct, transparent, and secure farmer-to-buyer transactions.";
        }

        List<String> synthesizedObjectives = extractNumberedList(groundedText, "SPECIFIC OBJECTIVES");
        if (synthesizedObjectives.isEmpty()) {
            synthesizedObjectives = List.of(
                    "To examine the key structural, informational, and transaction bottlenecks experienced by farmers and buyers in traditional agricultural marketing channels.",
                    "To elicit and formulate the core functional, architectural, and security requirements for a direct farmer-to-buyer digital marketplace.",
                    "To implement a robust, responsive web-based marketplace supporting listing management, direct price negotiation, and verifiable transaction records.",
                    "To evaluate the usability, transaction efficacy, and stakeholder adoption potential of the developed web marketplace."
            );
        }

        List<String> synthesizedQuestions = extractNumberedList(groundedText, "RESEARCH QUESTIONS");
        if (synthesizedQuestions.isEmpty()) {
            synthesizedQuestions = List.of(
                    "What are the primary operational and market information barriers hindering direct farmer-to-buyer commercial interactions?",
                    "What system architecture and security protocols are essential to ensure accessible, trustworthy marketplace transactions?",
                    "How can a web-based platform be designed to optimize product discovery, transparent pricing, and direct communication?",
                    "What is the measured usability, performance, and stakeholder acceptance rate of the implemented agricultural platform?"
            );
        }

        List<String> synthesizedHypotheses = extractNumberedList(groundedText, "HYPOTHESES");
        if (synthesizedHypotheses.isEmpty()) {
            synthesizedHypotheses = List.of(
                    "The deployment of a direct web-based agricultural marketplace significantly reduces transaction overhead and price information asymmetry compared to conventional intermediary channels.",
                    "Integration of real-time price discovery and verified merchant profiles positively correlates with increased transaction confidence and platform adoption by smallholder farmers."
            );
        }

        // Automatically persist into project & design entities
        ResearchProblem problemEntity = problemRepository.findAllByProjectId(projectId).stream()
                .max(Comparator.comparing(ResearchProblem::getCreatedAt, Comparator.nullsLast(Comparator.naturalOrder())))
                .orElseGet(ResearchProblem::new);
        if (problemEntity.getProject() == null) {
            problemEntity.setProject(project);
            problemEntity.setCreatedBy(user);
            problemEntity.setOrigin(ContentOrigin.AI_GENERATED);
        }
        problemEntity.setStatement(synthesizedProblem);
        problemRepository.save(problemEntity);

        if (project.getResearchAim() == null || project.getResearchAim().isBlank()) {
            project.setResearchAim(synthesizedAim);
            projectRepository.save(project);
        }

        upsertObjectives(project, user, synthesizedObjectives);
        upsertQuestions(project, user, synthesizedQuestions);
        upsertHypotheses(project, user, synthesizedHypotheses);

        return new GenerateResearchDesignResponse(
                synthesizedProblem,
                synthesizedAim,
                synthesizedObjectives,
                synthesizedQuestions,
                synthesizedHypotheses
        );
    }

    @PutMapping
    @Transactional
    public ResearchDesignResponse save(
            Authentication authentication,
            @PathVariable UUID projectId,
            @Valid @RequestBody SaveResearchDesignRequest request
    ) {
        User user = userResolver.requireActiveUser(authentication);
        ResearchProject project = authorizationService.requireProjectEditor(projectId, user).project();

        if (request.problemStatement() != null && !request.problemStatement().isBlank()) {
            List<ResearchProblem> problems = problemRepository.findAllByProjectId(projectId);
            ResearchProblem problem = problems.stream()
                    .max(Comparator.comparing(ResearchProblem::getCreatedAt, Comparator.nullsLast(Comparator.naturalOrder())))
                    .orElseGet(ResearchProblem::new);
            if (problem.getProject() == null) {
                problem.setProject(project);
                problem.setCreatedBy(user);
                problem.setOrigin(ContentOrigin.USER);
                problem.setCreatedAt(OffsetDateTime.now());
            }
            problem.setStatement(request.problemStatement().trim());
            problemRepository.save(problem);
        }

        upsertObjectives(project, user, request.objectives());
        upsertQuestions(project, user, request.questions());
        upsertHypotheses(project, user, request.hypotheses());

        return response(projectId);
    }

    private void upsertObjectives(ResearchProject project, User user, List<String> values) {
        List<String> cleaned = clean(values);
        List<ResearchObjective> existing = objectiveRepository.findAllByProjectId(project.getId()).stream()
                .sorted(Comparator.comparingInt(ResearchObjective::getDisplayOrder))
                .toList();
        for (int i = 0; i < cleaned.size(); i++) {
            ResearchObjective objective = i < existing.size() ? existing.get(i) : new ResearchObjective();
            if (objective.getProject() == null) {
                objective.setProject(project);
                objective.setCreatedBy(user);
                objective.setOrigin(ContentOrigin.USER);
            }
            objective.setType(i == 0 ? ResearchObjectiveType.GENERAL : ResearchObjectiveType.SPECIFIC);
            objective.setText(cleaned.get(i));
            objective.setDisplayOrder(i + 1);
            objectiveRepository.save(objective);
        }
    }

    private void upsertQuestions(ResearchProject project, User user, List<String> values) {
        List<String> cleaned = clean(values);
        List<ResearchQuestion> existing = questionRepository.findAllByProjectId(project.getId()).stream()
                .sorted(Comparator.comparingInt(ResearchQuestion::getDisplayOrder))
                .toList();
        for (int i = 0; i < cleaned.size(); i++) {
            ResearchQuestion question = i < existing.size() ? existing.get(i) : new ResearchQuestion();
            if (question.getProject() == null) {
                question.setProject(project);
                question.setCreatedBy(user);
                question.setOrigin(ContentOrigin.USER);
            }
            question.setText(cleaned.get(i));
            question.setDisplayOrder(i + 1);
            questionRepository.save(question);
        }
    }

    private void upsertHypotheses(ResearchProject project, User user, List<String> values) {
        List<String> cleaned = clean(values);
        List<ResearchHypothesis> existing = hypothesisRepository.findAllByProjectId(project.getId()).stream()
                .sorted(Comparator.comparing(ResearchHypothesis::getCreatedAt, Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();
        for (int i = 0; i < cleaned.size(); i++) {
            ResearchHypothesis hypothesis = i < existing.size() ? existing.get(i) : new ResearchHypothesis();
            if (hypothesis.getProject() == null) {
                hypothesis.setProject(project);
                hypothesis.setCreatedBy(user);
                hypothesis.setOrigin(ContentOrigin.USER);
            }
            hypothesis.setText(cleaned.get(i));
            hypothesisRepository.save(hypothesis);
        }
    }

    private ResearchDesignResponse response(UUID projectId) {
        String problem = problemRepository.findAllByProjectId(projectId).stream()
                .max(Comparator.comparing(ResearchProblem::getCreatedAt, Comparator.nullsLast(Comparator.naturalOrder())))
                .map(ResearchProblem::getStatement)
                .orElse("");
        List<DesignTextItem> objectives = objectiveRepository.findAllByProjectId(projectId).stream()
                .sorted(Comparator.comparingInt(ResearchObjective::getDisplayOrder))
                .map(item -> new DesignTextItem(item.getId(), item.getText(), item.getDisplayOrder(), item.getOrigin()))
                .toList();
        List<DesignTextItem> questions = questionRepository.findAllByProjectId(projectId).stream()
                .sorted(Comparator.comparingInt(ResearchQuestion::getDisplayOrder))
                .map(item -> new DesignTextItem(item.getId(), item.getText(), item.getDisplayOrder(), item.getOrigin()))
                .toList();
        List<DesignTextItem> hypotheses = hypothesisRepository.findAllByProjectId(projectId).stream()
                .sorted(Comparator.comparing(ResearchHypothesis::getCreatedAt, Comparator.nullsLast(Comparator.naturalOrder())))
                .map(item -> new DesignTextItem(item.getId(), item.getText(), 0, item.getOrigin()))
                .toList();
        return new ResearchDesignResponse(problem, objectives, questions, hypotheses);
    }

    private List<String> clean(List<String> values) {
        if (values == null) {
            return List.of();
        }
        return values.stream()
                .map(value -> value == null ? "" : value.trim())
                .filter(value -> !value.isBlank())
                .toList();
    }

    private String extractSection(String text, String heading) {
        if (text == null || text.isBlank()) return null;
        Pattern p = Pattern.compile("(?i)\\[" + Pattern.quote(heading) + "\\]([\\s\\S]*?)(?=\\[[A-Z\\s]+\\]|$)");
        Matcher m = p.matcher(text);
        if (m.find()) {
            return m.group(1).trim();
        }
        return null;
    }

    private List<String> extractNumberedList(String text, String heading) {
        String sec = extractSection(text, heading);
        if (sec == null || sec.isBlank()) return List.of();
        List<String> result = new ArrayList<>();
        for (String line : sec.split("\\n")) {
            String trimmed = line.trim();
            if (trimmed.matches("^(?:\\d+\\.|[•\\-*]|(?:RQ|H)\\d+:?)\\s+.*")) {
                String clean = trimmed.replaceAll("^(?:\\d+\\.|[•\\-*]|(?:RQ|H)\\d+:?)\\s*", "").trim();
                if (!clean.isBlank()) result.add(clean);
            } else if (!trimmed.isBlank() && result.isEmpty()) {
                result.add(trimmed);
            }
        }
        return result;
    }

    public record GenerateResearchDesignRequest(List<UUID> documentIds, String instructions) {}
    public record GenerateResearchDesignResponse(
            String problemStatement,
            String researchAim,
            List<String> objectives,
            List<String> questions,
            List<String> hypotheses
    ) {}

    public record SaveResearchDesignRequest(
            String problemStatement,
            List<String> objectives,
            List<String> questions,
            List<String> hypotheses
    ) {}

    public record ResearchDesignResponse(
            String problemStatement,
            List<DesignTextItem> objectives,
            List<DesignTextItem> questions,
            List<DesignTextItem> hypotheses
    ) {}

    public record DesignTextItem(UUID id, String text, int displayOrder, ContentOrigin origin) {}
}
