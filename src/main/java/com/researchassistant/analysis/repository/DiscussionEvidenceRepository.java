package com.researchassistant.analysis.repository;

import com.researchassistant.analysis.entity.DiscussionEvidence;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface DiscussionEvidenceRepository extends JpaRepository<DiscussionEvidence, UUID> {
    List<DiscussionEvidence> findAllByDiscussionIdOrderByCitationOrdinalAsc(UUID discussionId);
}
