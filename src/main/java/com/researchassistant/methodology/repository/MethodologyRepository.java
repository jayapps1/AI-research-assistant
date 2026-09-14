package com.researchassistant.methodology.repository;

import com.researchassistant.methodology.entity.Methodology;
import com.researchassistant.methodology.entity.MethodologyStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MethodologyRepository extends JpaRepository<Methodology, UUID> {
    List<Methodology> findAllByProjectIdOrderByRevisionNumberDesc(UUID projectId);
    Optional<Methodology> findByProjectIdAndStatus(UUID projectId, MethodologyStatus status);
}
