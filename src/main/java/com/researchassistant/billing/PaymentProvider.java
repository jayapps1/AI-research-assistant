package com.researchassistant.billing;

import com.researchassistant.subscription.SubscriptionPlan;

import java.math.BigDecimal;

public interface PaymentProvider {
    PaymentProviderType providerType();

    PaymentEnvironment environment();

    PaystackInitializeResponse initialize(PaymentTransaction transaction, SubscriptionPlan plan);

    PaystackInitializeResponse initialize(PaymentAttempt attempt, SubscriptionPlan plan);

    PaystackVerificationResponse verify(String reference);

    long toSmallestUnit(BigDecimal amount);
}
