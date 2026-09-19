package com.researchassistant.billing.aicredit.repository;

import com.researchassistant.billing.aicredit.entity.AiCreditRate;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AiCreditRateRepository extends JpaRepository<AiCreditRate, UUID> {
    Optional<AiCreditRate> findFirstByProviderIgnoreCaseAndModelIgnoreCaseAndActiveTrueAndEffectiveFromBeforeOrderByEffectiveFromDesc(
            String provider, String model, OffsetDateTime effectiveFrom
    );
    List<AiCreditRate> findAllByActiveTrueOrderByProviderAscModelAsc();
}
