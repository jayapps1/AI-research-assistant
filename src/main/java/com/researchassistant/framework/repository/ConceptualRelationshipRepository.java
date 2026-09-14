package com.researchassistant.framework.repository;

import com.researchassistant.framework.entity.ConceptualRelationship;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface ConceptualRelationshipRepository extends JpaRepository<ConceptualRelationship, UUID> {
    List<ConceptualRelationship> findAllByFrameworkId(UUID frameworkId);
}
