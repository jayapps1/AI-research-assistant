package com.researchassistant.billing;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.researchassistant.billing.dto.BillingDtos;
import com.researchassistant.billing.dto.BillingDtos.InitializePaymentRequest;
import com.researchassistant.billing.dto.BillingDtos.InitializePaymentResponse;
import com.researchassistant.billing.dto.BillingDtos.PaymentAttemptDetailResponse;
import com.researchassistant.billing.dto.BillingDtos.PaymentAttemptInitializationResponse;
import com.researchassistant.billing.dto.BillingDtos.PaymentIntentResponse;
import com.researchassistant.billing.dto.BillingDtos.PaymentTransactionResponse;
import com.researchassistant.identity.entity.User;
import com.researchassistant.identity.service.AuthenticatedUserResolver;
import com.researchassistant.subscription.WorkspaceSubscription;
import com.researchassistant.workspace.service.WorkspaceAuthorizationService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
public class BillingController {
    private final AuthenticatedUserResolver userResolver;
    private final WorkspaceAuthorizationService workspaceAuthorizationService;
    private final BillingService billingService;
    private final com.researchassistant.subscription.SubscriptionChangeService changeService;
    private final PaystackWebhookVerifier webhookVerifier;
    private final ObjectMapper objectMapper;
    private final com.researchassistant.subscription.ComplimentaryAccessGrantRepository complimentaryAccessGrantRepository;

    public BillingController(AuthenticatedUserResolver userResolver, WorkspaceAuthorizationService workspaceAuthorizationService,
                             BillingService billingService,
                             com.researchassistant.subscription.SubscriptionChangeService changeService,
                             PaystackWebhookVerifier webhookVerifier, ObjectMapper objectMapper,
                             com.researchassistant.subscription.ComplimentaryAccessGrantRepository complimentaryAccessGrantRepository) {
        this.userResolver = userResolver;
        this.workspaceAuthorizationService = workspaceAuthorizationService;
        this.billingService = billingService;
        this.changeService = changeService;
        this.webhookVerifier = webhookVerifier;
        this.objectMapper = objectMapper;
        this.complimentaryAccessGrantRepository = complimentaryAccessGrantRepository;
    }

    @PostMapping("/api/v1/workspaces/{workspaceId}/billing/initialize")
    public InitializePaymentResponse initialize(@PathVariable UUID workspaceId,
                                                @Valid @RequestBody InitializePaymentRequest request,
                                                @RequestHeader(name = "Idempotency-Key", required = false) String idempotencyKey,
                                                Authentication authentication) {
        User user = userResolver.requireActiveUser(authentication);
        workspaceAuthorizationService.requireAdminOrOwner(workspaceId, user);
        PaymentAttemptInitializationResponse response = billingService.initializeIntent(workspaceId, user, request.planCode(), request.billingInterval(), idempotencyKey);
        return new InitializePaymentResponse(response.paymentIntentId(), response.reference(), response.authorizationUrl(), null);
    }

    @PostMapping("/api/v1/billing/payment-intents/{paymentIntentId}/retry")
    public PaymentAttemptInitializationResponse retry(@PathVariable UUID paymentIntentId,
                                                     @RequestHeader(name = "Idempotency-Key", required = false) String idempotencyKey,
                                                     Authentication authentication) {
        User user = userResolver.requireActiveUser(authentication);
        PaymentIntentResponse intent = billingService.getIntent(paymentIntentId);
        workspaceAuthorizationService.requireAdminOrOwner(intent.workspaceId(), user);
        return billingService.retry(paymentIntentId, user, idempotencyKey);
    }

    @GetMapping("/api/v1/billing/payment-intents/{paymentIntentId}")
    public PaymentIntentResponse paymentIntent(@PathVariable UUID paymentIntentId, Authentication authentication) {
        User user = userResolver.requireActiveUser(authentication);
        PaymentIntentResponse response = billingService.getIntent(paymentIntentId);
        workspaceAuthorizationService.requireActiveMembership(response.workspaceId(), user);
        return response;
    }

    @PostMapping("/api/v1/billing/payment-attempts/{attemptId}/verify")
    public Map<String, Object> verifyAttempt(@PathVariable UUID attemptId, Authentication authentication) {
        User user = userResolver.requireActiveUser(authentication);
        PaymentAttempt existing = billingService.getAttempt(attemptId);
        workspaceAuthorizationService.requireAdminOrOwner(existing.getPaymentIntent().getWorkspace().getId(), user);
        PaymentAttempt attempt = billingService.verifyAttempt(attemptId, user.getId());
        workspaceAuthorizationService.requireAdminOrOwner(attempt.getPaymentIntent().getWorkspace().getId(), user);
        String planCode = attempt.getPaymentIntent() != null && attempt.getPaymentIntent().getPlan() != null
                ? attempt.getPaymentIntent().getPlan().getCode()
                : null;
        return Map.of(
                "paymentAttemptId", attempt.getId(),
                "paymentIntentId", attempt.getPaymentIntent().getId(),
                "status", attempt.getStatus().name(),
                "planCode", planCode != null ? planCode : ""
        );
    }

    @GetMapping("/api/v1/workspaces/{workspaceId}/billing/transactions")
    public Page<PaymentTransactionResponse> transactions(@PathVariable UUID workspaceId, Pageable pageable, Authentication authentication) {
        User user = userResolver.requireActiveUser(authentication);
        workspaceAuthorizationService.requireAdminOrOwner(workspaceId, user);
        return billingService.transactions(workspaceId, pageable).map(this::toResponse);
    }

    @GetMapping("/api/v1/workspaces/{workspaceId}/billing/subscription")
    public Map<String, Object> subscription(@PathVariable UUID workspaceId, Authentication authentication) {
        User user = userResolver.requireActiveUser(authentication);
        workspaceAuthorizationService.requireActiveMembership(workspaceId, user);
        WorkspaceSubscription subscription = billingService.subscription(workspaceId);
        com.researchassistant.subscription.SubscriptionPlan plan = subscription.getPlan();

        java.time.OffsetDateTime now = java.time.OffsetDateTime.now();
        java.util.List<com.researchassistant.subscription.ComplimentaryAccessGrant> grants =
                complimentaryAccessGrantRepository.findActiveWorkspacePlanGrants(workspaceId, now);
        boolean isComplimentary = !grants.isEmpty() || subscription.getAccessSource() == com.researchassistant.subscription.SubscriptionAccessSource.COMPLIMENTARY;
        String accessSource = !grants.isEmpty()
                ? grants.getFirst().getType().name()
                : (subscription.getAccessSource() != null ? subscription.getAccessSource().name() : "FREE_DEFAULT");
        if (!grants.isEmpty()) {
            plan = grants.getFirst().getPlan();
        }

        Map<String, Object> response = new java.util.LinkedHashMap<>();
        response.put("id", subscription.getId());
        response.put("planCode", plan.getCode());
        response.put("planName", plan.getName());
        response.put("price", plan.getPrice());
        response.put("currency", plan.getCurrency());
        response.put("status", subscription.getStatus().name());
        response.put("accessSource", accessSource);
        response.put("accessType", accessSource);
        response.put("isComplimentary", isComplimentary);
        response.put("periodStart", subscription.getCurrentPeriodStart());
        response.put("periodEnd", subscription.getCurrentPeriodEnd());
        response.put("autoRenew", subscription.isAutoRenew());
        return response;
    }

    @PostMapping("/api/v1/billing/transactions/{transactionId}/verify")
    public PaymentTransactionResponse verify(@PathVariable UUID transactionId, Authentication authentication) {
        User user = userResolver.requireActiveUser(authentication);
        PaymentTransaction transaction = billingService.verify(transactionId, user);
        workspaceAuthorizationService.requireAdminOrOwner(transaction.getWorkspace().getId(), user);
        return toResponse(transaction);
    }

    @GetMapping("/api/v1/billing/payment-attempts/by-reference/{reference}")
    public BillingDtos.PaymentAttemptDetailResponse getAttemptByReference(@PathVariable String reference, Authentication authentication) {
        User user = userResolver.requireActiveUser(authentication);
        BillingDtos.PaymentAttemptDetailResponse detail = billingService.getOrVerifyAttemptByReference(reference, user.getId());
        workspaceAuthorizationService.requireActiveMembership(detail.workspaceId(), user);
        return detail;
    }

    @GetMapping("/api/v1/billing/payment-attempts/{attemptId}")
    public BillingDtos.PaymentAttemptDetailResponse getAttemptDetail(@PathVariable UUID attemptId, Authentication authentication) {
        User user = userResolver.requireActiveUser(authentication);
        BillingDtos.PaymentAttemptDetailResponse detail = billingService.getAttemptDetail(attemptId);
        workspaceAuthorizationService.requireActiveMembership(detail.workspaceId(), user);
        return detail;
    }

    @GetMapping("/api/v1/billing/plans/{planCode}/breakdown")
    public BillingDtos.PlanPriceBreakdownResponse getPlanPriceBreakdown(
            @PathVariable String planCode,
            @RequestParam(defaultValue = "MONTHLY") com.researchassistant.subscription.BillingInterval interval) {
        return billingService.getPriceBreakdown(planCode, interval);
    }

    @GetMapping("/api/v1/billing/paystack/callback")
    public org.springframework.web.servlet.view.RedirectView callback(@RequestParam String reference) {
        try {
            billingService.verifyAttemptReference(reference, null);
        } catch (RuntimeException ignored) {
            try {
                billingService.verifyReference(reference, null);
            } catch (RuntimeException alsoIgnored) {
            }
        }
        return new org.springframework.web.servlet.view.RedirectView("/app/billing/payment-result?reference=" + java.net.URLEncoder.encode(reference, java.nio.charset.StandardCharsets.UTF_8));
    }

    @PostMapping("/api/v1/workspaces/{workspaceId}/billing/cancel")
    public Map<String, Object> cancel(@PathVariable UUID workspaceId, Authentication authentication) {
        User user = userResolver.requireActiveUser(authentication);
        workspaceAuthorizationService.requireAdminOrOwner(workspaceId, user);
        WorkspaceSubscription subscription = changeService.cancel(workspaceId, user.getId());
        return Map.of("id", subscription.getId(), "cancelAt", subscription.getCancelAt(), "autoRenew", subscription.isAutoRenew());
    }

    @PostMapping("/api/v1/billing/webhooks/paystack")
    public ResponseEntity<Map<String, Object>> paystackWebhook(@RequestBody byte[] rawBody,
                                                               @RequestHeader(name = "x-paystack-signature", required = false) String signature) throws Exception {
        if (!webhookVerifier.valid(rawBody, signature)) {
            return ResponseEntity.status(401).body(Map.of("accepted", false));
        }
        JsonNode root = objectMapper.readTree(rawBody);
        if ("charge.success".equals(root.path("event").asText())) {
            String reference = root.path("data").path("reference").asText(null);
            if (reference != null) {
                try {
                    billingService.verifyAttemptReference(reference, null);
                } catch (RuntimeException ignored) {
                    billingService.verifyReference(reference, null);
                }
            }
        }
        return ResponseEntity.ok(Map.of("accepted", true));
    }

    private PaymentTransactionResponse toResponse(PaymentTransaction transaction) {
        return new PaymentTransactionResponse(transaction.getId(), transaction.getWorkspace().getId(), transaction.getInternalReference(),
                transaction.getEnvironment(), transaction.getStatus(), transaction.getAmount(), transaction.getCurrency(),
                transaction.getPlanCode(), transaction.getBillingInterval(), transaction.getCreatedAt());
    }
}
