package com.researchassistant.subscription;

import com.researchassistant.identity.entity.User;
import com.researchassistant.identity.service.AuthenticatedUserResolver;
import com.researchassistant.subscription.dto.EntitlementResponse;
import com.researchassistant.workspace.service.WorkspaceAuthorizationService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/workspaces/{workspaceId}/entitlements")
public class EntitlementController {
    private final AuthenticatedUserResolver userResolver;
    private final WorkspaceAuthorizationService authorizationService;
    private final EntitlementService entitlementService;

    public EntitlementController(AuthenticatedUserResolver userResolver,
                                 WorkspaceAuthorizationService authorizationService,
                                 EntitlementService entitlementService) {
        this.userResolver = userResolver;
        this.authorizationService = authorizationService;
        this.entitlementService = entitlementService;
    }

    @GetMapping
    public List<EntitlementResponse> list(@PathVariable UUID workspaceId, Authentication authentication) {
        User user = userResolver.requireActiveUser(authentication);
        authorizationService.requireActiveMembership(workspaceId, user);
        return entitlementService.listEntitlements(workspaceId).stream()
                .map(e -> new EntitlementResponse(e.feature(), e.enabled(), e.limitValue(), e.limitUnit()))
                .toList();
    }
}
