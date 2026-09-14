package com.researchassistant.integrity.repository;

import com.researchassistant.integrity.entity.SimilarityMatch;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface SimilarityMatchRepository extends JpaRepository<SimilarityMatch, UUID> {
    List<SimilarityMatch> findAllByCheckId(UUID checkId);
}
