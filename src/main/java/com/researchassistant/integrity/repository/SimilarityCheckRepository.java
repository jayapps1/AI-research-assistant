package com.researchassistant.integrity.repository;

import com.researchassistant.integrity.entity.SimilarityCheck;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface SimilarityCheckRepository extends JpaRepository<SimilarityCheck, UUID> {}
