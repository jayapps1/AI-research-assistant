package com.researchassistant.researchdesign.repository;

import com.researchassistant.researchdesign.entity.ResearchObjective;
import com.researchassistant.researchdesign.entity.ResearchObjectiveType;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface ResearchObjectiveRepository extends JpaRepository<ResearchObjective, UUID> {
    List<ResearchObjective> findAllByProjectId(UUID projectId);
    boolean existsByProjectIdAndType(UUID projectId, ResearchObjectiveType type);
}
