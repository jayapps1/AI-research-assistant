package com.researchassistant.billing;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PaymentAttemptRepository extends JpaRepository<PaymentAttempt, UUID> {
    List<PaymentAttempt> findAllByPaymentIntentIdOrderByAttemptNumberAsc(UUID paymentIntentId);
    Optional<PaymentAttempt> findFirstByPaymentIntentIdOrderByAttemptNumberDesc(UUID paymentIntentId);
    Optional<PaymentAttempt> findByInternalReference(String internalReference);
    Optional<PaymentAttempt> findByProviderAndEnvironmentAndProviderReference(PaymentProviderType provider, PaymentEnvironment environment, String providerReference);
    long countByPaymentIntentId(UUID paymentIntentId);
    long countByPaymentIntentIdAndStatus(UUID paymentIntentId, PaymentAttemptStatus status);
    long countByStatus(PaymentAttemptStatus status);
}
