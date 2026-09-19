package com.researchassistant.billing.aicredit.controller;

import com.researchassistant.billing.aicredit.dto.*;
import com.researchassistant.billing.aicredit.entity.AiCreditPack;
import com.researchassistant.billing.aicredit.repository.AiCreditPackRepository;
import com.researchassistant.billing.aicredit.service.AiCreditPurchaseService;
import com.researchassistant.billing.aicredit.service.AiCreditService;
import com.researchassistant.billing.dto.BillingDtos.PaymentAttemptInitializationResponse;
import com.researchassistant.identity.entity.User;
import com.researchassistant.identity.service.AuthenticatedUserResolver;
import com.researchassistant.workspace.service.WorkspaceAuthorizationService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/workspaces/{workspaceId}")
public class WorkspaceAiCreditController {

    private final AuthenticatedUserResolver userResolver;
    private final WorkspaceAuthorizationService workspaceAuthorizationService;
    private final AiCreditService creditService;
    private final AiCreditPurchaseService purchaseService;
    private final AiCreditPackRepository packRepository;

    public WorkspaceAiCreditController(
            AuthenticatedUserResolver userResolver,
            WorkspaceAuthorizationService workspaceAuthorizationService,
            AiCreditService creditService,
            AiCreditPurchaseService purchaseService,
            AiCreditPackRepository packRepository
    ) {
        this.userResolver = userResolver;
        this.workspaceAuthorizationService = workspaceAuthorizationService;
        this.creditService = creditService;
        this.purchaseService = purchaseService;
        this.packRepository = packRepository;
    }

    @GetMapping("/ai-credits")
    public AiCreditBalanceResponse getBalance(
            @PathVariable UUID workspaceId,
            Authentication authentication
    ) {
        User user = userResolver.requireActiveUser(authentication);
        workspaceAuthorizationService.requireActiveMembership(workspaceId, user);
        return creditService.getBalance(workspaceId);
    }

    @GetMapping("/billing/ai-credit-packs")
    public List<AiCreditPackResponse> getActivePacks(
            @PathVariable UUID workspaceId,
            Authentication authentication
    ) {
        User user = userResolver.requireActiveUser(authentication);
        workspaceAuthorizationService.requireActiveMembership(workspaceId, user);
        return packRepository.findAllByActiveTrueOrderByDisplayOrderAscCreatedAtAsc()
                .stream()
                .map(this::toPackResponse)
                .toList();
    }

    @PostMapping("/billing/ai-credits/purchase")
    public PaymentAttemptInitializationResponse purchasePack(
            @PathVariable UUID workspaceId,
            @Valid @RequestBody BuyAiCreditPackRequest request,
            Authentication authentication
    ) {
        User user = userResolver.requireActiveUser(authentication);
        workspaceAuthorizationService.requireAdminOrOwner(workspaceId, user);
        return purchaseService.initializePurchase(workspaceId, user, request);
    }

    @GetMapping("/ai-credits/ledger")
    public Page<AiCreditLedgerItemResponse> getLedger(
            @PathVariable UUID workspaceId,
            Pageable pageable,
            Authentication authentication
    ) {
        User user = userResolver.requireActiveUser(authentication);
        workspaceAuthorizationService.requireActiveMembership(workspaceId, user);
        return creditService.listLedger(workspaceId, pageable);
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
