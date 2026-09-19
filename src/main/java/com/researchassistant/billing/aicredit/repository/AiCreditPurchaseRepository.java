package com.researchassistant.billing.aicredit.repository;

import com.researchassistant.billing.aicredit.entity.AiCreditPurchase;
import com.researchassistant.billing.aicredit.entity.AiCreditPurchaseStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface AiCreditPurchaseRepository extends JpaRepository<AiCreditPurchase, UUID> {
    Page<AiCreditPurchase> findAllByWorkspaceIdOrderByCreatedAtDesc(UUID workspaceId, Pageable pageable);
    Optional<AiCreditPurchase> findByBillingPaymentIntentId(UUID paymentIntentId);
    long countByCreditPackId(UUID creditPackId);
}
