package com.researchassistant.project.service;

import com.researchassistant.common.exception.DuplicateResourceException;
import com.researchassistant.common.exception.ResourceNotFoundException;
import com.researchassistant.identity.entity.User;
import com.researchassistant.identity.repository.UserRepository;
import com.researchassistant.project.dto.AddProjectMemberRequest;
import com.researchassistant.project.dto.ChangeProjectMemberRoleRequest;
import com.researchassistant.project.dto.CreateResearchProjectRequest;
import com.researchassistant.project.dto.ProjectMemberResponse;
import com.researchassistant.project.dto.ResearchProjectResponse;
import com.researchassistant.project.dto.UpdateResearchProjectRequest;
import com.researchassistant.project.entity.ProjectMembership;
import com.researchassistant.project.entity.ProjectMembershipStatus;
import com.researchassistant.project.entity.ProjectRole;
import com.researchassistant.project.entity.ResearchProject;
import com.researchassistant.project.entity.ResearchProjectStatus;
import com.researchassistant.project.exception.InvalidProjectOperationException;
import com.researchassistant.project.exception.ResearchProjectNotFoundException;
import com.researchassistant.project.repository.ProjectMembershipRepository;
import com.researchassistant.project.repository.ResearchProjectRepository;
import com.researchassistant.security.audit.SecurityAuditEventType;
import com.researchassistant.security.audit.SecurityAuditService;
import com.researchassistant.workspace.entity.Workspace;
import com.researchassistant.workspace.entity.WorkspaceMembership;
import com.researchassistant.workspace.entity.WorkspaceMembershipStatus;
import com.researchassistant.workspace.repository.WorkspaceMembershipRepository;
import com.researchassistant.workspace.service.WorkspaceAuthorizationService;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
@Transactional
public class ResearchProjectService {

    private final ResearchProjectRepository projectRepository;
    private final ProjectMembershipRepository membershipRepository;
    private final UserRepository userRepository;
    private final WorkspaceMembershipRepository workspaceMembershipRepository;
    private final WorkspaceAuthorizationService workspaceAuthorizationService;
    private final ProjectAuthorizationService authorizationService;
    private final SecurityAuditService auditService;

    public ResearchProjectService(
            ResearchProjectRepository projectRepository,
            ProjectMembershipRepository membershipRepository,
            UserRepository userRepository,
            WorkspaceMembershipRepository workspaceMembershipRepository,
            WorkspaceAuthorizationService workspaceAuthorizationService,
            ProjectAuthorizationService authorizationService,
            SecurityAuditService auditService
    ) {
        this.projectRepository = projectRepository;
        this.membershipRepository = membershipRepository;
        this.userRepository = userRepository;
        this.workspaceMembershipRepository = workspaceMembershipRepository;
        this.workspaceAuthorizationService = workspaceAuthorizationService;
        this.authorizationService = authorizationService;
        this.auditService = auditService;
    }

    public ResearchProjectResponse createProject(
            UUID workspaceId,
            User currentUser,
            CreateResearchProjectRequest request
    ) {
        if (!workspaceId.equals(request.workspaceId())) {
            throw new InvalidProjectOperationException(
                    "Request workspace does not match path workspace."
            );
        }

        WorkspaceMembership workspaceMembership =
                workspaceAuthorizationService.requireAdminOrOwner(
                        workspaceId,
                        currentUser
                );

        Workspace workspace = workspaceMembership.getWorkspace();

        ResearchProject project = new ResearchProject();
        project.setWorkspace(workspace);
        project.setTitle(normalizeRequiredTitle(request.title()));
        project.setDescription(normalizeOptionalText(request.description()));
        project.setStatus(ResearchProjectStatus.DRAFT);
        project.setCreatedBy(currentUser);
        project.setNextDocumentNumber(1L);

        ResearchProject savedProject = projectRepository.save(project);

        ProjectMembership membership = new ProjectMembership();
        membership.setProject(savedProject);
        membership.setUser(currentUser);
        membership.setRole(ProjectRole.LEAD);
        membership.setStatus(ProjectMembershipStatus.ACTIVE);
        membership.setAddedBy(currentUser);
        membership.setJoinedAt(OffsetDateTime.now());
        membershipRepository.save(membership);

        auditService.record(
                currentUser.getId(),
                SecurityAuditEventType.RESEARCH_PROJECT_CREATED
        );

        return toProjectResponse(savedProject, membership);
    }

    @Transactional(readOnly = true)
    public Page<ResearchProjectResponse> listWorkspaceProjects(
            UUID workspaceId,
            User currentUser,
            Pageable pageable
    ) {
        WorkspaceMembership workspaceMembership =
                workspaceAuthorizationService.requireActiveMembership(
                        workspaceId,
                        currentUser
                );

        Page<ResearchProject> projects =
                authorizationService.isWorkspaceAdmin(workspaceMembership)
                        ? projectRepository.findAllByWorkspaceId(
                        workspaceId,
                        pageable
                )
                        : projectRepository.findAuthorizedMemberProjects(
                        workspaceId,
                        currentUser.getId(),
                        pageable
                );

        return projects.map(project -> toProjectResponse(
                project,
                membershipRepository
                        .findByProjectIdAndUserIdAndStatus(
                                project.getId(),
                                currentUser.getId(),
                                ProjectMembershipStatus.ACTIVE
                        )
                        .orElse(null)
        ));
    }

    @Transactional(readOnly = true)
    public ResearchProjectResponse getProject(
            UUID projectId,
            User currentUser
    ) {
        ProjectAuthorizationContext context =
                authorizationService.requireProjectViewer(
                        projectId,
                        currentUser
                );

        return toProjectResponse(
                context.project(),
                context.projectMembership().orElse(null)
        );
    }

    public ResearchProjectResponse updateProject(
            UUID projectId,
            User currentUser,
            UpdateResearchProjectRequest request
    ) {
        ProjectAuthorizationContext context =
                authorizationService.requireProjectAdminAccess(
                        projectId,
                        currentUser
                );

        ResearchProject project = context.project();

        if (request.title() != null) {
            project.setTitle(normalizeRequiredTitle(request.title()));
        }
        if (request.description() != null) {
            project.setDescription(normalizeOptionalText(request.description()));
        }

        auditService.record(
                currentUser.getId(),
                SecurityAuditEventType.RESEARCH_PROJECT_UPDATED
        );

        return toProjectResponse(
                project,
                context.projectMembership().orElse(null)
        );
    }

    public ResearchProjectResponse activateProject(
            UUID projectId,
            User currentUser
    ) {
        ProjectAuthorizationContext context =
                authorizationService.requireProjectAdminAccess(
                        projectId,
                        currentUser
                );

        ResearchProject project = context.project();
        if (project.getStatus() != ResearchProjectStatus.DRAFT
                && project.getStatus() != ResearchProjectStatus.COMPLETED) {
            throw new InvalidProjectOperationException(
                    "Only draft or completed projects can be activated."
            );
        }
        project.setStatus(ResearchProjectStatus.ACTIVE);

        auditService.record(
                currentUser.getId(),
                SecurityAuditEventType.RESEARCH_PROJECT_ACTIVATED
        );

        return toProjectResponse(
                project,
                context.projectMembership().orElse(null)
        );
    }

    public ResearchProjectResponse completeProject(
            UUID projectId,
            User currentUser
    ) {
        ProjectAuthorizationContext context =
                authorizationService.requireProjectAdminAccess(
                        projectId,
                        currentUser
                );

        ResearchProject project = context.project();
        if (project.getStatus() == ResearchProjectStatus.ARCHIVED) {
            throw new InvalidProjectOperationException(
                    "Archived projects cannot be completed."
            );
        }
        project.setStatus(ResearchProjectStatus.COMPLETED);

        auditService.record(
                currentUser.getId(),
                SecurityAuditEventType.RESEARCH_PROJECT_COMPLETED
        );

        return toProjectResponse(
                project,
                context.projectMembership().orElse(null)
        );
    }

    public ResearchProjectResponse archiveProject(
            UUID projectId,
            User currentUser
    ) {
        ProjectAuthorizationContext context =
                authorizationService.requireProjectAdminAccess(
                        projectId,
                        currentUser
                );

        ResearchProject project = context.project();
        project.setStatus(ResearchProjectStatus.ARCHIVED);

        auditService.record(
                currentUser.getId(),
                SecurityAuditEventType.RESEARCH_PROJECT_ARCHIVED
        );

        return toProjectResponse(
                project,
                context.projectMembership().orElse(null)
        );
    }

    @Transactional(readOnly = true)
    public List<ProjectMemberResponse> listMembers(
            UUID projectId,
            User currentUser,
            Pageable pageable
    ) {
        authorizationService.requireProjectViewer(projectId, currentUser);

        return membershipRepository.findAllByProjectIdAndStatus(
                        projectId,
                        ProjectMembershipStatus.ACTIVE,
                        pageable
                )
                .stream()
                .map(this::toMemberResponse)
                .toList();
    }

    public ProjectMemberResponse addMember(
            UUID projectId,
            User currentUser,
            AddProjectMemberRequest request
    ) {
        ProjectAuthorizationContext context =
                authorizationService.requireProjectAdminAccess(
                        projectId,
                        currentUser
                );

        boolean workspaceAdmin =
                authorizationService.isWorkspaceAdmin(
                        context.workspaceMembership()
                );

        if (request.role() == ProjectRole.LEAD && !workspaceAdmin) {
            throw new InvalidProjectOperationException(
                    "Only workspace owners or admins may add a project lead."
            );
        }

        String normalizedEmail =
                request.email().trim().toLowerCase(Locale.ROOT);

        User targetUser = userRepository.findByEmailIgnoreCase(normalizedEmail)
                .orElseThrow(() ->
                        new ResourceNotFoundException("User not found.")
                );

        workspaceMembershipRepository
                .findByWorkspaceIdAndUserIdAndStatus(
                        context.project().getWorkspace().getId(),
                        targetUser.getId(),
                        WorkspaceMembershipStatus.ACTIVE
                )
                .orElseThrow(() ->
                        new InvalidProjectOperationException(
                                "User must belong to the workspace before joining the project."
                        )
                );

        if (membershipRepository.findByProjectIdAndUserId(
                projectId,
                targetUser.getId()
        ).isPresent()) {
            throw new DuplicateResourceException(
                    "User already has a project membership."
            );
        }

        ProjectMembership membership = new ProjectMembership();
        membership.setProject(context.project());
        membership.setUser(targetUser);
        membership.setRole(request.role());
        membership.setStatus(ProjectMembershipStatus.ACTIVE);
        membership.setAddedBy(currentUser);
        membership.setJoinedAt(OffsetDateTime.now());

        ProjectMembership savedMembership =
                membershipRepository.save(membership);

        auditService.record(
                currentUser.getId(),
                SecurityAuditEventType.PROJECT_MEMBER_ADDED
        );

        return toMemberResponse(savedMembership);
    }

    public ProjectMemberResponse changeMemberRole(
            UUID projectId,
            UUID targetUserId,
            User currentUser,
            ChangeProjectMemberRoleRequest request
    ) {
        ProjectAuthorizationContext context =
                authorizationService.requireProjectAdminAccess(
                        projectId,
                        currentUser
                );

        boolean workspaceAdmin =
                authorizationService.isWorkspaceAdmin(
                        context.workspaceMembership()
                );

        ProjectMembership targetMembership =
                membershipRepository.findByProjectIdAndUserIdAndStatus(
                                projectId,
                                targetUserId,
                                ProjectMembershipStatus.ACTIVE
                        )
                        .orElseThrow(ResearchProjectNotFoundException::new);

        membershipRepository.findActiveByProjectIdForUpdate(projectId);

        if (request.role() == ProjectRole.LEAD && !workspaceAdmin) {
            throw new InvalidProjectOperationException(
                    "Only workspace owners or admins may assign project lead."
            );
        }

        if (targetMembership.getRole() == ProjectRole.LEAD
                && request.role() != ProjectRole.LEAD
                && activeLeadCount(projectId) <= 1) {
            throw new InvalidProjectOperationException(
                    "Project must retain at least one active lead."
            );
        }

        targetMembership.setRole(request.role());

        auditService.record(
                currentUser.getId(),
                SecurityAuditEventType.PROJECT_MEMBER_ROLE_CHANGED
        );

        return toMemberResponse(targetMembership);
    }

    public void removeMember(
            UUID projectId,
            UUID targetUserId,
            User currentUser
    ) {
        ProjectAuthorizationContext context =
                authorizationService.requireProjectAdminAccess(
                        projectId,
                        currentUser
                );

        ProjectMembership targetMembership =
                membershipRepository.findByProjectIdAndUserIdAndStatus(
                                projectId,
                                targetUserId,
                                ProjectMembershipStatus.ACTIVE
                        )
                        .orElseThrow(ResearchProjectNotFoundException::new);

        membershipRepository.findActiveByProjectIdForUpdate(projectId);

        if (targetMembership.getRole() == ProjectRole.LEAD
                && activeLeadCount(projectId) <= 1) {
            throw new InvalidProjectOperationException(
                    "Project must retain at least one active lead."
            );
        }

        targetMembership.setStatus(ProjectMembershipStatus.REMOVED);

        auditService.record(
                currentUser.getId(),
                SecurityAuditEventType.PROJECT_MEMBER_REMOVED
        );
    }

    private long activeLeadCount(UUID projectId) {
        return membershipRepository.countByProjectIdAndRoleAndStatus(
                projectId,
                ProjectRole.LEAD,
                ProjectMembershipStatus.ACTIVE
        );
    }

    private ResearchProjectResponse toProjectResponse(
            ResearchProject project,
            ProjectMembership currentUserMembership
    ) {
        return new ResearchProjectResponse(
                project.getId(),
                project.getWorkspace().getId(),
                project.getTitle(),
                project.getDescription(),
                project.getStatus(),
                project.getCreatedBy().getId(),
                currentUserMembership == null
                        ? null
                        : currentUserMembership.getRole(),
                project.getCreatedAt(),
                project.getUpdatedAt()
        );
    }

    private ProjectMemberResponse toMemberResponse(
            ProjectMembership membership
    ) {
        User user = membership.getUser();
        return new ProjectMemberResponse(
                user.getId(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                membership.getRole(),
                membership.getStatus(),
                membership.getJoinedAt()
        );
    }

    private String normalizeRequiredTitle(String title) {
        String normalized = title == null ? "" : title.trim();
        if (normalized.isEmpty()) {
            throw new InvalidProjectOperationException(
                    "Project title is required."
            );
        }
        return normalized;
    }

    private String normalizeOptionalText(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }
}
