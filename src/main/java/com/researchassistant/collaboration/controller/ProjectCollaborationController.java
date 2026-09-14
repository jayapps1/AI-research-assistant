package com.researchassistant.collaboration.controller;

import com.researchassistant.collaboration.dto.CollaborationDtos.*;
import com.researchassistant.collaboration.entity.*;
import com.researchassistant.collaboration.service.*;
import com.researchassistant.identity.entity.User;
import com.researchassistant.identity.service.AuthenticatedUserResolver;
import com.researchassistant.project.dto.PageResponse;
import com.researchassistant.project.dto.ProjectMemberResponse;

import jakarta.validation.Valid;

import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
public class ProjectCollaborationController {
    private static final int MAX_PAGE_SIZE = 100;
    private final AuthenticatedUserResolver users;
    private final ProjectInvitationService invitations;
    private final ProjectTaskService tasks;
    private final ArtifactCollaborationService artifacts;
    private final ProjectActivityService activities;
    private final ProjectContributionService contributions;
    private final ResearchReportAuthorService authors;

    public ProjectCollaborationController(AuthenticatedUserResolver users, ProjectInvitationService invitations, ProjectTaskService tasks, ArtifactCollaborationService artifacts, ProjectActivityService activities, ProjectContributionService contributions, ResearchReportAuthorService authors) {
        this.users = users;
        this.invitations = invitations;
        this.tasks = tasks;
        this.artifacts = artifacts;
        this.activities = activities;
        this.contributions = contributions;
        this.authors = authors;
    }

    @PostMapping("/projects/{projectId}/invitations")
    @ResponseStatus(HttpStatus.CREATED)
    public ProjectInvitationResponse invite(Authentication authentication, @PathVariable UUID projectId, @Valid @RequestBody CreateProjectInvitationRequest request) {
        return invitations.invite(projectId, user(authentication), request);
    }

    @GetMapping("/projects/{projectId}/invitations")
    public List<ProjectInvitationResponse> invitations(Authentication authentication, @PathVariable UUID projectId) {
        return invitations.list(projectId, user(authentication));
    }

    @PostMapping("/project-invitations/{invitationId}/accept")
    public ProjectInvitationResponse acceptInvitation(Authentication authentication, @PathVariable UUID invitationId) {
        return invitations.accept(invitationId, user(authentication));
    }

    @PostMapping("/project-invitations/{invitationId}/decline")
    public ProjectInvitationResponse declineInvitation(Authentication authentication, @PathVariable UUID invitationId) {
        return invitations.decline(invitationId, user(authentication));
    }

    @PostMapping("/project-invitations/{invitationId}/revoke")
    public ProjectInvitationResponse revokeInvitation(Authentication authentication, @PathVariable UUID invitationId) {
        return invitations.revoke(invitationId, user(authentication));
    }

    @PostMapping("/project-invitations/{invitationId}/resend")
    public ProjectInvitationResponse resendInvitation(Authentication authentication, @PathVariable UUID invitationId) {
        return invitations.resend(invitationId, user(authentication));
    }

    @PatchMapping("/projects/{projectId}/memberships/{memberId}/role")
    public ProjectMemberResponse changeRole(Authentication authentication, @PathVariable UUID projectId, @PathVariable UUID memberId, @Valid @RequestBody ChangeMemberRoleRequest request) {
        return invitations.changeRole(projectId, memberId, user(authentication), request);
    }

    @PostMapping("/projects/{projectId}/memberships/{memberId}/suspend")
    public ProjectMemberResponse suspend(Authentication authentication, @PathVariable UUID projectId, @PathVariable UUID memberId) {
        return invitations.suspend(projectId, memberId, user(authentication));
    }

    @PostMapping("/projects/{projectId}/memberships/{memberId}/reactivate")
    public ProjectMemberResponse reactivate(Authentication authentication, @PathVariable UUID projectId, @PathVariable UUID memberId) {
        return invitations.reactivate(projectId, memberId, user(authentication));
    }

    @PostMapping("/projects/{projectId}/memberships/{memberId}/transfer-lead")
    public ProjectMemberResponse transferLead(Authentication authentication, @PathVariable UUID projectId, @PathVariable UUID memberId, @RequestBody(required = false) TransferLeadRequest request) {
        return invitations.transferLead(projectId, memberId, user(authentication), request == null ? new TransferLeadRequest(false) : request);
    }

    @DeleteMapping("/projects/{projectId}/memberships/{memberId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeMember(Authentication authentication, @PathVariable UUID projectId, @PathVariable UUID memberId) {
        invitations.remove(projectId, memberId, user(authentication));
    }

    @PostMapping("/reports/{reportId}/authors")
    @ResponseStatus(HttpStatus.CREATED)
    public ReportAuthorResponse addAuthor(Authentication authentication, @PathVariable UUID reportId, @Valid @RequestBody CreateReportAuthorRequest request) {
        return authors.add(reportId, user(authentication), request);
    }

    @GetMapping("/reports/{reportId}/authors")
    public List<ReportAuthorResponse> listAuthors(Authentication authentication, @PathVariable UUID reportId) {
        return authors.list(reportId, user(authentication));
    }

    @PostMapping("/projects/{projectId}/tasks")
    @ResponseStatus(HttpStatus.CREATED)
    public ProjectTaskResponse createTask(Authentication authentication, @PathVariable UUID projectId, @Valid @RequestBody CreateProjectTaskRequest request) {
        return tasks.create(projectId, user(authentication), request);
    }

    @GetMapping("/projects/{projectId}/tasks")
    public PageResponse<ProjectTaskResponse> listTasks(Authentication authentication, @PathVariable UUID projectId, @RequestParam(required = false) ProjectTaskStatus status, @RequestParam(required = false) ProjectTaskPriority priority, @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        return PageResponse.from(tasks.list(projectId, user(authentication), status, priority, PageRequest.of(Math.max(0, page), Math.min(Math.max(1, size), MAX_PAGE_SIZE))));
    }

    @GetMapping("/projects/{projectId}/tasks/mine")
    public List<ProjectTaskResponse> myTasks(Authentication authentication, @PathVariable UUID projectId) {
        return tasks.mine(projectId, user(authentication));
    }

    @GetMapping("/project-tasks/{taskId}")
    public ProjectTaskResponse getTask(Authentication authentication, @PathVariable UUID taskId) {
        return tasks.get(taskId, user(authentication));
    }

    @PatchMapping("/project-tasks/{taskId}")
    public ProjectTaskResponse updateTask(Authentication authentication, @PathVariable UUID taskId, @Valid @RequestBody UpdateProjectTaskRequest request) {
        return tasks.update(taskId, user(authentication), request);
    }

    @PostMapping("/project-tasks/{taskId}/assignees")
    public ProjectTaskResponse addAssignee(Authentication authentication, @PathVariable UUID taskId, @Valid @RequestBody AddTaskAssigneeRequest request) {
        return tasks.addAssignee(taskId, user(authentication), request);
    }

    @DeleteMapping("/project-tasks/{taskId}/assignees/{memberId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeAssignee(Authentication authentication, @PathVariable UUID taskId, @PathVariable UUID memberId) {
        tasks.removeAssignee(taskId, memberId, user(authentication));
    }

    @PostMapping("/project-tasks/{taskId}/start")
    public ProjectTaskResponse startTask(Authentication authentication, @PathVariable UUID taskId) { return tasks.transition(taskId, user(authentication), ProjectTaskStatus.IN_PROGRESS); }
    @PostMapping("/project-tasks/{taskId}/submit-for-review")
    public ProjectTaskResponse submitTask(Authentication authentication, @PathVariable UUID taskId) { return tasks.transition(taskId, user(authentication), ProjectTaskStatus.IN_REVIEW); }
    @PostMapping("/project-tasks/{taskId}/complete")
    public ProjectTaskResponse completeTask(Authentication authentication, @PathVariable UUID taskId) { return tasks.transition(taskId, user(authentication), ProjectTaskStatus.COMPLETED); }
    @PostMapping("/project-tasks/{taskId}/cancel")
    public ProjectTaskResponse cancelTask(Authentication authentication, @PathVariable UUID taskId) { return tasks.transition(taskId, user(authentication), ProjectTaskStatus.CANCELLED); }

    @PostMapping("/projects/{projectId}/comments")
    @ResponseStatus(HttpStatus.CREATED)
    public ArtifactCommentResponse createComment(Authentication authentication, @PathVariable UUID projectId, @Valid @RequestBody CreateCommentRequest request) { return artifacts.createComment(projectId, user(authentication), request); }
    @GetMapping("/projects/{projectId}/comments")
    public PageResponse<ArtifactCommentResponse> listComments(Authentication authentication, @PathVariable UUID projectId, @RequestParam(required = false) CollaborationArtifactType artifactType, @RequestParam(required = false) UUID artifactId, @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) { return PageResponse.from(artifacts.listComments(projectId, user(authentication), artifactType, artifactId, PageRequest.of(Math.max(0, page), Math.min(Math.max(1, size), MAX_PAGE_SIZE)))); }
    @PostMapping("/comments/{commentId}/reply")
    public ArtifactCommentResponse reply(Authentication authentication, @PathVariable UUID commentId, @Valid @RequestBody ReplyCommentRequest request) { return artifacts.reply(commentId, user(authentication), request); }
    @PostMapping("/comments/{commentId}/resolve")
    public ArtifactCommentResponse resolve(Authentication authentication, @PathVariable UUID commentId) { return artifacts.resolve(commentId, user(authentication), false); }
    @PostMapping("/comments/{commentId}/reopen")
    public ArtifactCommentResponse reopen(Authentication authentication, @PathVariable UUID commentId) { return artifacts.resolve(commentId, user(authentication), true); }

    @PostMapping("/projects/{projectId}/reviews")
    @ResponseStatus(HttpStatus.CREATED)
    public ArtifactReviewResponse requestReview(Authentication authentication, @PathVariable UUID projectId, @Valid @RequestBody CreateReviewRequest request) { return artifacts.requestReview(projectId, user(authentication), request); }
    @GetMapping("/projects/{projectId}/reviews")
    public PageResponse<ArtifactReviewResponse> listReviews(Authentication authentication, @PathVariable UUID projectId, @RequestParam(required = false) ArtifactReviewStatus status, @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) { return PageResponse.from(artifacts.listReviews(projectId, user(authentication), status, PageRequest.of(Math.max(0, page), Math.min(Math.max(1, size), MAX_PAGE_SIZE)))); }
    @GetMapping("/reviews/{reviewId}")
    public ArtifactReviewResponse getReview(Authentication authentication, @PathVariable UUID reviewId) { return artifacts.getReview(reviewId, user(authentication)); }
    @PostMapping("/reviews/{reviewId}/start")
    public ArtifactReviewResponse startReview(Authentication authentication, @PathVariable UUID reviewId) { return artifacts.startReview(reviewId, user(authentication)); }
    @PostMapping("/reviews/{reviewId}/approve")
    public ArtifactReviewResponse approveReview(Authentication authentication, @PathVariable UUID reviewId, @RequestBody(required = false) ReviewDecisionRequest request) { return artifacts.approveReview(reviewId, user(authentication), request == null ? new ReviewDecisionRequest(null) : request); }
    @PostMapping("/reviews/{reviewId}/request-changes")
    public ArtifactReviewResponse requestChanges(Authentication authentication, @PathVariable UUID reviewId, @RequestBody(required = false) ReviewDecisionRequest request) { return artifacts.requestChanges(reviewId, user(authentication), request == null ? new ReviewDecisionRequest(null) : request); }

    @PostMapping("/projects/{projectId}/approvals")
    @ResponseStatus(HttpStatus.CREATED)
    public ArtifactApprovalResponse submitApproval(Authentication authentication, @PathVariable UUID projectId, @Valid @RequestBody SubmitApprovalRequest request) { return artifacts.submitApproval(projectId, user(authentication), request); }
    @PostMapping("/approvals/{approvalId}/approve")
    public ArtifactApprovalResponse approve(Authentication authentication, @PathVariable UUID approvalId, @RequestBody(required = false) ApprovalDecisionRequest request) { return artifacts.approve(approvalId, user(authentication), request == null ? new ApprovalDecisionRequest(null) : request); }
    @PostMapping("/approvals/{approvalId}/request-changes")
    public ArtifactApprovalResponse approvalChanges(Authentication authentication, @PathVariable UUID approvalId, @RequestBody(required = false) ApprovalDecisionRequest request) { return artifacts.requestApprovalChanges(approvalId, user(authentication), request == null ? new ApprovalDecisionRequest(null) : request); }
    @GetMapping("/projects/{projectId}/collaboration-policy")
    public ProjectCollaborationPolicyResponse policy(Authentication authentication, @PathVariable UUID projectId) { return artifacts.policy(projectId, user(authentication)); }

    @PostMapping("/projects/{projectId}/ai-proposals")
    @ResponseStatus(HttpStatus.CREATED)
    public AiArtifactProposalResponse createProposal(Authentication authentication, @PathVariable UUID projectId, @Valid @RequestBody CreateAiArtifactProposalRequest request) { return artifacts.createAiProposal(projectId, user(authentication), request); }
    @PostMapping("/ai-proposals/{proposalId}/accept")
    public AiArtifactProposalResponse acceptProposal(Authentication authentication, @PathVariable UUID proposalId) { return artifacts.acceptAiProposal(proposalId, user(authentication)); }

    @GetMapping("/projects/{projectId}/activity")
    public PageResponse<ProjectActivityResponse> activity(Authentication authentication, @PathVariable UUID projectId, @RequestParam(required = false) UUID member, @RequestParam(required = false) ProjectActivityType activityType, @RequestParam(required = false) CollaborationArtifactType artifactType, @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        return PageResponse.from(activities.list(projectId, user(authentication), member, activityType, artifactType, PageRequest.of(Math.max(0, page), Math.min(Math.max(1, size), MAX_PAGE_SIZE))));
    }

    @GetMapping("/projects/{projectId}/members/{memberId}/activity")
    public PageResponse<ProjectActivityResponse> memberActivity(Authentication authentication, @PathVariable UUID projectId, @PathVariable UUID memberId, @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        return PageResponse.from(activities.list(projectId, user(authentication), memberId, null, null, PageRequest.of(Math.max(0, page), Math.min(Math.max(1, size), MAX_PAGE_SIZE))));
    }

    @GetMapping("/projects/{projectId}/contributions")
    public ContributionSummaryResponse contributions(Authentication authentication, @PathVariable UUID projectId) {
        return contributions.summary(projectId, user(authentication));
    }

    private User user(Authentication authentication) {
        return users.requireActiveUser(authentication);
    }
}
