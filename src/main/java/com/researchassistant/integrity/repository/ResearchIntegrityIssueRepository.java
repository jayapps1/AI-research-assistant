package com.researchassistant.integrity.repository;

import com.researchassistant.integrity.entity.ResearchIntegrityIssue;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface ResearchIntegrityIssueRepository extends JpaRepository<ResearchIntegrityIssue, UUID> {
    List<ResearchIntegrityIssue> findAllByReviewId(UUID reviewId);
}
