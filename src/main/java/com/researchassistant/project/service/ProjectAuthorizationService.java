package com.researchassistant.project.service;

import com.researchassistant.identity.entity.User;
import com.researchassistant.project.entity.ProjectMembership;
import com.researchassistant.project.entity.ProjectMembershipStatus;
import com.researchassistant.project.entity.ProjectRole;
import com.researchassistant.project.entity.ResearchProject;
import com.researchassistant.project.exception.ProjectAccessDeniedException;
import com.researchassistant.project.exception.ResearchProjectNotFoundException;
import com.researchassistant.project.repository.ProjectMembershipRepository;
import com.researchassistant.project.repository.ResearchProjectRepository;
import com.researchassistant.workspace.entity.WorkspaceMembership;
import com.researchassistant.workspace.entity.WorkspaceRole;
import com.researchassistant.workspace.service.WorkspaceAuthorizationService;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

/**
 * Central project authorization boundary.
 *
 * <p>Project UUIDs are not tenant proof. Every project access first
 * resolves the project, then requires ACTIVE membership in the parent
 * workspace. Workspace OWNER and ADMIN users have administrative
 * project access without explicit project membership; workspace
 * MEMBER users need ACTIVE project membership.</p>
 */
@Service
@Transactional(readOnly = true)
public class ProjectAuthorizationService {

    private final ResearchProjectRepository projectRepository;
    private final ProjectMembershipRepository membershipRepository;
    private final WorkspaceAuthorizationService workspaceAuthorizationService;

    public ProjectAuthorizationService(
            ResearchProjectRepository projectRepository,
            ProjectMembershipRepository membershipRepository,
            WorkspaceAuthorizationService workspaceAuthorizationService
    ) {
        this.projectRepository = projectRepository;
        this.membershipRepository = membershipRepository;
        this.workspaceAuthorizationService = workspaceAuthorizationService;
    }

    public ProjectAuthorizationContext requireProjectAccess(
            UUID projectId,
            User user
    ) {
        ResearchProject project = projectRepository.findById(projectId)
                .orElseThrow(ResearchProjectNotFoundException::new);

        WorkspaceMembership workspaceMembership =
                workspaceAuthorizationService.requireActiveMembership(
                        project.getWorkspace().getId(),
                        user
                );

        Optional<ProjectMembership> projectMembership =
                membershipRepository.findByProjectIdAndUserIdAndStatus(
                        projectId,
                        user.getId(),
                        ProjectMembershipStatus.ACTIVE
                );

        if (isWorkspaceAdmin(workspaceMembership)
                || projectMembership.isPresent()) {
            return new ProjectAuthorizationContext(
                    project,
                    workspaceMembership,
                    projectMembership
            );
        }

        throw new ResearchProjectNotFoundException();
    }

    public ProjectAuthorizationContext requireProjectViewer(
            UUID projectId,
            User user
    ) {
        return requireProjectAccess(projectId, user);
    }

    public ProjectAuthorizationContext requireProjectEditor(
            UUID projectId,
            User user
    ) {
        ProjectAuthorizationContext context =
                requireProjectAccess(projectId, user);

        if (isWorkspaceAdmin(context.workspaceMembership())
                || context.projectMembership()
                .map(ProjectMembership::getRole)
                .filter(role -> role == ProjectRole.LEAD
                        || role == ProjectRole.EDITOR)
                .isPresent()) {
            return context;
        }

        throw new ProjectAccessDeniedException(
                "Project editor access is required."
        );
    }

    public ProjectAuthorizationContext requireProjectLead(
            UUID projectId,
            User user
    ) {
        ProjectAuthorizationContext context =
                requireProjectAccess(projectId, user);

        if (context.projectMembership()
                .map(ProjectMembership::getRole)
                .filter(role -> role == ProjectRole.LEAD)
                .isPresent()) {
            return context;
        }

        throw new ProjectAccessDeniedException(
                "Project lead access is required."
        );
    }

    public ProjectAuthorizationContext requireProjectAdminAccess(
            UUID projectId,
            User user
    ) {
        ProjectAuthorizationContext context =
                requireProjectAccess(projectId, user);

        if (isWorkspaceAdmin(context.workspaceMembership())
                || context.projectMembership()
                .map(ProjectMembership::getRole)
                .filter(role -> role == ProjectRole.LEAD)
                .isPresent()) {
            return context;
        }

        throw new ProjectAccessDeniedException(
                "Project administrator access is required."
        );
    }

    public boolean isWorkspaceAdmin(WorkspaceMembership membership) {
        return membership.getRole() == WorkspaceRole.OWNER
                || membership.getRole() == WorkspaceRole.ADMIN;
    }
}
