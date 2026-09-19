package com.researchassistant.billing;

import com.researchassistant.subscription.SubscriptionPlan;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;

@Component
@EnableConfigurationProperties(PaymentProperties.class)
public class PaystackClient {
    private final PaymentProperties properties;
    private final RestClient restClient;
    private final MoneyMinorUnitConverter moneyMinorUnitConverter;

    public PaystackClient(PaymentProperties properties, RestClient.Builder builder, MoneyMinorUnitConverter moneyMinorUnitConverter) {
        this.properties = properties;
        this.moneyMinorUnitConverter = moneyMinorUnitConverter;
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(properties.connectTimeout());
        requestFactory.setReadTimeout(properties.readTimeout());
        this.restClient = builder
                .baseUrl("https://api.paystack.co")
                .requestFactory(requestFactory)
                .build();
    }

    public PaystackInitializeResponse initialize(PaymentTransaction transaction, SubscriptionPlan plan) {
        requireConfigured();
        Map<String, Object> body = Map.of(
                "email", transaction.getInitiatedBy().getEmail(),
                "amount", toSmallestUnit(transaction.getAmount()),
                "currency", transaction.getCurrency(),
                "reference", transaction.getInternalReference(),
                "callback_url", callbackUrl(transaction.getInternalReference()),
                "metadata", Map.of(
                        "internalPaymentId", transaction.getId().toString(),
                        "workspaceId", transaction.getWorkspace().getId().toString(),
                        "planCode", plan.getCode(),
                        "billingInterval", transaction.getBillingInterval().name()
                )
        );
        return restClient.post()
                .uri("/transaction/initialize")
                .contentType(MediaType.APPLICATION_JSON)
                .headers(h -> h.setBearerAuth(properties.secretKey()))
                .body(body)
                .retrieve()
                .body(PaystackInitializeResponse.class);
    }

    public PaystackInitializeResponse initialize(PaymentAttempt attempt, SubscriptionPlan plan) {
        requireConfigured();
        BillingPaymentIntent intent = attempt.getPaymentIntent();
        java.util.Map<String, Object> metadata = new java.util.HashMap<>();
        metadata.put("internalPaymentIntentId", intent.getId().toString());
        metadata.put("internalPaymentAttemptId", attempt.getId().toString());
        metadata.put("workspaceId", intent.getWorkspace().getId().toString());
        metadata.put("purchaseType", intent.getPurchaseType() != null ? intent.getPurchaseType().name() : "SUBSCRIPTION");
        metadata.put("attemptNumber", attempt.getAttemptNumber());
        if (plan != null) {
            metadata.put("planCode", plan.getCode());
        }
        if (intent.getBillingInterval() != null) {
            metadata.put("billingInterval", intent.getBillingInterval().name());
        }
        if (intent.getAiCreditPurchase() != null) {
            metadata.put("packCode", intent.getAiCreditPurchase().getPackCode());
        }

        Map<String, Object> body = Map.of(
                "email", intent.getInitiatedBy().getEmail(),
                "amount", toSmallestUnit(attempt.getExpectedAmount()),
                "currency", attempt.getCurrency(),
                "reference", attempt.getInternalReference(),
                "callback_url", callbackUrl(attempt.getInternalReference()),
                "metadata", metadata
        );
        return restClient.post()
                .uri("/transaction/initialize")
                .contentType(MediaType.APPLICATION_JSON)
                .headers(h -> h.setBearerAuth(properties.secretKey()))
                .body(body)
                .retrieve()
                .body(PaystackInitializeResponse.class);
    }

    public PaystackVerificationResponse verify(String reference) {
        requireConfigured();
        return restClient.get()
                .uri("/transaction/verify/{reference}", reference)
                .headers(h -> h.setBearerAuth(properties.secretKey()))
                .retrieve()
                .body(PaystackVerificationResponse.class);
    }

    public long toSmallestUnit(BigDecimal amount) {
        return moneyMinorUnitConverter.toMinorUnits(amount, "GHS");
    }

    public PaymentEnvironment environment() {
        return properties.mode();
    }

    String secretKey() {
        return properties.secretKey();
    }

    private String callbackUrl(String reference) {
        String base = properties.callbackBaseUrl() == null ? "" : properties.callbackBaseUrl();
        return base.endsWith("/") ? base + "api/v1/billing/paystack/callback?reference=" + reference
                : base + "/api/v1/billing/paystack/callback?reference=" + reference;
    }

    private void requireConfigured() {
        if (!properties.enabled()) {
            throw new IllegalStateException("Paystack payments are disabled.");
        }
        if (properties.mode() == PaymentEnvironment.LIVE && !properties.liveEnabled()) {
            throw new IllegalStateException("Paystack LIVE mode is disabled for this development stage.");
        }
        if (properties.secretKey() == null || properties.secretKey().isBlank()) {
            throw new IllegalStateException("Paystack TEST secret key is not configured.");
        }
    }
}
