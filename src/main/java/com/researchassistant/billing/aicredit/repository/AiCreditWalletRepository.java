package com.researchassistant.billing.aicredit.repository;

import com.researchassistant.billing.aicredit.entity.AiCreditWallet;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface AiCreditWalletRepository extends JpaRepository<AiCreditWallet, UUID> {
    Optional<AiCreditWallet> findByWorkspaceId(UUID workspaceId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select w from AiCreditWallet w where w.workspace.id = :workspaceId")
    Optional<AiCreditWallet> findByWorkspaceIdForUpdate(@Param("workspaceId") UUID workspaceId);
}
