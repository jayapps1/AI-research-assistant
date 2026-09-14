package com.researchassistant.framework.repository;

import com.researchassistant.framework.entity.ConceptualFramework;
import com.researchassistant.framework.entity.ConceptualFrameworkStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ConceptualFrameworkRepository extends JpaRepository<ConceptualFramework, UUID> {
    List<ConceptualFramework> findAllByProjectIdOrderByRevisionNumberDesc(UUID projectId);
    Optional<ConceptualFramework> findByProjectIdAndStatus(UUID projectId, ConceptualFrameworkStatus status);
}
