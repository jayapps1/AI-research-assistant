package com.researchassistant.ethics.repository;

import com.researchassistant.ethics.model.EthicsProtocol;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EthicsProtocolRepository extends JpaRepository<EthicsProtocol, UUID> {
    List<EthicsProtocol> findAllByProjectIdOrderByCreatedAtDesc(UUID projectId);
    Optional<EthicsProtocol> findFirstByProjectIdOrderByCreatedAtDesc(UUID projectId);
}
