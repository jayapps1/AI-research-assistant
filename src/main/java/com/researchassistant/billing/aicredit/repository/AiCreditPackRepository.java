package com.researchassistant.billing.aicredit.repository;

import com.researchassistant.billing.aicredit.entity.AiCreditPack;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AiCreditPackRepository extends JpaRepository<AiCreditPack, UUID> {
    Optional<AiCreditPack> findByCodeIgnoreCase(String code);
    boolean existsByCodeIgnoreCase(String code);
    List<AiCreditPack> findAllByActiveTrueOrderByDisplayOrderAscCreatedAtAsc();
    List<AiCreditPack> findAllByOrderByDisplayOrderAscCreatedAtAsc();
}
