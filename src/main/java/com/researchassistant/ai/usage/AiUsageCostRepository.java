package com.researchassistant.ai.usage;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface AiUsageCostRepository extends JpaRepository<AiUsageCost, UUID> {

    Optional<AiUsageCost> findByRequestId(UUID requestId);

    @org.springframework.data.jpa.repository.Query("SELECT COALESCE(SUM(c.totalCost), 0) FROM AiUsageCost c WHERE c.currency = :currency")
    java.math.BigDecimal sumTotalCostByCurrency(@org.springframework.data.repository.query.Param("currency") String currency);
}
