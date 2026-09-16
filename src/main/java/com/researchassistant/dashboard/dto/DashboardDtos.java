package com.researchassistant.dashboard.dto;

import com.researchassistant.collaboration.entity.ProjectTaskPriority;
import com.researchassistant.collaboration.entity.ProjectTaskStatus;
import com.researchassistant.project.dto.ResearchProjectResponse;
import com.researchassistant.project.entity.ProjectRole;
import com.researchassistant.workspace.dto.WorkspaceResponse;
import com.researchassistant.workspace.entity.WorkspaceRole;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public class DashboardDtos {

    public record UserDashboardResponse(
            String greeting,
            WorkspaceResponse currentWorkspace,
            long activeProjectCount,
            long openTaskCount,
            long documentCount,
            long unreadNotifications,
            AiUsageSummary aiUsage,
            List<ResearchProjectResponse> recentProjects,
            List<UserTaskSummary> recentTasks,
            List<UserActivitySummary> recentActivities
    ) {}

    public record AiUsageSummary(
            long requestsToday,
            long requestsThisMonth,
            long tokensThisMonth,
            Long requestLimit,
            Long tokenLimit,
            boolean isUnlimited,
            OffsetDateTime resetDate
    ) {}

    public record UserTaskSummary(
            UUID id,
            UUID projectId,
            String projectTitle,
            String title,
            ProjectTaskStatus status,
            ProjectTaskPriority priority,
            LocalDate dueDate,
            boolean overdue,
            OffsetDateTime updatedAt
    ) {}

    public record UserActivitySummary(
            UUID id,
            UUID projectId,
            String projectTitle,
            String type,
            String summary,
            String actorName,
            OffsetDateTime occurredAt
    ) {}

    public record WorkspaceDashboardResponse(
            WorkspaceResponse workspace,
            WorkspaceRole currentUserRole,
            long memberCount,
            long projectCount,
            long activeProjectCount,
            long documentCount,
            long storageBytes,
            long aiRequestsThisMonth,
            String planCode,
            List<ResearchProjectResponse> recentProjects
    ) {}

    public record ProjectDashboardResponse(
            ResearchProjectResponse project,
            ProjectRole currentUserRole,
            long memberCount,
            DocumentMetricsSummary documents,
            TaskMetricsSummary tasks,
            ResearchProgressSummary researchProgress,
            List<UserActivitySummary> recentActivities
    ) {}

    public record DocumentMetricsSummary(
            long total,
            long ready,
            long processing,
            long failed
    ) {}

    public record TaskMetricsSummary(
            long total,
            long todo,
            long inProgress,
            long inReview,
            long completed,
            long overdue
    ) {}

    public record ResearchProgressSummary(
            int completedStages,
            int totalStages,
            int percentComplete,
            String nextIncompleteStage,
            String nextStageUrl
    ) {}

    public record ResearchProgressResponse(
            UUID projectId,
            String projectTitle,
            int completedStages,
            int totalStages,
            int percentComplete,
            String nextIncompleteStage,
            String nextStageUrl,
            List<ResearchStageDto> stages
    ) {}

    public record ResearchStageDto(
            int number,
            String key,
            String name,
            String status, // NOT_STARTED, DRAFT, ACTIVE, IN_PROGRESS, COMPLETE
            String description,
            int itemCount,
            String actionUrl,
            OffsetDateTime lastUpdated
    ) {}

    public record MyTasksResponse(
            long openCount,
            long inProgressCount,
            long inReviewCount,
            long overdueCount
    ) {}

    public record MyDocumentsResponse(
            long totalCount,
            long readyCount,
            long processingCount,
            long failedCount
    ) {}
}
