package com.researchassistant.framework.repository;

import com.researchassistant.framework.entity.TheoreticalFrameworkEvidence;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface TheoreticalFrameworkEvidenceRepository extends JpaRepository<TheoreticalFrameworkEvidence, UUID> {
    List<TheoreticalFrameworkEvidence> findAllByTheoryId(UUID theoryId);
}
