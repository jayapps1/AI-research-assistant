package com.researchassistant.project.controller;

import com.researchassistant.identity.entity.User;
import com.researchassistant.identity.service.AuthenticatedUserResolver;
import com.researchassistant.project.dto.AddProjectMemberRequest;
import com.researchassistant.project.dto.ChangeProjectMemberRoleRequest;
import com.researchassistant.project.dto.CreateResearchProjectRequest;
import com.researchassistant.project.dto.PageResponse;
import com.researchassistant.project.dto.ProjectMemberResponse;
import com.researchassistant.project.dto.ResearchProjectResponse;
import com.researchassistant.project.dto.UpdateResearchProjectRequest;
import com.researchassistant.project.service.ResearchProjectService;

import jakarta.validation.Valid;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
public class ResearchProjectController {

    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int MAX_PAGE_SIZE = 100;

    private final ResearchProjectService projectService;
    private final AuthenticatedUserResolver authenticatedUserResolver;

    public ResearchProjectController(
            ResearchProjectService projectService,
            AuthenticatedUserResolver authenticatedUserResolver
    ) {
        this.projectService = projectService;
        this.authenticatedUserResolver = authenticatedUserResolver;
    }

    @PostMapping("/workspaces/{workspaceId}/projects")
    @ResponseStatus(HttpStatus.CREATED)
    public ResearchProjectResponse createProject(
            Authentication authentication,
            @PathVariable UUID workspaceId,
            @Valid @RequestBody CreateResearchProjectRequest request
    ) {
        User user = authenticatedUserResolver.requireActiveUser(authentication);
        return projectService.createProject(workspaceId, user, request);
    }

    @GetMapping("/workspaces/{workspaceId}/projects")
    public PageResponse<ResearchProjectResponse> listWorkspaceProjects(
            Authentication authentication,
            @PathVariable UUID workspaceId,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) com.researchassistant.project.entity.ResearchProjectStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        User user = authenticatedUserResolver.requireActiveUser(authentication);
        return PageResponse.from(projectService.listWorkspaceProjects(
                workspaceId,
                user,
                q,
                status,
                pageable(page, size)
        ));
    }

    @GetMapping("/projects/mine")
    public PageResponse<ResearchProjectResponse> listMyProjects(
            Authentication authentication,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) com.researchassistant.project.entity.ResearchProjectStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        User user = authenticatedUserResolver.requireActiveUser(authentication);
        return PageResponse.from(projectService.listMyProjects(
                user,
                q,
                status,
                pageable(page, size)
        ));
    }

    @GetMapping("/projects/{projectId}")
    public ResearchProjectResponse getProject(
            Authentication authentication,
            @PathVariable UUID projectId
    ) {
        User user = authenticatedUserResolver.requireActiveUser(authentication);
        return projectService.getProject(projectId, user);
    }

    @PatchMapping("/projects/{projectId}")
    public ResearchProjectResponse updateProject(
            Authentication authentication,
            @PathVariable UUID projectId,
            @Valid @RequestBody UpdateResearchProjectRequest request
    ) {
        User user = authenticatedUserResolver.requireActiveUser(authentication);
        return projectService.updateProject(projectId, user, request);
    }

    @PostMapping("/projects/{projectId}/activate")
    public ResearchProjectResponse activateProject(
            Authentication authentication,
            @PathVariable UUID projectId
    ) {
        User user = authenticatedUserResolver.requireActiveUser(authentication);
        return projectService.activateProject(projectId, user);
    }

    @PostMapping("/projects/{projectId}/complete")
    public ResearchProjectResponse completeProject(
            Authentication authentication,
            @PathVariable UUID projectId
    ) {
        User user = authenticatedUserResolver.requireActiveUser(authentication);
        return projectService.completeProject(projectId, user);
    }

    @PostMapping("/projects/{projectId}/archive")
    public ResearchProjectResponse archiveProject(
            Authentication authentication,
            @PathVariable UUID projectId
    ) {
        User user = authenticatedUserResolver.requireActiveUser(authentication);
        return projectService.archiveProject(projectId, user);
    }

    @GetMapping("/projects/{projectId}/members")
    public List<ProjectMemberResponse> listMembers(
            Authentication authentication,
            @PathVariable UUID projectId
    ) {
        User user = authenticatedUserResolver.requireActiveUser(authentication);
        return projectService.listMembers(
                projectId,
                user,
                PageRequest.of(0, MAX_PAGE_SIZE)
        );
    }

    @PostMapping("/projects/{projectId}/members")
    @ResponseStatus(HttpStatus.CREATED)
    public ProjectMemberResponse addMember(
            Authentication authentication,
            @PathVariable UUID projectId,
            @Valid @RequestBody AddProjectMemberRequest request
    ) {
        User user = authenticatedUserResolver.requireActiveUser(authentication);
        return projectService.addMember(projectId, user, request);
    }

    @PatchMapping("/projects/{projectId}/members/{userId}/role")
    public ProjectMemberResponse changeMemberRole(
            Authentication authentication,
            @PathVariable UUID projectId,
            @PathVariable UUID userId,
            @Valid @RequestBody ChangeProjectMemberRoleRequest request
    ) {
        User user = authenticatedUserResolver.requireActiveUser(authentication);
        return projectService.changeMemberRole(
                projectId,
                userId,
                user,
                request
        );
    }

    @DeleteMapping("/projects/{projectId}/members/{userId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeMember(
            Authentication authentication,
            @PathVariable UUID projectId,
            @PathVariable UUID userId
    ) {
        User user = authenticatedUserResolver.requireActiveUser(authentication);
        projectService.removeMember(projectId, userId, user);
    }

    private Pageable pageable(int page, int size) {
        int safePage = Math.max(0, page);
        int safeSize = size <= 0
                ? DEFAULT_PAGE_SIZE
                : Math.min(size, MAX_PAGE_SIZE);
        return PageRequest.of(safePage, safeSize);
    }
}
