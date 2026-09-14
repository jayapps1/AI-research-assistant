package com.researchassistant.researchdesign.repository;

import com.researchassistant.researchdesign.entity.ResearchQuestion;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface ResearchQuestionRepository extends JpaRepository<ResearchQuestion, UUID> {
    List<ResearchQuestion> findAllByProjectId(UUID projectId);
    long countByProjectId(UUID projectId);
}
