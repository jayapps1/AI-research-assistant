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
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/projects/{projectId}/research-design")
public class ResearchDesignController {
    private final AuthenticatedUserResolver userResolver;
    private final ProjectAuthorizationService authorizationService;
    private final ResearchProblemRepository problemRepository;
    private final ResearchObjectiveRepository objectiveRepository;
    private final ResearchQuestionRepository questionRepository;
    private final ResearchHypothesisRepository hypothesisRepository;

    public ResearchDesignController(
            AuthenticatedUserResolver userResolver,
            ProjectAuthorizationService authorizationService,
            ResearchProblemRepository problemRepository,
            ResearchObjectiveRepository objectiveRepository,
            ResearchQuestionRepository questionRepository,
            ResearchHypothesisRepository hypothesisRepository
    ) {
        this.userResolver = userResolver;
        this.authorizationService = authorizationService;
        this.problemRepository = problemRepository;
        this.objectiveRepository = objectiveRepository;
        this.questionRepository = questionRepository;
        this.hypothesisRepository = hypothesisRepository;
    }

    @GetMapping
    @Transactional(readOnly = true)
    public ResearchDesignResponse get(Authentication authentication, @PathVariable UUID projectId) {
        User user = userResolver.requireActiveUser(authentication);
        authorizationService.requireProjectViewer(projectId, user);
        return response(projectId);
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
