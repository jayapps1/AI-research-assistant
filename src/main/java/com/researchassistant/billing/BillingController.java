package com.researchassistant.billing;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.researchassistant.billing.dto.BillingDtos.InitializePaymentRequest;
import com.researchassistant.billing.dto.BillingDtos.InitializePaymentResponse;
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

    public BillingController(AuthenticatedUserResolver userResolver, WorkspaceAuthorizationService workspaceAuthorizationService,
                             BillingService billingService,
                             com.researchassistant.subscription.SubscriptionChangeService changeService,
                             PaystackWebhookVerifier webhookVerifier, ObjectMapper objectMapper) {
        this.userResolver = userResolver;
        this.workspaceAuthorizationService = workspaceAuthorizationService;
        this.billingService = billingService;
        this.changeService = changeService;
        this.webhookVerifier = webhookVerifier;
        this.objectMapper = objectMapper;
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
        return Map.of("paymentAttemptId", attempt.getId(), "paymentIntentId", attempt.getPaymentIntent().getId(), "status", attempt.getStatus());
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
        return Map.of("id", subscription.getId(), "planCode", subscription.getPlan().getCode(), "status", subscription.getStatus(),
                "periodStart", subscription.getCurrentPeriodStart(), "periodEnd", subscription.getCurrentPeriodEnd(), "autoRenew", subscription.isAutoRenew());
    }

    @PostMapping("/api/v1/billing/transactions/{transactionId}/verify")
    public PaymentTransactionResponse verify(@PathVariable UUID transactionId, Authentication authentication) {
        User user = userResolver.requireActiveUser(authentication);
        PaymentTransaction transaction = billingService.verify(transactionId, user);
        workspaceAuthorizationService.requireAdminOrOwner(transaction.getWorkspace().getId(), user);
        return toResponse(transaction);
    }

    @GetMapping("/api/v1/billing/paystack/callback")
    public Map<String, Object> callback(@RequestParam String reference) {
        try {
            PaymentAttempt attempt = billingService.verifyAttemptReference(reference, null);
            return Map.of("paymentIntentId", attempt.getPaymentIntent().getId(), "paymentAttemptId", attempt.getId(), "status", attempt.getStatus());
        } catch (RuntimeException ignored) {
            PaymentTransaction transaction = billingService.verifyReference(reference, null);
            return Map.of("transactionId", transaction.getId(), "status", transaction.getStatus());
        }
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
