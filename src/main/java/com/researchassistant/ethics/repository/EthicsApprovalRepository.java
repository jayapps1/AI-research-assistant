package com.researchassistant.ethics.repository;

import com.researchassistant.ethics.model.EthicsApproval;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EthicsApprovalRepository extends JpaRepository<EthicsApproval, UUID> {
    List<EthicsApproval> findAllByProtocolIdOrderByCreatedAtDesc(UUID protocolId);
    Optional<EthicsApproval> findFirstByProtocolIdOrderByCreatedAtDesc(UUID protocolId);
}
