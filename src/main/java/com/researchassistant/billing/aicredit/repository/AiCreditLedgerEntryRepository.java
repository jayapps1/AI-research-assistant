package com.researchassistant.billing.aicredit.repository;

import com.researchassistant.billing.aicredit.entity.AiCreditLedgerEntry;
import com.researchassistant.billing.aicredit.entity.AiCreditLedgerType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface AiCreditLedgerEntryRepository extends JpaRepository<AiCreditLedgerEntry, UUID> {
    Page<AiCreditLedgerEntry> findAllByWorkspaceIdOrderByCreatedAtDesc(UUID workspaceId, Pageable pageable);
    Optional<AiCreditLedgerEntry> findByWorkspaceIdAndIdempotencyKey(UUID workspaceId, String idempotencyKey);
    boolean existsByWorkspaceIdAndIdempotencyKey(UUID workspaceId, String idempotencyKey);
    long countByPurchaseIdAndType(UUID purchaseId, AiCreditLedgerType type);
}
