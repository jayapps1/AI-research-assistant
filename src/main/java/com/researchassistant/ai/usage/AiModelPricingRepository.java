package com.researchassistant.ai.usage;

import com.researchassistant.ai.provider.AiProviderType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AiModelPricingRepository extends JpaRepository<AiModelPricing, UUID> {

    Optional<AiModelPricing> findFirstByProviderAndModelAndActiveIsTrueAndEffectiveFromBeforeOrderByEffectiveFromDesc(
            AiProviderType provider,
            String model,
            OffsetDateTime before
    );
}
