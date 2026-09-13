package com.researchassistant.workspace.controller;

import com.researchassistant.identity.entity.User;
import com.researchassistant.identity.service.AuthenticatedUserResolver;
import com.researchassistant.workspace.dto.AddWorkspaceMemberRequest;
import com.researchassistant.workspace.dto.ChangeWorkspaceMemberRoleRequest;
import com.researchassistant.workspace.dto.CreateWorkspaceRequest;
import com.researchassistant.workspace.dto.UpdateWorkspaceRequest;
import com.researchassistant.workspace.dto.WorkspaceMemberResponse;
import com.researchassistant.workspace.dto.WorkspaceResponse;
import com.researchassistant.workspace.service.WorkspaceService;

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
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/workspaces")
public class WorkspaceController {

    private final WorkspaceService workspaceService;
    private final AuthenticatedUserResolver authenticatedUserResolver;

    public WorkspaceController(
            WorkspaceService workspaceService,
            AuthenticatedUserResolver authenticatedUserResolver
    ) {
        this.workspaceService = workspaceService;
        this.authenticatedUserResolver = authenticatedUserResolver;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public WorkspaceResponse createWorkspace(
            Authentication authentication,
            @Valid @RequestBody CreateWorkspaceRequest request
    ) {
        User user = authenticatedUserResolver.requireActiveUser(authentication);
        return workspaceService.createWorkspace(user, request);
    }

    @GetMapping
    public List<WorkspaceResponse> listWorkspaces(
            Authentication authentication
    ) {
        User user = authenticatedUserResolver.requireActiveUser(authentication);
        return workspaceService.listWorkspaces(user);
    }

    @GetMapping("/{workspaceId}")
    public WorkspaceResponse getWorkspace(
            Authentication authentication,
            @PathVariable UUID workspaceId
    ) {
        User user = authenticatedUserResolver.requireActiveUser(authentication);
        return workspaceService.getWorkspace(workspaceId, user);
    }

    @PatchMapping("/{workspaceId}")
    public WorkspaceResponse updateWorkspace(
            Authentication authentication,
            @PathVariable UUID workspaceId,
            @Valid @RequestBody UpdateWorkspaceRequest request
    ) {
        User user = authenticatedUserResolver.requireActiveUser(authentication);
        return workspaceService.updateWorkspace(workspaceId, user, request);
    }

    @PostMapping("/{workspaceId}/archive")
    public WorkspaceResponse archiveWorkspace(
            Authentication authentication,
            @PathVariable UUID workspaceId
    ) {
        User user = authenticatedUserResolver.requireActiveUser(authentication);
        return workspaceService.archiveWorkspace(workspaceId, user);
    }

    @GetMapping("/{workspaceId}/members")
    public List<WorkspaceMemberResponse> listMembers(
            Authentication authentication,
            @PathVariable UUID workspaceId
    ) {
        User user = authenticatedUserResolver.requireActiveUser(authentication);
        Pageable pageable = PageRequest.of(0, 100);
        return workspaceService.listMembers(workspaceId, user, pageable);
    }

    @PostMapping("/{workspaceId}/members")
    @ResponseStatus(HttpStatus.CREATED)
    public WorkspaceMemberResponse addMember(
            Authentication authentication,
            @PathVariable UUID workspaceId,
            @Valid @RequestBody AddWorkspaceMemberRequest request
    ) {
        User user = authenticatedUserResolver.requireActiveUser(authentication);
        return workspaceService.addMember(workspaceId, user, request);
    }

    @PatchMapping("/{workspaceId}/members/{userId}/role")
    public WorkspaceMemberResponse changeMemberRole(
            Authentication authentication,
            @PathVariable UUID workspaceId,
            @PathVariable UUID userId,
            @Valid @RequestBody ChangeWorkspaceMemberRoleRequest request
    ) {
        User user = authenticatedUserResolver.requireActiveUser(authentication);
        return workspaceService.changeMemberRole(
                workspaceId,
                userId,
                user,
                request
        );
    }

    @DeleteMapping("/{workspaceId}/members/{userId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeMember(
            Authentication authentication,
            @PathVariable UUID workspaceId,
            @PathVariable UUID userId
    ) {
        User user = authenticatedUserResolver.requireActiveUser(authentication);
        workspaceService.removeMember(workspaceId, userId, user);
    }
}
