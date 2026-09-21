package com.researchassistant.dashboard.service;

import com.researchassistant.ai.usage.AiUsageReportingService;
import com.researchassistant.ai.usage.dto.AiUsageSummaryResponse;
import com.researchassistant.collaboration.entity.ProjectActivity;
import com.researchassistant.collaboration.entity.ProjectTask;
import com.researchassistant.collaboration.entity.ProjectTaskStatus;
import com.researchassistant.collaboration.repository.ProjectActivityRepository;
import com.researchassistant.project.repository.ProjectMembershipRepository;
import com.researchassistant.collaboration.repository.ProjectTaskRepository;
import com.researchassistant.dashboard.dto.DashboardDtos.*;
import com.researchassistant.dataset.repository.ResearchDatasetRepository;
import com.researchassistant.document.entity.DocumentStatus;
import com.researchassistant.document.repository.DocumentRepository;
import com.researchassistant.identity.entity.User;
import com.researchassistant.notification.NotificationRepository;
import com.researchassistant.project.dto.ResearchProjectResponse;
import com.researchassistant.project.entity.ProjectMembership;
import com.researchassistant.project.entity.ProjectMembershipStatus;
import com.researchassistant.project.entity.ProjectRole;
import com.researchassistant.project.entity.ResearchProject;
import com.researchassistant.project.entity.ResearchProjectStatus;
import com.researchassistant.project.repository.ResearchProjectRepository;
import com.researchassistant.project.service.ProjectAuthorizationContext;
import com.researchassistant.project.service.ProjectAuthorizationService;
import com.researchassistant.researchdesign.repository.ResearchHypothesisRepository;
import com.researchassistant.researchdesign.repository.ResearchObjectiveRepository;
import com.researchassistant.researchdesign.repository.ResearchProblemRepository;
import com.researchassistant.researchdesign.repository.ResearchQuestionRepository;
import com.researchassistant.subscription.WorkspaceSubscription;
import com.researchassistant.subscription.WorkspaceSubscriptionRepository;
import com.researchassistant.workspace.dto.WorkspaceResponse;
import com.researchassistant.workspace.entity.Workspace;
import com.researchassistant.workspace.entity.WorkspaceMembership;
import com.researchassistant.workspace.entity.WorkspaceMembershipStatus;
import com.researchassistant.workspace.repository.WorkspaceMembershipRepository;
import com.researchassistant.workspace.service.PersonalWorkspaceService;
import com.researchassistant.workspace.service.WorkspaceAuthorizationService;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class DashboardService {

    private final ResearchProjectRepository projectRepository;
    private final ProjectMembershipRepository projectMembershipRepository;
    private final WorkspaceMembershipRepository workspaceMembershipRepository;
    private final PersonalWorkspaceService personalWorkspaceService;
    private final ProjectTaskRepository taskRepository;
    private final DocumentRepository documentRepository;
    private final NotificationRepository notificationRepository;
    private final AiUsageReportingService aiUsageReportingService;
    private final ProjectActivityRepository activityRepository;
    private final WorkspaceAuthorizationService workspaceAuthorizationService;
    private final ProjectAuthorizationService projectAuthorizationService;
    private final WorkspaceSubscriptionRepository subscriptionRepository;

    // Research entities for progress resolution
    private final ResearchProblemRepository problemRepository;
    private final ResearchObjectiveRepository objectiveRepository;
    private final ResearchQuestionRepository questionRepository;
    private final ResearchHypothesisRepository hypothesisRepository;
    private final ResearchDatasetRepository datasetRepository;

    public DashboardService(
            ResearchProjectRepository projectRepository,
            ProjectMembershipRepository projectMembershipRepository,
            WorkspaceMembershipRepository workspaceMembershipRepository,
            PersonalWorkspaceService personalWorkspaceService,
            ProjectTaskRepository taskRepository,
            DocumentRepository documentRepository,
            NotificationRepository notificationRepository,
            AiUsageReportingService aiUsageReportingService,
            ProjectActivityRepository activityRepository,
            WorkspaceAuthorizationService workspaceAuthorizationService,
            ProjectAuthorizationService projectAuthorizationService,
            WorkspaceSubscriptionRepository subscriptionRepository,
            ResearchProblemRepository problemRepository,
            ResearchObjectiveRepository objectiveRepository,
            ResearchQuestionRepository questionRepository,
            ResearchHypothesisRepository hypothesisRepository,
            ResearchDatasetRepository datasetRepository
    ) {
        this.projectRepository = projectRepository;
        this.projectMembershipRepository = projectMembershipRepository;
        this.workspaceMembershipRepository = workspaceMembershipRepository;
        this.personalWorkspaceService = personalWorkspaceService;
        this.taskRepository = taskRepository;
        this.documentRepository = documentRepository;
        this.notificationRepository = notificationRepository;
        this.aiUsageReportingService = aiUsageReportingService;
        this.activityRepository = activityRepository;
        this.workspaceAuthorizationService = workspaceAuthorizationService;
        this.projectAuthorizationService = projectAuthorizationService;
        this.subscriptionRepository = subscriptionRepository;
        this.problemRepository = problemRepository;
        this.objectiveRepository = objectiveRepository;
        this.questionRepository = questionRepository;
        this.hypothesisRepository = hypothesisRepository;
        this.datasetRepository = datasetRepository;
    }

    public UserDashboardResponse getUserDashboard(User user) {
        String greeting = buildGreeting(user);

        // Ensure personal workspace or pick current
        WorkspaceResponse workspace = personalWorkspaceService.ensurePersonalWorkspaceResponse(user);

        long activeProjectCount = projectRepository.countActiveProjectsForUser(user.getId());
        long openTaskCount = taskRepository.countOpenTasksForUser(user.getId());
        long documentCount = documentRepository.countActiveDocumentsForUser(user.getId());
        long unreadNotifications = notificationRepository.countByRecipientIdAndReadAtIsNull(user.getId());

        AiUsageSummaryResponse aiSummary = aiUsageReportingService.getUserUsageSummary(user.getId());
        OffsetDateTime resetDate = OffsetDateTime.now(ZoneOffset.UTC).plusMonths(1).withDayOfMonth(1).toLocalDate().atStartOfDay().atOffset(ZoneOffset.UTC);
        AiUsageSummary aiUsage = new AiUsageSummary(
                aiSummary.requestsToday(),
                aiSummary.requestsThisMonth(),
                aiSummary.tokensThisMonth(),
                1000L,
                1_000_000L,
                false,
                resetDate
        );

        List<ResearchProjectResponse> recentProjects = projectRepository
                .findRecentAuthorizedProjectsForUser(user.getId(), PageRequest.of(0, 5))
                .stream()
                .map(p -> toProjectResponse(p, user.getId()))
                .toList();

        List<UserTaskSummary> recentTasks = taskRepository
                .findAllMyTasks(user.getId(), null, null, null, PageRequest.of(0, 5))
                .stream()
                .map(this::toUserTaskSummary)
                .toList();

        List<UserActivitySummary> recentActivities = new ArrayList<>();
        if (!recentProjects.isEmpty()) {
            recentActivities = activityRepository
                    .findAllByProjectIdOrderByOccurredAtDesc(recentProjects.get(0).id(), PageRequest.of(0, 5))
                    .stream()
                    .map(this::toUserActivitySummary)
                    .toList();
        }

        return new UserDashboardResponse(
                greeting,
                workspace,
                activeProjectCount,
                openTaskCount,
                documentCount,
                unreadNotifications,
                aiUsage,
                recentProjects,
                recentTasks,
                recentActivities
        );
    }

    public WorkspaceDashboardResponse getWorkspaceDashboard(UUID workspaceId, User user) {
        WorkspaceMembership membership = workspaceAuthorizationService.requireActiveMembership(workspaceId, user);
        Workspace workspace = membership.getWorkspace();

        long memberCount = workspaceMembershipRepository.findAllByWorkspaceIdAndStatus(
                workspaceId,
                WorkspaceMembershipStatus.ACTIVE,
                PageRequest.of(0, 100)
        ).size();

        long projectCount = projectRepository.countByWorkspaceId(workspaceId);
        long activeProjectCount = projectRepository.countByWorkspaceIdAndStatus(workspaceId, ResearchProjectStatus.ACTIVE);
        long documentCount = 0L; // aggregated across workspace projects

        AiUsageSummaryResponse aiSummary = aiUsageReportingService.getWorkspaceUsageSummary(workspaceId);

        Optional<WorkspaceSubscription> subOpt = subscriptionRepository.findCurrentEffective(workspaceId, OffsetDateTime.now());
        String planCode = subOpt.map(s -> s.getPlan().getCode()).orElse("FREE");

        List<ResearchProjectResponse> recentProjects = projectRepository
                .findAllByWorkspaceId(workspaceId, PageRequest.of(0, 5))
                .stream()
                .map(p -> toProjectResponse(p, user.getId()))
                .toList();

        WorkspaceResponse workspaceDto = new WorkspaceResponse(
                workspace.getId(),
                workspace.getName(),
                workspace.getType(),
                workspace.getStatus(),
                workspace.getOwner().getId(),
                membership.getRole(),
                workspace.getCreatedAt(),
                workspace.getUpdatedAt()
        );

        return new WorkspaceDashboardResponse(
                workspaceDto,
                membership.getRole(),
                memberCount,
                projectCount,
                activeProjectCount,
                documentCount,
                0L,
                aiSummary.requestsThisMonth(),
                planCode,
                recentProjects
        );
    }

    public ProjectDashboardResponse getProjectDashboard(UUID projectId, User user) {
        ProjectAuthorizationContext context = projectAuthorizationService.requireProjectViewer(projectId, user);
        ResearchProject project = context.project();
        ProjectRole role = context.projectMembership()
                .map(ProjectMembership::getRole)
                .orElse(ProjectRole.VIEWER);

        long memberCount = projectMembershipRepository.findAllByProjectIdAndStatus(
                projectId,
                ProjectMembershipStatus.ACTIVE,
                PageRequest.of(0, 100)
        ).size();

        long docTotal = documentRepository.countByProjectId(projectId);
        long docReady = documentRepository.countByProjectIdAndStatus(projectId, DocumentStatus.READY);
        long docProcessing = documentRepository.countByProjectIdAndStatus(projectId, DocumentStatus.PROCESSING);
        long docFailed = documentRepository.countByProjectIdAndStatus(projectId, DocumentStatus.FAILED);
        DocumentMetricsSummary documents = new DocumentMetricsSummary(docTotal, docReady, docProcessing, docFailed);

        long taskTotal = taskRepository.countByProjectId(projectId);
        long taskTodo = taskRepository.countByProjectIdAndStatus(projectId, ProjectTaskStatus.TODO);
        long taskInProgress = taskRepository.countByProjectIdAndStatus(projectId, ProjectTaskStatus.IN_PROGRESS);
        long taskInReview = taskRepository.countByProjectIdAndStatus(projectId, ProjectTaskStatus.IN_REVIEW);
        long taskCompleted = taskRepository.countByProjectIdAndStatus(projectId, ProjectTaskStatus.COMPLETED);
        TaskMetricsSummary tasks = new TaskMetricsSummary(taskTotal, taskTodo, taskInProgress, taskInReview, taskCompleted, 0L);

        ResearchProgressResponse progress = getResearchProgress(projectId, user);
        ResearchProgressSummary progressSummary = new ResearchProgressSummary(
                progress.completedStages(),
                progress.totalStages(),
                progress.percentComplete(),
                progress.nextIncompleteStage(),
                progress.nextStageUrl()
        );

        List<UserActivitySummary> recentActivities = activityRepository
                .findAllByProjectIdOrderByOccurredAtDesc(projectId, PageRequest.of(0, 5))
                .stream()
                .map(this::toUserActivitySummary)
                .toList();

        return new ProjectDashboardResponse(
                toProjectResponse(project, user.getId()),
                role,
                memberCount,
                documents,
                tasks,
                progressSummary,
                recentActivities
        );
    }

    public ResearchProgressResponse getResearchProgress(UUID projectId, User user) {
        projectAuthorizationService.requireProjectViewer(projectId, user);
        ResearchProject project = projectRepository.findById(projectId).orElseThrow();

        List<ResearchStageDto> stages = new ArrayList<>();
        OffsetDateTime now = OffsetDateTime.now();

        // 1. Research Problem
        boolean hasProblem = !problemRepository.findAllByProjectId(projectId).isEmpty();
        stages.add(new ResearchStageDto(
                1, "problem", "Research Problem",
                hasProblem ? "COMPLETE" : "NOT_STARTED",
                "Define the problem statement, context, and motivation.",
                hasProblem ? 1 : 0,
                "/app/projects/" + projectId + "/research#Research%20Problem",
                now
        ));

        // 2. Objectives
        int objectivesCount = objectiveRepository.findAllByProjectId(projectId).size();
        stages.add(new ResearchStageDto(
                2, "objectives", "Research Objectives",
                objectivesCount > 0 ? "COMPLETE" : "NOT_STARTED",
                "Primary and specific objectives guiding the inquiry.",
                objectivesCount,
                "/app/projects/" + projectId + "/research#Objectives",
                now
        ));

        // 3. Questions & Hypotheses
        int questionsCount = questionRepository.findAllByProjectId(projectId).size()
                + hypothesisRepository.findAllByProjectId(projectId).size();
        stages.add(new ResearchStageDto(
                3, "questions", "Questions & Hypotheses",
                questionsCount > 0 ? "COMPLETE" : "NOT_STARTED",
                "Core research questions and testable hypotheses.",
                questionsCount,
                "/app/projects/" + projectId + "/research#Objectives",
                now
        ));

        // 4. Literature Review
        int docCount = (int) documentRepository.countByProjectId(projectId);
        stages.add(new ResearchStageDto(
                4, "literature", "Literature Review",
                docCount >= 3 ? "COMPLETE" : docCount > 0 ? "IN_PROGRESS" : "NOT_STARTED",
                "Synthesize existing academic scholarship and identify gaps.",
                docCount,
                "/app/projects/" + projectId + "/documents",
                now
        ));

        // 5. Frameworks
        stages.add(new ResearchStageDto(
                5, "frameworks", "Theoretical & Conceptual Frameworks",
                "NOT_STARTED",
                "Underlying theoretical models and operational definitions.",
                0,
                "/app/projects/" + projectId + "/research#Conceptual%20Framework",
                now
        ));

        // 6. Methodology
        stages.add(new ResearchStageDto(
                6, "methodology", "Research Methodology",
                "NOT_STARTED",
                "Qualitative, quantitative, or mixed methodology approach.",
                0,
                "/app/projects/" + projectId + "/research#Methodology",
                now
        ));

        // 7. Population & Sampling
        stages.add(new ResearchStageDto(
                7, "sampling", "Population & Sampling",
                "NOT_STARTED",
                "Target population, sampling technique, and sample size calculations.",
                0,
                "/app/projects/" + projectId + "/research#Methodology",
                now
        ));

        // 8. Data Collection Methods
        stages.add(new ResearchStageDto(
                8, "collection", "Data Collection Methods",
                "NOT_STARTED",
                "Protocols for primary and secondary data acquisition.",
                0,
                "/app/projects/" + projectId + "/research#Instruments",
                now
        ));

        // 9. Instruments
        stages.add(new ResearchStageDto(
                9, "instruments", "Research Instruments",
                "NOT_STARTED",
                "Surveys, interview schedules, and observation guides.",
                0,
                "/app/projects/" + projectId + "/research#Instruments",
                now
        ));

        // 10. Ethics
        stages.add(new ResearchStageDto(
                10, "ethics", "Ethics & Compliance",
                "NOT_STARTED",
                "IRB consent, participant anonymity, and data governance.",
                0,
                "/app/projects/" + projectId + "/research#Ethics",
                now
        ));

        // 11. Fieldwork
        stages.add(new ResearchStageDto(
                11, "fieldwork", "Fieldwork Execution",
                "NOT_STARTED",
                "Participant recruitment and data gathering tracking.",
                0,
                "/app/projects/" + projectId + "/research#Ethics",
                now
        ));

        // 12. Dataset
        int datasetCount = (int) datasetRepository.findAllByProjectId(projectId, PageRequest.of(0, 1)).getTotalElements();
        stages.add(new ResearchStageDto(
                12, "dataset", "Datasets & Variables",
                datasetCount > 0 ? "COMPLETE" : "NOT_STARTED",
                "Raw empirical datasets, codebooks, and variable distributions.",
                datasetCount,
                "/app/projects/" + projectId + "/data",
                now
        ));

        // 13. Analysis
        stages.add(new ResearchStageDto(
                13, "analysis", "Data Analysis",
                "NOT_STARTED",
                "Descriptive statistics, hypothesis tests, and thematic coding.",
                0,
                "/app/projects/" + projectId + "/analysis",
                now
        ));

        // 14. Findings
        stages.add(new ResearchStageDto(
                14, "findings", "Findings & Results",
                "NOT_STARTED",
                "Empirical outcomes tied to research questions.",
                0,
                "/app/projects/" + projectId + "/findings",
                now
        ));

        // 15. Discussion
        stages.add(new ResearchStageDto(
                15, "discussion", "Discussion",
                "NOT_STARTED",
                "Interpretation of findings in light of existing literature.",
                0,
                "/app/projects/" + projectId + "/report",
                now
        ));

        // 16. Conclusions
        stages.add(new ResearchStageDto(
                16, "conclusions", "Conclusions",
                "NOT_STARTED",
                "Definitive synthesis of research outcomes.",
                0,
                "/app/projects/" + projectId + "/report",
                now
        ));

        // 17. Recommendations
        stages.add(new ResearchStageDto(
                17, "recommendations", "Recommendations",
                "NOT_STARTED",
                "Actionable practical and academic recommendations.",
                0,
                "/app/projects/" + projectId + "/report",
                now
        ));

        // 18. Report
        stages.add(new ResearchStageDto(
                18, "report", "Academic Report",
                "NOT_STARTED",
                "Complete monograph, thesis, or manuscript assembly.",
                0,
                "/app/projects/" + projectId + "/report",
                now
        ));

        int completedStages = (int) stages.stream().filter(s -> "COMPLETE".equals(s.status())).count();
        int totalStages = stages.size();
        int percentComplete = (completedStages * 100) / totalStages;

        ResearchStageDto nextIncomplete = stages.stream()
                .filter(s -> !"COMPLETE".equals(s.status()))
                .findFirst()
                .orElse(stages.get(0));

        return new ResearchProgressResponse(
                projectId,
                project.getTitle(),
                completedStages,
                totalStages,
                percentComplete,
                nextIncomplete.name(),
                nextIncomplete.actionUrl(),
                stages
        );
    }

    private String buildGreeting(User user) {
        int hour = OffsetDateTime.now().getHour();
        String timeOfDay = hour < 12 ? "Good morning" : hour < 17 ? "Good afternoon" : "Good evening";
        String name = user.getFirstName();
        if (name == null || name.isBlank()) {
            name = user.getEmail() != null && user.getEmail().contains("@")
                    ? user.getEmail().substring(0, user.getEmail().indexOf('@'))
                    : "Researcher";
        }
        return timeOfDay + ", " + name.trim();
    }

    private ResearchProjectResponse toProjectResponse(ResearchProject project, UUID userId) {
        ProjectRole role = projectMembershipRepository
                .findByProjectIdAndUserIdAndStatus(project.getId(), userId, ProjectMembershipStatus.ACTIVE)
                .map(ProjectMembership::getRole)
                .orElse(ProjectRole.VIEWER);

        return new ResearchProjectResponse(
                project.getId(),
                project.getWorkspace().getId(),
                project.getTitle(),
                project.getDescription(),
                project.getResearchAim(),
                project.getStudyArea(),
                project.getResearchType(),
                project.getKeywords(),
                project.getStatus(),
                project.getCreatedBy().getId(),
                role,
                project.getCreatedAt(),
                project.getUpdatedAt()
        );
    }

    private UserTaskSummary toUserTaskSummary(ProjectTask task) {
        boolean overdue = task.getDueDate() != null && task.getDueDate().isBefore(LocalDate.now());
        return new UserTaskSummary(
                task.getId(),
                task.getProject().getId(),
                task.getProject().getTitle(),
                task.getTitle(),
                task.getStatus(),
                task.getPriority(),
                task.getDueDate(),
                overdue,
                task.getUpdatedAt()
        );
    }

    private UserActivitySummary toUserActivitySummary(ProjectActivity activity) {
        String actorName = activity.getActor() != null ? activity.getActor().getEmail() : "System";
        return new UserActivitySummary(
                activity.getId(),
                activity.getProject().getId(),
                activity.getProject().getTitle(),
                activity.getType().name(),
                activity.getSafeSummary(),
                actorName,
                activity.getOccurredAt()
        );
    }
}
