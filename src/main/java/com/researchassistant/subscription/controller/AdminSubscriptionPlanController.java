package com.researchassistant.subscription.controller;

import com.researchassistant.admin.SystemAdminAuthorizationService;
import com.researchassistant.audit.AuditEventService;
import com.researchassistant.audit.AuditEventType;
import com.researchassistant.cache.AppCacheNames;
import com.researchassistant.common.exception.DuplicateResourceException;
import com.researchassistant.common.exception.ResourceNotFoundException;
import com.researchassistant.identity.entity.User;
import com.researchassistant.identity.service.AuthenticatedUserResolver;
import com.researchassistant.subscription.*;
import com.researchassistant.subscription.dto.AdminPlanDtos.*;
import jakarta.validation.Valid;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.*;

@RestController
@RequestMapping("/api/v1/admin/subscription-plans")
public class AdminSubscriptionPlanController {

    private final AuthenticatedUserResolver userResolver;
    private final SystemAdminAuthorizationService adminAuthorizationService;
    private final SubscriptionPlanRepository planRepository;
    private final PlanEntitlementRepository entitlementRepository;
    private final WorkspaceSubscriptionRepository workspaceSubscriptionRepository;
    private final SubscriptionPlanPriceRepository planPriceRepository;
    private final AuditEventService auditEventService;

    public AdminSubscriptionPlanController(
            AuthenticatedUserResolver userResolver,
            SystemAdminAuthorizationService adminAuthorizationService,
            SubscriptionPlanRepository planRepository,
            PlanEntitlementRepository entitlementRepository,
            WorkspaceSubscriptionRepository workspaceSubscriptionRepository,
            SubscriptionPlanPriceRepository planPriceRepository,
            AuditEventService auditEventService
    ) {
        this.userResolver = userResolver;
        this.adminAuthorizationService = adminAuthorizationService;
        this.planRepository = planRepository;
        this.entitlementRepository = entitlementRepository;
        this.workspaceSubscriptionRepository = workspaceSubscriptionRepository;
        this.planPriceRepository = planPriceRepository;
        this.auditEventService = auditEventService;
    }

    @GetMapping
    @Transactional(readOnly = true)
    public Page<AdminSubscriptionPlanResponse> listPlans(Pageable pageable, Authentication authentication) {
        requireAdmin(authentication);
        Page<SubscriptionPlan> page = planRepository.findAll(pageable);
        List<AdminSubscriptionPlanResponse> dtos = page.getContent().stream()
                .map(this::toResponse)
                .toList();
        return new PageImpl<>(dtos, pageable, page.getTotalElements());
    }

    @PostMapping
    @Transactional
    @ResponseStatus(org.springframework.http.HttpStatus.CREATED)
    @CacheEvict(cacheNames = {AppCacheNames.WORKSPACE_ENTITLEMENTS, AppCacheNames.PUBLIC_PRICING}, allEntries = true)
    public AdminSubscriptionPlanResponse createPlan(
            @Valid @RequestBody CreateSubscriptionPlanRequest request,
            Authentication authentication
    ) {
        User admin = requireAdmin(authentication);

        String normalizedCode = request.code().trim().toUpperCase();
        if (planRepository.existsByCodeIgnoreCase(normalizedCode)) {
            throw new DuplicateResourceException("Subscription plan with code '" + normalizedCode + "' already exists.");
        }

        if ("FREE".equalsIgnoreCase(normalizedCode) && request.price().compareTo(BigDecimal.ZERO) != 0) {
            throw new IllegalArgumentException("FREE plan price must be exactly 0.");
        }
        if (!"FREE".equalsIgnoreCase(normalizedCode) && request.price().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Paid subscription plans must have a price greater than 0.");
        }

        SubscriptionPlan plan = new SubscriptionPlan();
        plan.setCode(normalizedCode);
        plan.setName(request.name().trim());
        plan.setDescription(request.description() != null ? request.description().trim() : null);
        plan.setStatus(request.status() != null ? request.status() : SubscriptionPlanStatus.ACTIVE);
        plan.setBillingInterval(request.billingInterval());
        plan.setPrice(request.price());
        plan.setCurrency(request.currency().trim().toUpperCase());
        plan.setPubliclyAvailable(Boolean.TRUE.equals(request.publiclyAvailable()));
        plan.setFeatured(Boolean.TRUE.equals(request.featured()));
        plan.setDisplayOrder(request.displayOrder() != null ? request.displayOrder() : 0);

        SubscriptionPlan saved = planRepository.save(plan);
        savePlanPrice(saved, BillingInterval.MONTHLY, saved.getPrice(), saved.getCurrency());
        if (request.yearlyPrice() != null && request.yearlyPrice().compareTo(BigDecimal.ZERO) > 0) {
            savePlanPrice(saved, BillingInterval.YEARLY, request.yearlyPrice(), saved.getCurrency());
        }

        auditEventService.record(admin.getId(), "ADMIN", null, null, AuditEventType.PLAN_CREATED,
                "SubscriptionPlan", saved.getId(), "{\"code\":\"" + saved.getCode() + "\",\"price\":" + saved.getPrice() + "}");

        return toResponse(saved);
    }

    @GetMapping("/{id}")
    @Transactional(readOnly = true)
    public AdminSubscriptionPlanResponse getPlan(@PathVariable UUID id, Authentication authentication) {
        requireAdmin(authentication);
        SubscriptionPlan plan = planRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Subscription plan not found: " + id));
        return toResponse(plan);
    }

    @PatchMapping("/{id}")
    @Transactional
    @CacheEvict(cacheNames = {AppCacheNames.WORKSPACE_ENTITLEMENTS, AppCacheNames.PUBLIC_PRICING}, allEntries = true)
    public AdminSubscriptionPlanResponse updatePlan(
            @PathVariable UUID id,
            @RequestBody UpdateSubscriptionPlanRequest request,
            Authentication authentication
    ) {
        User admin = requireAdmin(authentication);
        SubscriptionPlan plan = planRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Subscription plan not found: " + id));

        if (request.name() != null && !request.name().isBlank()) {
            plan.setName(request.name().trim());
        }
        if (request.description() != null) {
            plan.setDescription(request.description().trim());
        }
        if (request.status() != null) {
            plan.setStatus(request.status());
        }
        if (request.billingInterval() != null) {
            plan.setBillingInterval(request.billingInterval());
        }
        if (request.price() != null) {
            if ("FREE".equalsIgnoreCase(plan.getCode()) && request.price().compareTo(BigDecimal.ZERO) != 0) {
                throw new IllegalArgumentException("FREE plan price must remain 0.");
            }
            if (!"FREE".equalsIgnoreCase(plan.getCode()) && request.price().compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException("Paid subscription plans must have a price greater than 0.");
            }
            plan.setPrice(request.price());
            savePlanPrice(plan, BillingInterval.MONTHLY, request.price(), plan.getCurrency());
        }
        if (request.yearlyPrice() != null) {
            if (request.yearlyPrice().compareTo(BigDecimal.ZERO) > 0) {
                savePlanPrice(plan, BillingInterval.YEARLY, request.yearlyPrice(), plan.getCurrency());
            } else {
                planPriceRepository.findByPlanIdAndBillingIntervalAndActiveTrue(plan.getId(), BillingInterval.YEARLY)
                        .ifPresent(p -> {
                            p.setActive(false);
                            planPriceRepository.save(p);
                        });
            }
        }
        if (request.currency() != null && !request.currency().isBlank()) {
            plan.setCurrency(request.currency().trim().toUpperCase());
        }
        if (request.publiclyAvailable() != null) {
            plan.setPubliclyAvailable(request.publiclyAvailable());
        }
        if (request.featured() != null) {
            plan.setFeatured(request.featured());
        }
        if (request.displayOrder() != null) {
            plan.setDisplayOrder(request.displayOrder());
        }

        SubscriptionPlan saved = planRepository.save(plan);

        auditEventService.record(admin.getId(), "ADMIN", null, null, AuditEventType.PLAN_UPDATED,
                "SubscriptionPlan", saved.getId(), "{\"code\":\"" + saved.getCode() + "\"}");

        return toResponse(saved);
    }

    @PostMapping("/{id}/activate")
    @Transactional
    @CacheEvict(cacheNames = {AppCacheNames.WORKSPACE_ENTITLEMENTS, AppCacheNames.PUBLIC_PRICING}, allEntries = true)
    public AdminSubscriptionPlanResponse activatePlan(@PathVariable UUID id, Authentication authentication) {
        User admin = requireAdmin(authentication);
        SubscriptionPlan plan = planRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Subscription plan not found: " + id));

        plan.setStatus(SubscriptionPlanStatus.ACTIVE);
        SubscriptionPlan saved = planRepository.save(plan);

        auditEventService.record(admin.getId(), "ADMIN", null, null, AuditEventType.PLAN_ACTIVATED,
                "SubscriptionPlan", saved.getId(), "{\"code\":\"" + saved.getCode() + "\"}");

        return toResponse(saved);
    }

    @PostMapping("/{id}/deactivate")
    @Transactional
    @CacheEvict(cacheNames = {AppCacheNames.WORKSPACE_ENTITLEMENTS, AppCacheNames.PUBLIC_PRICING}, allEntries = true)
    public AdminSubscriptionPlanResponse deactivatePlan(@PathVariable UUID id, Authentication authentication) {
        User admin = requireAdmin(authentication);
        SubscriptionPlan plan = planRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Subscription plan not found: " + id));

        plan.setStatus(SubscriptionPlanStatus.INACTIVE);
        SubscriptionPlan saved = planRepository.save(plan);

        auditEventService.record(admin.getId(), "ADMIN", null, null, AuditEventType.PLAN_DEACTIVATED,
                "SubscriptionPlan", saved.getId(), "{\"code\":\"" + saved.getCode() + "\"}");

        return toResponse(saved);
    }

    @GetMapping("/{id}/entitlements")
    @Transactional(readOnly = true)
    public List<AdminPlanEntitlementDto> getPlanEntitlements(@PathVariable UUID id, Authentication authentication) {
        requireAdmin(authentication);
        SubscriptionPlan plan = planRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Subscription plan not found: " + id));

        Map<PlanFeature, PlanEntitlement> existingMap = new HashMap<>();
        for (PlanEntitlement e : entitlementRepository.findAllByPlanId(plan.getId())) {
            existingMap.put(e.getFeature(), e);
        }

        List<AdminPlanEntitlementDto> results = new ArrayList<>();
        for (PlanFeature feature : PlanFeature.values()) {
            PlanEntitlement existing = existingMap.get(feature);
            if (existing != null) {
                LimitMode mode = !existing.isEnabled()
                        ? LimitMode.DISABLED
                        : (existing.getLimitValue() == null ? LimitMode.UNLIMITED : LimitMode.LIMITED);
                String formatted = formatLimit(feature, existing.getLimitValue(), existing.getLimitUnit());
                results.add(new AdminPlanEntitlementDto(feature, existing.isEnabled(), mode, existing.getLimitValue(), existing.getLimitUnit(), formatted));
            } else {
                LimitUnit defaultUnit = defaultUnitForFeature(feature);
                results.add(new AdminPlanEntitlementDto(feature, false, LimitMode.DISABLED, null, defaultUnit, "Disabled"));
            }
        }
        return results;
    }

    @PutMapping("/{id}/entitlements")
    @Transactional
    @CacheEvict(cacheNames = {AppCacheNames.WORKSPACE_ENTITLEMENTS, AppCacheNames.PUBLIC_PRICING}, allEntries = true)
    public List<AdminPlanEntitlementDto> updatePlanEntitlements(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateEntitlementsRequest request,
            Authentication authentication
    ) {
        User admin = requireAdmin(authentication);
        SubscriptionPlan plan = planRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Subscription plan not found: " + id));

        Map<PlanFeature, PlanEntitlement> existingMap = new HashMap<>();
        for (PlanEntitlement e : entitlementRepository.findAllByPlanId(plan.getId())) {
            existingMap.put(e.getFeature(), e);
        }

        List<AdminPlanEntitlementDto> responseList = new ArrayList<>();

        for (AdminPlanEntitlementDto item : request.entitlements()) {
            PlanEntitlement entitlement = existingMap.computeIfAbsent(item.feature(), f -> {
                PlanEntitlement pe = new PlanEntitlement();
                pe.setPlan(plan);
                pe.setFeature(f);
                return pe;
            });

            boolean enabled = item.limitMode() != LimitMode.DISABLED && item.enabled();
            Long limitValue = null;
            if (enabled && item.limitMode() == LimitMode.LIMITED) {
                limitValue = item.limitValue() != null ? Math.max(0L, item.limitValue()) : 0L;
            }

            LimitUnit unit = item.limitUnit() != null ? item.limitUnit() : defaultUnitForFeature(item.feature());

            entitlement.setEnabled(enabled);
            entitlement.setLimitValue(limitValue);
            entitlement.setLimitUnit(unit);

            PlanEntitlement saved = entitlementRepository.save(entitlement);
            String formatted = formatLimit(saved.getFeature(), saved.getLimitValue(), saved.getLimitUnit());
            responseList.add(new AdminPlanEntitlementDto(saved.getFeature(), saved.isEnabled(), item.limitMode(), saved.getLimitValue(), saved.getLimitUnit(), formatted));
        }

        auditEventService.record(admin.getId(), "ADMIN", null, null, AuditEventType.PLAN_UPDATED,
                "SubscriptionPlan", plan.getId(), "{\"action\":\"ENTITLEMENTS_UPDATED\"}");

        return responseList;
    }

    private User requireAdmin(Authentication authentication) {
        User user = userResolver.requireActiveUser(authentication);
        adminAuthorizationService.requireSystemAdmin(user.getId());
        return user;
    }

    private AdminSubscriptionPlanResponse toResponse(SubscriptionPlan plan) {
        long subscribers = workspaceSubscriptionRepository.countByPlanIdAndStatus(plan.getId(), WorkspaceSubscriptionStatus.ACTIVE);
        BigDecimal yearlyPrice = planPriceRepository
                .findByPlanIdAndBillingIntervalAndActiveTrue(plan.getId(), BillingInterval.YEARLY)
                .map(SubscriptionPlanPrice::getPrice)
                .orElse(null);
        return new AdminSubscriptionPlanResponse(
                plan.getId(),
                plan.getCode(),
                plan.getName(),
                plan.getDescription(),
                plan.getStatus(),
                plan.getBillingInterval(),
                plan.getPrice(),
                yearlyPrice,
                plan.getCurrency(),
                plan.isPubliclyAvailable(),
                plan.isFeatured(),
                plan.getDisplayOrder(),
                subscribers,
                plan.getCreatedAt(),
                plan.getUpdatedAt()
        );
    }

    private void savePlanPrice(SubscriptionPlan plan, BillingInterval interval, BigDecimal price, String currency) {
        if (price == null) return;
        SubscriptionPlanPrice planPrice = planPriceRepository
                .findByPlanIdAndBillingIntervalAndActiveTrue(plan.getId(), interval)
                .orElseGet(() -> {
                    SubscriptionPlanPrice p = new SubscriptionPlanPrice();
                    p.setPlan(plan);
                    p.setBillingInterval(interval);
                    return p;
                });
        planPrice.setPrice(price);
        planPrice.setCurrency(currency != null ? currency : plan.getCurrency());
        planPrice.setActive(true);
        planPriceRepository.save(planPrice);
    }

    private LimitUnit defaultUnitForFeature(PlanFeature feature) {
        return switch (feature) {
            case AI_GENERATION -> LimitUnit.REQUESTS;
            case AI_TOKENS -> LimitUnit.TOKENS;
            case STORAGE -> LimitUnit.BYTES;
            case PROJECT_CREATION -> LimitUnit.PROJECTS;
            case COLLABORATION, COLLABORATORS_PER_PROJECT -> LimitUnit.USERS;
            case DOCUMENT_UPLOAD, DOCUMENT_COUNT -> LimitUnit.DOCUMENTS;
            case REPORT_EXPORT, REPORT_EXPORT_DOCX, REPORT_EXPORT_PDF -> LimitUnit.EXPORTS;
            default -> LimitUnit.NONE;
        };
    }

    private String formatLimit(PlanFeature feature, Long limit, LimitUnit unit) {
        if (limit == null) return "Unlimited";
        if (unit == LimitUnit.BYTES) {
            long gb = limit / (1024L * 1024L * 1024L);
            if (gb > 0) return gb + " GB";
            long mb = limit / (1024L * 1024L);
            return mb + " MB";
        }
        return String.format("%,d %s", limit, unit.name().toLowerCase());
    }
}
