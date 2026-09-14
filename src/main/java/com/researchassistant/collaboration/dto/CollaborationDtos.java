package com.researchassistant.collaboration.dto;

import com.researchassistant.collaboration.entity.*;
import com.researchassistant.project.entity.ProjectRole;

import jakarta.validation.constraints.*;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public final class CollaborationDtos {
    private CollaborationDtos() {
    }

    public record CreateProjectInvitationRequest(
            @Email @NotBlank String email,
            @NotNull ProjectRole role
    ) {
    }

    public record ProjectInvitationResponse(
            UUID id,
            UUID projectId,
            String invitedEmailNormalized,
            ProjectRole role,
            ProjectInvitationStatus status,
            UUID invitedBy,
            OffsetDateTime createdAt,
            OffsetDateTime expiresAt,
            OffsetDateTime acceptedAt,
            OffsetDateTime declinedAt,
            OffsetDateTime revokedAt,
            String deliveryToken
    ) {
    }

    public record ChangeMemberRoleRequest(@NotNull ProjectRole role) {
    }

    public record TransferLeadRequest(boolean demoteCurrentLead) {
    }

    public record CreateReportAuthorRequest(
            UUID userId,
            @NotBlank @Size(max = 255) String displayName,
            @Size(max = 100) String studentNumber,
            @Size(max = 100) String indexNumber,
            @Size(max = 255) String programme,
            @Min(1) int authorOrder,
            boolean correspondingAuthor
    ) {
    }

    public record ReportAuthorResponse(
            UUID id,
            UUID reportId,
            UUID userId,
            String displayName,
            String studentNumber,
            String indexNumber,
            String programme,
            int authorOrder,
            boolean correspondingAuthor
    ) {
    }

    public record ArtifactLinkRequest(
            @NotNull CollaborationArtifactType artifactType,
            @NotNull UUID artifactId
    ) {
    }

    public record CreateProjectTaskRequest(
            @NotBlank @Size(max = 255) String title,
            String description,
            ProjectTaskPriority priority,
            LocalDate dueDate,
            OffsetDateTime dueAt,
            List<UUID> assigneeMemberIds,
            List<ArtifactLinkRequest> artifactLinks
    ) {
    }

    public record UpdateProjectTaskRequest(
            @Size(max = 255) String title,
            String description,
            ProjectTaskPriority priority,
            LocalDate dueDate,
            OffsetDateTime dueAt,
            Long expectedVersion
    ) {
    }

    public record AddTaskAssigneeRequest(@NotNull UUID memberId) {
    }

    public record ProjectTaskResponse(
            UUID id,
            UUID projectId,
            String title,
            String description,
            ProjectTaskStatus status,
            ProjectTaskPriority priority,
            UUID createdBy,
            UUID assignedBy,
            LocalDate dueDate,
            OffsetDateTime dueAt,
            OffsetDateTime completedAt,
            Long version,
            boolean overdue,
            List<UUID> assigneeMemberIds,
            List<ArtifactLinkResponse> artifactLinks,
            OffsetDateTime createdAt,
            OffsetDateTime updatedAt
    ) {
    }

    public record ArtifactLinkResponse(UUID id, CollaborationArtifactType artifactType, UUID artifactId) {
    }

    public record CreateCommentRequest(
            @NotNull CollaborationArtifactType artifactType,
            @NotNull UUID artifactId,
            @NotBlank String content,
            List<UUID> mentionedUserIds,
            UUID reviewId
    ) {
    }

    public record ReplyCommentRequest(@NotBlank String content, List<UUID> mentionedUserIds) {
    }

    public record ArtifactCommentResponse(
            UUID id,
            UUID projectId,
            CollaborationArtifactType artifactType,
            UUID artifactId,
            UUID authorId,
            UUID parentCommentId,
            UUID reviewId,
            String content,
            ArtifactCommentStatus status,
            List<UUID> mentionedUserIds,
            OffsetDateTime createdAt,
            OffsetDateTime updatedAt,
            OffsetDateTime resolvedAt,
            UUID resolvedBy
    ) {
    }

    public record CreateReviewRequest(
            @NotNull CollaborationArtifactType artifactType,
            @NotNull UUID artifactId,
            @Min(1) int artifactRevision,
            @NotNull UUID reviewerMemberId,
            String summary
    ) {
    }

    public record ReviewDecisionRequest(String summary) {
    }

    public record ArtifactReviewResponse(
            UUID id,
            UUID projectId,
            CollaborationArtifactType artifactType,
            UUID artifactId,
            int artifactRevision,
            UUID requestedBy,
            UUID reviewerId,
            ArtifactReviewStatus status,
            String summary,
            OffsetDateTime requestedAt,
            OffsetDateTime completedAt
    ) {
    }

    public record ProjectActivityResponse(
            UUID id,
            UUID projectId,
            UUID actorId,
            ProjectActivityType type,
            CollaborationArtifactType artifactType,
            UUID artifactId,
            String safeSummary,
            String metadataJson,
            OffsetDateTime occurredAt
    ) {
    }

    public record ContributionMemberResponse(
            UUID userId,
            String email,
            long tasksCompleted,
            long commentsCreated,
            long reviewsCompleted,
            long documentsUploaded,
            long aiDraftsRequested,
            long aiDraftsAccepted
    ) {
    }

    public record ContributionSummaryResponse(UUID projectId, List<ContributionMemberResponse> members) {
    }

    public record SubmitApprovalRequest(
            @NotNull CollaborationArtifactType artifactType,
            @NotNull UUID artifactId,
            @Min(1) int artifactRevision
    ) {
    }

    public record ApprovalDecisionRequest(String decisionComment) {
    }

    public record ArtifactApprovalResponse(
            UUID id,
            UUID projectId,
            CollaborationArtifactType artifactType,
            UUID artifactId,
            int artifactRevision,
            ArtifactApprovalStatus status,
            UUID submittedBy,
            UUID decidedBy,
            String decisionComment,
            OffsetDateTime submittedAt,
            OffsetDateTime decidedAt
    ) {
    }

    public record ProjectCollaborationPolicyResponse(
            UUID projectId,
            boolean requireReviewBeforeApproval,
            boolean allowLeadSelfApproval,
            boolean supervisorApprovalRequired,
            int minimumReviewers,
            boolean allowEditorsInviteMembers,
            boolean commentsEnabled,
            boolean taskAssignmentsEnabled
    ) {
    }

    public record CreateAiArtifactProposalRequest(
            @NotNull CollaborationArtifactType artifactType,
            @NotNull UUID artifactId,
            @Min(1) Integer basedOnRevision,
            @NotBlank String prompt
    ) {
    }

    public record AiArtifactProposalResponse(
            UUID id,
            UUID projectId,
            CollaborationArtifactType artifactType,
            UUID artifactId,
            Integer basedOnRevision,
            UUID aiRequestId,
            String proposedContent,
            AiArtifactProposalStatus status,
            UUID requestedBy,
            UUID acceptedBy,
            OffsetDateTime createdAt,
            OffsetDateTime acceptedAt
    ) {
    }
}
