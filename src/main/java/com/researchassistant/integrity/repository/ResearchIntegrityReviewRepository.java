package com.researchassistant.integrity.repository;

import com.researchassistant.integrity.entity.ResearchIntegrityReview;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface ResearchIntegrityReviewRepository extends JpaRepository<ResearchIntegrityReview, UUID> {}
