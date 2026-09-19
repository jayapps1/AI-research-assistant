package com.researchassistant.subscription;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SubscriptionPlanPriceRepository extends JpaRepository<SubscriptionPlanPrice, UUID> {

    Optional<SubscriptionPlanPrice> findByPlanIdAndBillingIntervalAndActiveTrue(UUID planId, BillingInterval billingInterval);

    List<SubscriptionPlanPrice> findAllByPlanIdAndActiveTrue(UUID planId);

    @Query("""
            SELECT p FROM SubscriptionPlanPrice p
            JOIN p.plan pl
            WHERE UPPER(pl.code) = UPPER(:planCode)
              AND p.active = TRUE
            """)
    List<SubscriptionPlanPrice> findAllByPlanCodeIgnoreCaseAndActiveTrue(@Param("planCode") String planCode);

    @Query("""
            SELECT p FROM SubscriptionPlanPrice p
            JOIN p.plan pl
            WHERE UPPER(pl.code) = UPPER(:planCode)
              AND p.billingInterval = :billingInterval
              AND p.active = TRUE
            """)
    Optional<SubscriptionPlanPrice> findByPlanCodeIgnoreCaseAndBillingIntervalAndActiveTrue(
            @Param("planCode") String planCode,
            @Param("billingInterval") BillingInterval billingInterval
    );

    List<SubscriptionPlanPrice> findAllByActiveTrue();
}
