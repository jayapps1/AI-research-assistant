package com.researchassistant.admin;

import com.researchassistant.audit.AuditEvent;
import com.researchassistant.audit.AuditEventRepository;
import com.researchassistant.identity.entity.User;
import com.researchassistant.identity.entity.UserStatus;
import com.researchassistant.identity.repository.UserRepository;
import com.researchassistant.identity.service.AuthenticatedUserResolver;
import com.researchassistant.subscription.*;
import com.researchassistant.workspace.entity.Workspace;
import com.researchassistant.workspace.repository.WorkspaceRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin")
public class AdminController {
    private final AuthenticatedUserResolver userResolver;
    private final SystemAdminAuthorizationService adminAuthorizationService;
    private final AdminDashboardService dashboardService;
    private final UserRepository userRepository;
    private final WorkspaceRepository workspaceRepository;
    private final SubscriptionPlanRepository planRepository;
    private final PlanEntitlementRepository entitlementRepository;
    private final AuditEventRepository auditEventRepository;
    private final ComplimentaryAccessService complimentaryAccessService;

    public AdminController(AuthenticatedUserResolver userResolver, SystemAdminAuthorizationService adminAuthorizationService,
                           AdminDashboardService dashboardService, UserRepository userRepository, WorkspaceRepository workspaceRepository,
                           SubscriptionPlanRepository planRepository, PlanEntitlementRepository entitlementRepository,
                           AuditEventRepository auditEventRepository, ComplimentaryAccessService complimentaryAccessService) {
        this.userResolver = userResolver;
        this.adminAuthorizationService = adminAuthorizationService;
        this.dashboardService = dashboardService;
        this.userRepository = userRepository;
        this.workspaceRepository = workspaceRepository;
        this.planRepository = planRepository;
        this.entitlementRepository = entitlementRepository;
        this.auditEventRepository = auditEventRepository;
        this.complimentaryAccessService = complimentaryAccessService;
    }

    @GetMapping("/dashboard")
    public Map<String, Object> dashboard(Authentication authentication) {
        requireAdmin(authentication);
        return dashboardService.dashboard();
    }

    @GetMapping("/operations/status")
    public Map<String, Object> operations(Authentication authentication) {
        requireAdmin(authentication);
        return Map.of("database", "UP", "paystack", "TEST_MODE_CONFIGURED_WHEN_KEYS_PRESENT",
                "email", "CONFIGURATION_DEPENDENT", "arkesel", "DISABLED_BY_DEFAULT",
                "firebase", "DISABLED_BY_DEFAULT");
    }

    @GetMapping("/users")
    public Page<Map<String, Object>> users(Pageable pageable, Authentication authentication) {
        requireAdmin(authentication);
        return userRepository.findAll(pageable).map(u -> Map.of("id", u.getId(), "email", u.getEmail(),
                "firstName", u.getFirstName(), "lastName", u.getLastName(), "status", u.getStatus()));
    }

    @GetMapping("/users/{userId}")
    public Map<String, Object> user(@PathVariable UUID userId, Authentication authentication) {
        requireAdmin(authentication);
        User u = userRepository.findById(userId).orElseThrow();
        return Map.of("id", u.getId(), "email", u.getEmail(), "firstName", u.getFirstName(),
                "lastName", u.getLastName(), "status", u.getStatus(), "createdAt", u.getCreatedAt());
    }

    @PostMapping("/users/{userId}/suspend")
    public void suspend(@PathVariable UUID userId, Authentication authentication) {
        requireAdmin(authentication);
        userRepository.findById(userId).orElseThrow().setStatus(UserStatus.SUSPENDED);
    }

    @PostMapping("/users/{userId}/reactivate")
    public void reactivate(@PathVariable UUID userId, Authentication authentication) {
        requireAdmin(authentication);
        userRepository.findById(userId).orElseThrow().setStatus(UserStatus.ACTIVE);
    }

    @GetMapping("/workspaces")
    public Page<Map<String, Object>> workspaces(Pageable pageable, Authentication authentication) {
        requireAdmin(authentication);
        return workspaceRepository.findAll(pageable).map(w -> Map.of("id", w.getId(), "name", w.getName(),
                "type", w.getType(), "status", w.getStatus(), "ownerId", w.getOwner().getId()));
    }

    @GetMapping("/workspaces/{workspaceId}")
    public Map<String, Object> workspace(@PathVariable UUID workspaceId, Authentication authentication) {
        requireAdmin(authentication);
        Workspace w = workspaceRepository.findById(workspaceId).orElseThrow();
        return Map.of("id", w.getId(), "name", w.getName(), "type", w.getType(),
                "status", w.getStatus(), "ownerId", w.getOwner().getId());
    }

    @GetMapping("/subscription-plans")
    public Page<SubscriptionPlan> plans(Pageable pageable, Authentication authentication) {
        requireAdmin(authentication);
        return planRepository.findAll(pageable);
    }

    @PostMapping("/subscription-plans")
    @CacheEvict(cacheNames = "subscription:workspace-entitlements", allEntries = true)
    public SubscriptionPlan createPlan(@Valid @RequestBody PlanRequest request, Authentication authentication) {
        requireAdmin(authentication);
        SubscriptionPlan plan = new SubscriptionPlan();
        apply(plan, request);
        return planRepository.save(plan);
    }

    @PatchMapping("/subscription-plans/{planId}")
    @CacheEvict(cacheNames = "subscription:workspace-entitlements", allEntries = true)
    public SubscriptionPlan updatePlan(@PathVariable UUID planId, @RequestBody PlanRequest request, Authentication authentication) {
        requireAdmin(authentication);
        SubscriptionPlan plan = planRepository.findById(planId).orElseThrow();
        apply(plan, request);
        return planRepository.save(plan);
    }

    @GetMapping("/audit-events")
    public Page<AuditEvent> audit(Pageable pageable, Authentication authentication) {
        requireAdmin(authentication);
        return auditEventRepository.findAllByOrderByOccurredAtDesc(pageable);
    }

    @PostMapping("/complimentary-access")
    public Map<String, Object> grantComplimentaryAccess(@Valid @RequestBody ComplimentaryAccessService.GrantRequest request,
                                                        Authentication authentication) {
        User admin = requireAdmin(authentication);
        return toGrantResponse(complimentaryAccessService.grant(request, admin));
    }

    @GetMapping("/complimentary-access")
    public Page<Map<String, Object>> complimentaryAccess(Pageable pageable, Authentication authentication) {
        requireAdmin(authentication);
        return complimentaryAccessService.list(pageable).map(this::toGrantResponse);
    }

    @GetMapping("/complimentary-access/{grantId}")
    public Map<String, Object> complimentaryAccessGrant(@PathVariable UUID grantId, Authentication authentication) {
        requireAdmin(authentication);
        return toGrantResponse(complimentaryAccessService.get(grantId));
    }

    @PostMapping("/complimentary-access/{grantId}/revoke")
    public Map<String, Object> revokeComplimentaryAccess(@PathVariable UUID grantId,
                                                         @RequestBody RevokeGrantRequest request,
                                                         Authentication authentication) {
        User admin = requireAdmin(authentication);
        return toGrantResponse(complimentaryAccessService.revoke(grantId, request == null ? null : request.reason(), admin));
    }

    private void apply(SubscriptionPlan plan, PlanRequest request) {
        if (request.code() != null) plan.setCode(request.code());
        if (request.name() != null) plan.setName(request.name());
        if (request.description() != null) plan.setDescription(request.description());
        if (request.status() != null) plan.setStatus(request.status());
        if (request.billingInterval() != null) plan.setBillingInterval(request.billingInterval());
        if (request.price() != null) plan.setPrice(request.price());
        if (request.currency() != null) plan.setCurrency(request.currency());
        if (request.publiclyAvailable() != null) plan.setPubliclyAvailable(request.publiclyAvailable());
        if (request.displayOrder() != null) plan.setDisplayOrder(request.displayOrder());
    }

    private User requireAdmin(Authentication authentication) {
        User user = userResolver.requireActiveUser(authentication);
        adminAuthorizationService.requireSystemAdmin(user.getId());
        return user;
    }

    private Map<String, Object> toGrantResponse(ComplimentaryAccessGrant grant) {
        return Map.of(
                "id", grant.getId(),
                "scope", grant.getScope(),
                "userId", grant.getUser() == null ? "" : grant.getUser().getId(),
                "workspaceId", grant.getWorkspace() == null ? "" : grant.getWorkspace().getId(),
                "planCode", grant.getPlan() == null ? "" : grant.getPlan().getCode(),
                "type", grant.getType(),
                "status", grant.getStatus(),
                "startsAt", grant.getStartsAt(),
                "expiresAt", grant.getExpiresAt() == null ? "" : grant.getExpiresAt()
        );
    }

    public record PlanRequest(@NotBlank String code, @NotBlank String name, String description,
                              SubscriptionPlanStatus status, @NotNull BillingInterval billingInterval,
                              @NotNull BigDecimal price, @NotBlank String currency, Boolean publiclyAvailable,
                              Integer displayOrder) {}
    public record RevokeGrantRequest(String reason) {}
}
