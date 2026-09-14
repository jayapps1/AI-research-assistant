package com.researchassistant.researchdesign.repository;

import com.researchassistant.researchdesign.entity.ResearchProblem;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface ResearchProblemRepository extends JpaRepository<ResearchProblem, UUID> {
    List<ResearchProblem> findAllByProjectId(UUID projectId);
}
