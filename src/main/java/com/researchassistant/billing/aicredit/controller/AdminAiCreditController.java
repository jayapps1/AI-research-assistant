package com.researchassistant.billing.aicredit.controller;

import com.researchassistant.admin.SystemAdminAuthorizationService;
import com.researchassistant.audit.AuditEventService;
import com.researchassistant.audit.AuditEventType;
import com.researchassistant.billing.aicredit.dto.*;
import com.researchassistant.billing.aicredit.entity.AiCreditBucket;
import com.researchassistant.billing.aicredit.entity.AiCreditPack;
import com.researchassistant.billing.aicredit.entity.AiCreditWallet;
import com.researchassistant.billing.aicredit.repository.AiCreditPackRepository;
import com.researchassistant.billing.aicredit.repository.AiCreditPurchaseRepository;
import com.researchassistant.billing.aicredit.repository.AiCreditWalletRepository;
import com.researchassistant.billing.aicredit.service.AiCreditService;
import com.researchassistant.common.exception.DuplicateResourceException;
import com.researchassistant.common.exception.ResourceNotFoundException;
import com.researchassistant.identity.entity.User;
import com.researchassistant.identity.service.AuthenticatedUserResolver;
import com.researchassistant.workspace.entity.Workspace;
import com.researchassistant.workspace.repository.WorkspaceRepository;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin")
public class AdminAiCreditController {

    private final AuthenticatedUserResolver userResolver;
    private final SystemAdminAuthorizationService adminAuthorizationService;
    private final AiCreditPackRepository packRepository;
    private final AiCreditPurchaseRepository purchaseRepository;
    private final AiCreditWalletRepository walletRepository;
    private final WorkspaceRepository workspaceRepository;
    private final AiCreditService creditService;
    private final AuditEventService auditEventService;

    public AdminAiCreditController(
            AuthenticatedUserResolver userResolver,
            SystemAdminAuthorizationService adminAuthorizationService,
            AiCreditPackRepository packRepository,
            AiCreditPurchaseRepository purchaseRepository,
            AiCreditWalletRepository walletRepository,
            WorkspaceRepository workspaceRepository,
            AiCreditService creditService,
            AuditEventService auditEventService
    ) {
        this.userResolver = userResolver;
        this.adminAuthorizationService = adminAuthorizationService;
        this.packRepository = packRepository;
        this.purchaseRepository = purchaseRepository;
        this.walletRepository = walletRepository;
        this.workspaceRepository = workspaceRepository;
        this.creditService = creditService;
        this.auditEventService = auditEventService;
    }

    @GetMapping("/billing/ai-credit-packs")
    public List<AiCreditPackResponse> listPacks(Authentication authentication) {
        User user = userResolver.requireActiveUser(authentication);
        adminAuthorizationService.requireSystemAdmin(user.getId());
        return packRepository.findAllByOrderByDisplayOrderAscCreatedAtAsc()
                .stream()
                .map(this::toPackResponse)
                .toList();
    }

    @PostMapping("/billing/ai-credit-packs")
    @Transactional
    public AiCreditPackResponse createPack(
            @Valid @RequestBody CreateAiCreditPackRequest request,
            Authentication authentication
    ) {
        User user = userResolver.requireActiveUser(authentication);
        adminAuthorizationService.requireSystemAdmin(user.getId());

        if (packRepository.existsByCodeIgnoreCase(request.code())) {
            throw new DuplicateResourceException("An AI credit pack with code '" + request.code() + "' already exists.");
        }

        AiCreditPack pack = new AiCreditPack();
        pack.setCode(request.code().trim().toUpperCase());
        pack.setName(request.name().trim());
        pack.setDescription(request.description());
        pack.setCreditAmount(request.creditAmount());
        pack.setPriceAmount(request.priceAmount());
        pack.setCurrency(request.currency() != null && !request.currency().isBlank() ? request.currency().toUpperCase() : "GHS");
        pack.setActive(request.active() != null ? request.active() : true);
        pack.setDisplayOrder(request.displayOrder() != null ? request.displayOrder() : 0);

        pack = packRepository.save(pack);

        auditEventService.record(user.getId(), "ADMIN", null, null, AuditEventType.AI_CREDIT_PACK_CREATED,
                "AiCreditPack", pack.getId(),
                "{\"code\":\"" + pack.getCode() + "\",\"credits\":" + pack.getCreditAmount() + ",\"price\":" + pack.getPriceAmount() + "}");

        return toPackResponse(pack);
    }

    @PatchMapping("/billing/ai-credit-packs/{packId}")
    @Transactional
    public AiCreditPackResponse updatePack(
            @PathVariable UUID packId,
            @Valid @RequestBody UpdateAiCreditPackRequest request,
            Authentication authentication
    ) {
        User user = userResolver.requireActiveUser(authentication);
        adminAuthorizationService.requireSystemAdmin(user.getId());

        AiCreditPack pack = packRepository.findById(packId)
                .orElseThrow(() -> new ResourceNotFoundException("AI credit pack not found: " + packId));

        if (request.name() != null && !request.name().isBlank()) {
            pack.setName(request.name().trim());
        }
        if (request.description() != null) {
            pack.setDescription(request.description());
        }
        if (request.creditAmount() != null) {
            if (request.creditAmount().compareTo(java.math.BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException("Credit amount must be greater than zero.");
            }
            pack.setCreditAmount(request.creditAmount());
        }
        if (request.priceAmount() != null) {
            if (request.priceAmount().compareTo(java.math.BigDecimal.ZERO) < 0) {
                throw new IllegalArgumentException("Price amount must be greater than or equal to zero.");
            }
            pack.setPriceAmount(request.priceAmount());
        }
        if (request.currency() != null && !request.currency().isBlank()) {
            pack.setCurrency(request.currency().toUpperCase());
        }
        if (request.active() != null) {
            pack.setActive(request.active());
        }
        if (request.displayOrder() != null) {
            pack.setDisplayOrder(request.displayOrder());
        }

        pack = packRepository.save(pack);

        auditEventService.record(user.getId(), "ADMIN", null, null,
                pack.isActive() ? AuditEventType.AI_CREDIT_PACK_UPDATED : AuditEventType.AI_CREDIT_PACK_DEACTIVATED,
                "AiCreditPack", pack.getId(), "{}");

        return toPackResponse(pack);
    }

    @PostMapping("/workspaces/{workspaceId}/ai-credits/grant")
    public AiCreditBalanceResponse grantCredits(
            @PathVariable UUID workspaceId,
            @Valid @RequestBody AdminGrantAiCreditsRequest request,
            Authentication authentication
    ) {
        User user = userResolver.requireActiveUser(authentication);
        adminAuthorizationService.requireSystemAdmin(user.getId());

        AiCreditBucket bucket;
        try {
            bucket = AiCreditBucket.valueOf(request.bucket().toUpperCase());
        } catch (Exception e) {
            bucket = AiCreditBucket.PURCHASED;
        }

        creditService.adminGrant(workspaceId, request.creditAmount(), bucket, request.reason(), user);
        return creditService.getBalance(workspaceId);
    }

    @GetMapping("/workspaces/{workspaceId}/ai-credits")
    public AiCreditBalanceResponse getWorkspaceCredits(
            @PathVariable UUID workspaceId,
            Authentication authentication
    ) {
        User user = userResolver.requireActiveUser(authentication);
        adminAuthorizationService.requireSystemAdmin(user.getId());
        return creditService.getBalance(workspaceId);
    }

    private AiCreditPackResponse toPackResponse(AiCreditPack pack) {
        return new AiCreditPackResponse(
                pack.getId(),
                pack.getCode(),
                pack.getName(),
                pack.getDescription(),
                pack.getCreditAmount(),
                pack.getPriceAmount(),
                pack.getCurrency(),
                pack.isActive(),
                pack.getDisplayOrder(),
                pack.getCreatedAt()
        );
    }
}
