package com.researchassistant.billing;

import com.researchassistant.subscription.SubscriptionPlan;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class PaystackPaymentProvider implements PaymentProvider {
    private final PaystackClient client;

    public PaystackPaymentProvider(PaystackClient client) {
        this.client = client;
    }

    @Override
    public PaymentProviderType providerType() {
        return PaymentProviderType.PAYSTACK;
    }

    @Override
    public PaymentEnvironment environment() {
        return client.environment();
    }

    @Override
    public PaystackInitializeResponse initialize(PaymentTransaction transaction, SubscriptionPlan plan) {
        return client.initialize(transaction, plan);
    }

    @Override
    public PaystackInitializeResponse initialize(PaymentAttempt attempt, SubscriptionPlan plan) {
        return client.initialize(attempt, plan);
    }

    @Override
    public PaystackVerificationResponse verify(String reference) {
        return client.verify(reference);
    }

    @Override
    public long toSmallestUnit(BigDecimal amount) {
        return client.toSmallestUnit(amount);
    }
}
