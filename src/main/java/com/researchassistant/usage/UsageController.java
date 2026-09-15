package com.researchassistant.usage;

import com.researchassistant.identity.entity.User;
import com.researchassistant.identity.service.AuthenticatedUserResolver;
import com.researchassistant.workspace.service.WorkspaceAuthorizationService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/workspaces/{workspaceId}/usage")
public class UsageController {
    private final AuthenticatedUserResolver userResolver;
    private final WorkspaceAuthorizationService authorizationService;
    private final QuotaService quotaService;

    public UsageController(AuthenticatedUserResolver userResolver, WorkspaceAuthorizationService authorizationService, QuotaService quotaService) {
        this.userResolver = userResolver;
        this.authorizationService = authorizationService;
        this.quotaService = quotaService;
    }

    @GetMapping
    public WorkspaceUsageSummary usage(@PathVariable UUID workspaceId, Authentication authentication) {
        User user = userResolver.requireActiveUser(authentication);
        authorizationService.requireActiveMembership(workspaceId, user);
        return quotaService.currentSummary(workspaceId);
    }
}
