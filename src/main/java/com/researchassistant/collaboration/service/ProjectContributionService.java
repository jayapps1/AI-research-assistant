package com.researchassistant.collaboration.service;

import com.researchassistant.ai.orchestration.AiTaskType;
import com.researchassistant.ai.usage.AiRequestRepository;
import com.researchassistant.collaboration.dto.CollaborationDtos.ContributionMemberResponse;
import com.researchassistant.collaboration.dto.CollaborationDtos.ContributionSummaryResponse;
import com.researchassistant.collaboration.entity.ProjectActivityType;
import com.researchassistant.collaboration.repository.ProjectActivityRepository;
import com.researchassistant.project.entity.ProjectMembership;
import com.researchassistant.project.entity.ProjectMembershipStatus;
import com.researchassistant.project.repository.ProjectMembershipRepository;
import com.researchassistant.project.service.ProjectAuthorizationService;
import com.researchassistant.identity.entity.User;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class ProjectContributionService {
    private final ProjectMembershipRepository membershipRepository;
    private final ProjectActivityRepository activityRepository;
    private final AiRequestRepository aiRequestRepository;
    private final ProjectAuthorizationService authorizationService;

    public ProjectContributionService(ProjectMembershipRepository membershipRepository, ProjectActivityRepository activityRepository, AiRequestRepository aiRequestRepository, ProjectAuthorizationService authorizationService) {
        this.membershipRepository = membershipRepository;
        this.activityRepository = activityRepository;
        this.aiRequestRepository = aiRequestRepository;
        this.authorizationService = authorizationService;
    }

    public ContributionSummaryResponse summary(UUID projectId, User actor) {
        authorizationService.requireProjectViewer(projectId, actor);
        List<ContributionMemberResponse> members = membershipRepository.findAllByProjectIdAndStatus(projectId, ProjectMembershipStatus.ACTIVE).stream()
                .map(member -> memberSummary(projectId, member))
                .toList();
        return new ContributionSummaryResponse(projectId, members);
    }

    private ContributionMemberResponse memberSummary(UUID projectId, ProjectMembership member) {
        UUID userId = member.getUser().getId();
        return new ContributionMemberResponse(
                userId,
                member.getUser().getEmail(),
                activityRepository.countByProjectIdAndActorIdAndType(projectId, userId, ProjectActivityType.TASK_COMPLETED),
                activityRepository.countByProjectIdAndActorIdAndType(projectId, userId, ProjectActivityType.COMMENT_CREATED),
                activityRepository.countByProjectIdAndActorIdAndType(projectId, userId, ProjectActivityType.REVIEW_APPROVED)
                        + activityRepository.countByProjectIdAndActorIdAndType(projectId, userId, ProjectActivityType.REVIEW_CHANGES_REQUESTED),
                activityRepository.countByProjectIdAndActorIdAndType(projectId, userId, ProjectActivityType.DOCUMENT_UPLOADED),
                aiRequestRepository.findAllByProjectIdAndCreatedAtAfter(projectId, OffsetDateTime.now().minusYears(100)).stream()
                        .filter(request -> request.getUser().getId().equals(userId))
                        .filter(request -> request.getTaskType() != AiTaskType.GROUNDED_QA)
                        .count(),
                activityRepository.countByProjectIdAndActorIdAndType(projectId, userId, ProjectActivityType.AI_DRAFT_ACCEPTED)
                        + activityRepository.countByProjectIdAndActorIdAndType(projectId, userId, ProjectActivityType.AI_ARTIFACT_PROPOSAL_ACCEPTED)
        );
    }
}
