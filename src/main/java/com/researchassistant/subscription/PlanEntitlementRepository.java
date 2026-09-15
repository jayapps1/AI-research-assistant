package com.researchassistant.subscription;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PlanEntitlementRepository extends JpaRepository<PlanEntitlement, UUID> {
    List<PlanEntitlement> findAllByPlanId(UUID planId);
    Optional<PlanEntitlement> findByPlanIdAndFeature(UUID planId, PlanFeature feature);
}
