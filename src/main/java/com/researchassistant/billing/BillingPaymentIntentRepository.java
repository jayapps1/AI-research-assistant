package com.researchassistant.billing;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.UUID;

public interface BillingPaymentIntentRepository extends JpaRepository<BillingPaymentIntent, UUID> {
    Page<BillingPaymentIntent> findAllByWorkspaceIdOrderByCreatedAtDesc(UUID workspaceId, Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select i from BillingPaymentIntent i where i.id = :id")
    Optional<BillingPaymentIntent> findByIdForUpdate(@Param("id") UUID id);
}
