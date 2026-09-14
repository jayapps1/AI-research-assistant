package com.researchassistant.researchdesign.repository;

import com.researchassistant.researchdesign.entity.ResearchHypothesis;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface ResearchHypothesisRepository extends JpaRepository<ResearchHypothesis, UUID> {
    List<ResearchHypothesis> findAllByProjectId(UUID projectId);
}
