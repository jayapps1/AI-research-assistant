package com.researchassistant.collaboration.repository;

import com.researchassistant.collaboration.entity.AiArtifactProposal;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface AiArtifactProposalRepository extends JpaRepository<AiArtifactProposal, UUID> {
    Optional<AiArtifactProposal> findByIdAndProjectId(UUID id, UUID projectId);
}
