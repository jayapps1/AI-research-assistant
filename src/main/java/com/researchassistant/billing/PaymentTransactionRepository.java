package com.researchassistant.billing;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface PaymentTransactionRepository extends JpaRepository<PaymentTransaction, UUID> {
    Page<PaymentTransaction> findAllByWorkspaceIdOrderByCreatedAtDesc(UUID workspaceId, Pageable pageable);
    Optional<PaymentTransaction> findByProviderAndEnvironmentAndProviderReference(PaymentProviderType provider, PaymentEnvironment environment, String providerReference);
    Optional<PaymentTransaction> findByInternalReference(String internalReference);
}
