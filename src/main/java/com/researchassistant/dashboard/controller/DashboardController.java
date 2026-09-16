package com.researchassistant.dashboard.controller;

import com.researchassistant.dashboard.dto.DashboardDtos.*;
import com.researchassistant.dashboard.service.DashboardService;
import com.researchassistant.identity.entity.User;
import com.researchassistant.identity.service.AuthenticatedUserResolver;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
public class DashboardController {

    private final DashboardService dashboardService;
    private final AuthenticatedUserResolver authenticatedUserResolver;

    public DashboardController(
            DashboardService dashboardService,
            AuthenticatedUserResolver authenticatedUserResolver
    ) {
        this.dashboardService = dashboardService;
        this.authenticatedUserResolver = authenticatedUserResolver;
    }

    @GetMapping("/dashboard")
    public UserDashboardResponse getUserDashboard(Authentication authentication) {
        User user = authenticatedUserResolver.requireActiveUser(authentication);
        return dashboardService.getUserDashboard(user);
    }

    @GetMapping("/workspaces/{workspaceId}/dashboard")
    public WorkspaceDashboardResponse getWorkspaceDashboard(
            Authentication authentication,
            @PathVariable UUID workspaceId
    ) {
        User user = authenticatedUserResolver.requireActiveUser(authentication);
        return dashboardService.getWorkspaceDashboard(workspaceId, user);
    }

    @GetMapping("/projects/{projectId}/dashboard")
    public ProjectDashboardResponse getProjectDashboard(
            Authentication authentication,
            @PathVariable UUID projectId
    ) {
        User user = authenticatedUserResolver.requireActiveUser(authentication);
        return dashboardService.getProjectDashboard(projectId, user);
    }

    @GetMapping("/projects/{projectId}/research-progress")
    public ResearchProgressResponse getResearchProgress(
            Authentication authentication,
            @PathVariable UUID projectId
    ) {
        User user = authenticatedUserResolver.requireActiveUser(authentication);
        return dashboardService.getResearchProgress(projectId, user);
    }
}
