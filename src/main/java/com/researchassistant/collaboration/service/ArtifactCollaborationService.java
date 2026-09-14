package com.researchassistant.collaboration.service;

import com.researchassistant.ai.orchestration.AiTaskRequest;
import com.researchassistant.ai.orchestration.AiTaskResult;
import com.researchassistant.ai.orchestration.AiTaskType;
import com.researchassistant.ai.usage.AiRequest;
import com.researchassistant.ai.usage.AiRequestRepository;
import com.researchassistant.ai.usage.AiRequestStatus;
import com.researchassistant.ai.orchestration.ResearchAiOrchestrator;
import com.researchassistant.collaboration.dto.GeneratedArtifactProposalDraft;
import com.researchassistant.collaboration.dto.CollaborationDtos.*;
import com.researchassistant.collaboration.entity.*;
import com.researchassistant.collaboration.repository.*;
import com.researchassistant.common.exception.ResourceNotFoundException;
import com.researchassistant.identity.entity.User;
import com.researchassistant.project.entity.ProjectMembership;
import com.researchassistant.project.entity.ProjectMembershipStatus;
import com.researchassistant.project.entity.ProjectRole;
import com.researchassistant.project.entity.ResearchProject;
import com.researchassistant.project.exception.InvalidProjectOperationException;
import com.researchassistant.project.repository.ProjectMembershipRepository;
import com.researchassistant.project.repository.ResearchProjectRepository;
import com.researchassistant.project.service.ProjectAuthorizationContext;
import com.researchassistant.project.service.ProjectAuthorizationService;
import com.researchassistant.security.audit.SecurityAuditEventType;
import com.researchassistant.security.audit.SecurityAuditService;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class ArtifactCollaborationService {
    private final ArtifactCommentRepository commentRepository;
    private final ArtifactCommentMentionRepository mentionRepository;
    private final ArtifactReviewRepository reviewRepository;
    private final ArtifactApprovalRepository approvalRepository;
    private final ProjectCollaborationPolicyRepository policyRepository;
    private final AiArtifactProposalRepository proposalRepository;
    private final AiRequestRepository aiRequestRepository;
    private final ResearchProjectRepository projectRepository;
    private final ProjectMembershipRepository membershipRepository;
    private final ProjectAuthorizationService authorizationService;
    private final CollaborationArtifactResolver artifactResolver;
    private final ProjectActivityService activityService;
    private final ResearchAiOrchestrator aiOrchestrator;
    private final SecurityAuditService auditService;

    public ArtifactCollaborationService(ArtifactCommentRepository commentRepository, ArtifactCommentMentionRepository mentionRepository, ArtifactReviewRepository reviewRepository, ArtifactApprovalRepository approvalRepository, ProjectCollaborationPolicyRepository policyRepository, AiArtifactProposalRepository proposalRepository, AiRequestRepository aiRequestRepository, ResearchProjectRepository projectRepository, ProjectMembershipRepository membershipRepository, ProjectAuthorizationService authorizationService, CollaborationArtifactResolver artifactResolver, ProjectActivityService activityService, ResearchAiOrchestrator aiOrchestrator, SecurityAuditService auditService) {
        this.commentRepository = commentRepository;
        this.mentionRepository = mentionRepository;
        this.reviewRepository = reviewRepository;
        this.approvalRepository = approvalRepository;
        this.policyRepository = policyRepository;
        this.proposalRepository = proposalRepository;
        this.aiRequestRepository = aiRequestRepository;
        this.projectRepository = projectRepository;
        this.membershipRepository = membershipRepository;
        this.authorizationService = authorizationService;
        this.artifactResolver = artifactResolver;
        this.activityService = activityService;
        this.aiOrchestrator = aiOrchestrator;
        this.auditService = auditService;
    }

    public ArtifactCommentResponse createComment(UUID projectId, User actor, CreateCommentRequest request) {
        ProjectAuthorizationContext context = authorizationService.requireProjectViewer(projectId, actor);
        artifactResolver.requireArtifact(projectId, request.artifactType(), request.artifactId());
        ArtifactReview review = request.reviewId() == null ? null : reviewRepository.findByIdAndProjectId(request.reviewId(), projectId).orElseThrow(() -> new ResourceNotFoundException("Review not found."));
        ArtifactComment comment = new ArtifactComment();
        comment.setProject(context.project());
        comment.setArtifactType(request.artifactType());
        comment.setArtifactId(request.artifactId());
        comment.setAuthor(actor);
        comment.setReview(review);
        comment.setContent(required(request.content(), "Comment content is required."));
        comment = commentRepository.save(comment);
        saveMentions(comment, projectId, request.mentionedUserIds());
        auditService.record(actor.getId(), SecurityAuditEventType.ARTIFACT_COMMENT_CREATED);
        activityService.record(context.project(), actor, ProjectActivityType.COMMENT_CREATED, request.artifactType(), request.artifactId(), "Artifact comment created.");
        return toComment(comment);
    }

    @Transactional(readOnly = true)
    public Page<ArtifactCommentResponse> listComments(UUID projectId, User actor, CollaborationArtifactType artifactType, UUID artifactId, Pageable pageable) {
        authorizationService.requireProjectViewer(projectId, actor);
        Page<ArtifactComment> page = artifactType != null && artifactId != null
                ? commentRepository.findAllByProjectIdAndArtifactTypeAndArtifactId(projectId, artifactType, artifactId, pageable)
                : commentRepository.findAllByProjectId(projectId, pageable);
        return page.map(this::toComment);
    }

    public ArtifactCommentResponse reply(UUID commentId, User actor, ReplyCommentRequest request) {
        ArtifactComment parent = commentRepository.findById(commentId).orElseThrow(() -> new ResourceNotFoundException("Comment not found."));
        CreateCommentRequest create = new CreateCommentRequest(parent.getArtifactType(), parent.getArtifactId(), request.content(), request.mentionedUserIds(), parent.getReview() == null ? null : parent.getReview().getId());
        ArtifactCommentResponse response = createComment(parent.getProject().getId(), actor, create);
        ArtifactComment reply = commentRepository.getReferenceById(response.id());
        reply.setParentCommentId(parent.getId());
        return toComment(reply);
    }

    public ArtifactCommentResponse resolve(UUID commentId, User actor, boolean reopen) {
        ArtifactComment comment = commentRepository.findById(commentId).orElseThrow(() -> new ResourceNotFoundException("Comment not found."));
        authorizationService.requireProjectViewer(comment.getProject().getId(), actor);
        comment.setStatus(reopen ? ArtifactCommentStatus.OPEN : ArtifactCommentStatus.RESOLVED);
        comment.setResolvedAt(reopen ? null : OffsetDateTime.now());
        comment.setResolvedBy(reopen ? null : actor);
        if (!reopen) {
            auditService.record(actor.getId(), SecurityAuditEventType.ARTIFACT_COMMENT_RESOLVED);
            activityService.record(comment.getProject(), actor, ProjectActivityType.COMMENT_RESOLVED, comment.getArtifactType(), comment.getArtifactId(), "Artifact comment resolved.");
        }
        return toComment(comment);
    }

    public ArtifactReviewResponse requestReview(UUID projectId, User actor, CreateReviewRequest request) {
        ProjectAuthorizationContext context = authorizationService.requireProjectEditor(projectId, actor);
        artifactResolver.requireArtifact(projectId, request.artifactType(), request.artifactId());
        ProjectMembership reviewerMembership = activeMember(projectId, request.reviewerMemberId());
        ArtifactReview review = new ArtifactReview();
        review.setProject(context.project());
        review.setArtifactType(request.artifactType());
        review.setArtifactId(request.artifactId());
        review.setArtifactRevision(request.artifactRevision());
        review.setRequestedBy(actor);
        review.setReviewer(reviewerMembership.getUser());
        review.setSummary(blankToNull(request.summary()));
        review = reviewRepository.save(review);
        auditService.record(actor.getId(), SecurityAuditEventType.ARTIFACT_REVIEW_REQUESTED);
        activityService.record(context.project(), actor, ProjectActivityType.REVIEW_REQUESTED, request.artifactType(), request.artifactId(), "Artifact review requested.");
        return toReview(review);
    }

    @Transactional(readOnly = true)
    public Page<ArtifactReviewResponse> listReviews(UUID projectId, User actor, ArtifactReviewStatus status, Pageable pageable) {
        authorizationService.requireProjectViewer(projectId, actor);
        Page<ArtifactReview> page = status == null ? reviewRepository.findAllByProjectId(projectId, pageable) : reviewRepository.findAllByProjectIdAndStatus(projectId, status, pageable);
        return page.map(this::toReview);
    }

    @Transactional(readOnly = true)
    public ArtifactReviewResponse getReview(UUID reviewId, User actor) {
        ArtifactReview review = reviewRepository.findById(reviewId).orElseThrow(() -> new ResourceNotFoundException("Review not found."));
        authorizationService.requireProjectViewer(review.getProject().getId(), actor);
        return toReview(review);
    }

    public ArtifactReviewResponse startReview(UUID reviewId, User actor) {
        ArtifactReview review = reviewForReviewer(reviewId, actor);
        review.setStatus(ArtifactReviewStatus.IN_REVIEW);
        return toReview(review);
    }

    public ArtifactReviewResponse approveReview(UUID reviewId, User actor, ReviewDecisionRequest request) {
        ArtifactReview review = reviewForReviewer(reviewId, actor);
        review.setStatus(ArtifactReviewStatus.APPROVED);
        review.setSummary(blankToNull(request.summary()));
        review.setCompletedAt(OffsetDateTime.now());
        auditService.record(actor.getId(), SecurityAuditEventType.ARTIFACT_REVIEW_APPROVED);
        activityService.record(review.getProject(), actor, ProjectActivityType.REVIEW_APPROVED, review.getArtifactType(), review.getArtifactId(), "Artifact review approved.");
        return toReview(review);
    }

    public ArtifactReviewResponse requestChanges(UUID reviewId, User actor, ReviewDecisionRequest request) {
        ArtifactReview review = reviewForReviewer(reviewId, actor);
        review.setStatus(ArtifactReviewStatus.CHANGES_REQUESTED);
        review.setSummary(blankToNull(request.summary()));
        review.setCompletedAt(OffsetDateTime.now());
        auditService.record(actor.getId(), SecurityAuditEventType.ARTIFACT_REVIEW_CHANGES_REQUESTED);
        activityService.record(review.getProject(), actor, ProjectActivityType.REVIEW_CHANGES_REQUESTED, review.getArtifactType(), review.getArtifactId(), "Artifact changes requested.");
        return toReview(review);
    }

    public ArtifactApprovalResponse submitApproval(UUID projectId, User actor, SubmitApprovalRequest request) {
        ProjectAuthorizationContext context = authorizationService.requireProjectEditor(projectId, actor);
        artifactResolver.requireArtifact(projectId, request.artifactType(), request.artifactId());
        ArtifactApproval approval = new ArtifactApproval();
        approval.setProject(context.project());
        approval.setArtifactType(request.artifactType());
        approval.setArtifactId(request.artifactId());
        approval.setArtifactRevision(request.artifactRevision());
        approval.setSubmittedBy(actor);
        approval = approvalRepository.save(approval);
        auditService.record(actor.getId(), SecurityAuditEventType.ARTIFACT_APPROVAL_SUBMITTED);
        activityService.record(context.project(), actor, ProjectActivityType.APPROVAL_SUBMITTED, request.artifactType(), request.artifactId(), "Artifact approval submitted.");
        return toApproval(approval);
    }

    public ArtifactApprovalResponse approve(UUID approvalId, User actor, ApprovalDecisionRequest request) {
        ArtifactApproval approval = approvalRepository.findById(approvalId).orElseThrow(() -> new ResourceNotFoundException("Approval not found."));
        requireReviewerOrLead(approval.getProject().getId(), actor);
        approval.setStatus(ArtifactApprovalStatus.APPROVED);
        approval.setDecidedBy(actor);
        approval.setDecisionComment(blankToNull(request.decisionComment()));
        approval.setDecidedAt(OffsetDateTime.now());
        auditService.record(actor.getId(), SecurityAuditEventType.ARTIFACT_APPROVED);
        activityService.record(approval.getProject(), actor, ProjectActivityType.ARTIFACT_APPROVED, approval.getArtifactType(), approval.getArtifactId(), "Artifact approved.");
        return toApproval(approval);
    }

    public ArtifactApprovalResponse requestApprovalChanges(UUID approvalId, User actor, ApprovalDecisionRequest request) {
        ArtifactApproval approval = approvalRepository.findById(approvalId).orElseThrow(() -> new ResourceNotFoundException("Approval not found."));
        requireReviewerOrLead(approval.getProject().getId(), actor);
        approval.setStatus(ArtifactApprovalStatus.CHANGES_REQUESTED);
        approval.setDecidedBy(actor);
        approval.setDecisionComment(blankToNull(request.decisionComment()));
        approval.setDecidedAt(OffsetDateTime.now());
        return toApproval(approval);
    }

    public ProjectCollaborationPolicyResponse policy(UUID projectId, User actor) {
        ResearchProject project = authorizationService.requireProjectViewer(projectId, actor).project();
        ProjectCollaborationPolicy policy = policyRepository.findByProjectId(projectId).orElseGet(() -> {
            ProjectCollaborationPolicy created = new ProjectCollaborationPolicy();
            created.setProject(project);
            return policyRepository.save(created);
        });
        return new ProjectCollaborationPolicyResponse(projectId, policy.isRequireReviewBeforeApproval(), policy.isAllowLeadSelfApproval(), policy.isSupervisorApprovalRequired(), policy.getMinimumReviewers(), policy.isAllowEditorsInviteMembers(), policy.isCommentsEnabled(), policy.isTaskAssignmentsEnabled());
    }

    public AiArtifactProposalResponse createAiProposal(UUID projectId, User actor, CreateAiArtifactProposalRequest request) {
        ProjectAuthorizationContext context = authorizationService.requireProjectEditor(projectId, actor);
        int currentRevision = artifactResolver.requireArtifact(projectId, request.artifactType(), request.artifactId()).revision();
        if (request.basedOnRevision() != null && request.basedOnRevision() != currentRevision) {
            throw new InvalidProjectOperationException("AI proposal base revision is stale.");
        }
        String prompt = """
                Return JSON only with this schema: {"proposedContent":"..."}.
                Draft a proposed revision for the requested project artifact. Do not overwrite authoritative project data.

                USER INSTRUCTION:
                """ + request.prompt();
        AiTaskRequest aiRequest = new AiTaskRequest(AiTaskType.REPORT_SECTION_DRAFT, actor.getId(), context.project().getWorkspace().getId(), projectId, request.prompt(), prompt, null, null, GeneratedArtifactProposalDraft.class, true);
        AiTaskResult<GeneratedArtifactProposalDraft> result = aiOrchestrator.executeTask(actor, aiRequest, GeneratedArtifactProposalDraft.class);
        if (result.status() != AiRequestStatus.COMPLETED || result.result() == null) {
            throw new InvalidProjectOperationException("AI proposal generation failed.");
        }
        AiArtifactProposal proposal = new AiArtifactProposal();
        proposal.setProject(context.project());
        proposal.setArtifactType(request.artifactType());
        proposal.setArtifactId(request.artifactId());
        proposal.setBasedOnRevision(currentRevision);
        proposal.setAiRequest(aiRequestRepository.findById(result.requestId()).orElse(null));
        proposal.setProposedContent(result.result().proposedContent());
        proposal.setRequestedBy(actor);
        proposal = proposalRepository.save(proposal);
        auditService.record(actor.getId(), SecurityAuditEventType.AI_ARTIFACT_PROPOSAL_CREATED);
        activityService.record(context.project(), actor, ProjectActivityType.AI_ARTIFACT_PROPOSAL_CREATED, request.artifactType(), request.artifactId(), "AI artifact proposal created.");
        return toProposal(proposal);
    }

    public AiArtifactProposalResponse acceptAiProposal(UUID proposalId, User actor) {
        AiArtifactProposal proposal = proposalRepository.findById(proposalId).orElseThrow(() -> new ResourceNotFoundException("AI proposal not found."));
        authorizationService.requireProjectEditor(proposal.getProject().getId(), actor);
        int currentRevision = artifactResolver.revision(proposal.getArtifactType(), proposal.getArtifactId());
        if (proposal.getBasedOnRevision() != null && proposal.getBasedOnRevision() != currentRevision) {
            proposal.setStatus(AiArtifactProposalStatus.STALE);
            throw new InvalidProjectOperationException("AI proposal is stale and cannot overwrite the current artifact.");
        }
        proposal.setStatus(AiArtifactProposalStatus.ACCEPTED);
        proposal.setAcceptedBy(actor);
        proposal.setAcceptedAt(OffsetDateTime.now());
        auditService.record(actor.getId(), SecurityAuditEventType.AI_ARTIFACT_PROPOSAL_ACCEPTED);
        activityService.record(proposal.getProject(), actor, ProjectActivityType.AI_ARTIFACT_PROPOSAL_ACCEPTED, proposal.getArtifactType(), proposal.getArtifactId(), "AI artifact proposal accepted.");
        return toProposal(proposal);
    }

    private ArtifactReview reviewForReviewer(UUID reviewId, User actor) {
        ArtifactReview review = reviewRepository.findById(reviewId).orElseThrow(() -> new ResourceNotFoundException("Review not found."));
        requireReviewerOrLead(review.getProject().getId(), actor);
        return review;
    }

    private void requireReviewerOrLead(UUID projectId, User actor) {
        ProjectAuthorizationContext context = authorizationService.requireProjectViewer(projectId, actor);
        ProjectRole role = context.projectMembership().map(ProjectMembership::getRole).orElse(null);
        if (authorizationService.isWorkspaceAdmin(context.workspaceMembership()) || role == ProjectRole.LEAD || role == ProjectRole.REVIEWER || role == ProjectRole.SUPERVISOR) return;
        throw new InvalidProjectOperationException("Reviewer, supervisor or lead access is required.");
    }

    public void staleRevisionBoundRecords(UUID projectId, CollaborationArtifactType type, UUID artifactId, int currentRevision) {
        reviewRepository.findAllByProjectIdAndArtifactTypeAndArtifactIdAndStatusNot(projectId, type, artifactId, ArtifactReviewStatus.STALE).forEach(review -> { if (review.getArtifactRevision() < currentRevision) review.setStatus(ArtifactReviewStatus.STALE); });
        approvalRepository.findAllByProjectIdAndArtifactTypeAndArtifactIdAndStatusNot(projectId, type, artifactId, ArtifactApprovalStatus.STALE).forEach(approval -> { if (approval.getArtifactRevision() < currentRevision) approval.setStatus(ArtifactApprovalStatus.STALE); });
    }

    private void saveMentions(ArtifactComment comment, UUID projectId, List<UUID> mentionedUserIds) {
        if (mentionedUserIds == null) return;
        for (UUID userId : mentionedUserIds.stream().distinct().toList()) {
            membershipRepository.findByProjectIdAndUserIdAndStatus(projectId, userId, ProjectMembershipStatus.ACTIVE)
                    .orElseThrow(() -> new InvalidProjectOperationException("Mentioned user must be an active project member."));
            ArtifactCommentMention mention = new ArtifactCommentMention();
            mention.setComment(comment);
            mention.setMentionedUser(membershipRepository.findByProjectIdAndUserIdAndStatus(projectId, userId, ProjectMembershipStatus.ACTIVE).orElseThrow().getUser());
            mentionRepository.save(mention);
        }
    }

    private ProjectMembership activeMember(UUID projectId, UUID memberId) {
        return membershipRepository.findByIdAndProjectIdAndStatus(memberId, projectId, ProjectMembershipStatus.ACTIVE)
                .orElseThrow(() -> new InvalidProjectOperationException("Reviewer must be an active project member."));
    }

    private ArtifactCommentResponse toComment(ArtifactComment comment) {
        List<UUID> mentions = mentionRepository.findAllByCommentId(comment.getId()).stream().map(m -> m.getMentionedUser().getId()).toList();
        return new ArtifactCommentResponse(comment.getId(), comment.getProject().getId(), comment.getArtifactType(), comment.getArtifactId(), comment.getAuthor().getId(), comment.getParentCommentId(), comment.getReview() == null ? null : comment.getReview().getId(), comment.getContent(), comment.getStatus(), mentions, comment.getCreatedAt(), comment.getUpdatedAt(), comment.getResolvedAt(), comment.getResolvedBy() == null ? null : comment.getResolvedBy().getId());
    }

    private ArtifactReviewResponse toReview(ArtifactReview review) {
        return new ArtifactReviewResponse(review.getId(), review.getProject().getId(), review.getArtifactType(), review.getArtifactId(), review.getArtifactRevision(), review.getRequestedBy().getId(), review.getReviewer().getId(), review.getStatus(), review.getSummary(), review.getRequestedAt(), review.getCompletedAt());
    }

    private ArtifactApprovalResponse toApproval(ArtifactApproval approval) {
        return new ArtifactApprovalResponse(approval.getId(), approval.getProject().getId(), approval.getArtifactType(), approval.getArtifactId(), approval.getArtifactRevision(), approval.getStatus(), approval.getSubmittedBy().getId(), approval.getDecidedBy() == null ? null : approval.getDecidedBy().getId(), approval.getDecisionComment(), approval.getSubmittedAt(), approval.getDecidedAt());
    }

    private AiArtifactProposalResponse toProposal(AiArtifactProposal proposal) {
        AiRequest request = proposal.getAiRequest();
        return new AiArtifactProposalResponse(proposal.getId(), proposal.getProject().getId(), proposal.getArtifactType(), proposal.getArtifactId(), proposal.getBasedOnRevision(), request == null ? null : request.getId(), proposal.getProposedContent(), proposal.getStatus(), proposal.getRequestedBy().getId(), proposal.getAcceptedBy() == null ? null : proposal.getAcceptedBy().getId(), proposal.getCreatedAt(), proposal.getAcceptedAt());
    }

    private String required(String value, String message) {
        if (value == null || value.isBlank()) throw new InvalidProjectOperationException(message);
        return value.trim();
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
