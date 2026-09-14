package com.researchassistant.framework.repository;

import com.researchassistant.framework.entity.ConceptualVariable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface ConceptualVariableRepository extends JpaRepository<ConceptualVariable, UUID> {
    List<ConceptualVariable> findAllByFrameworkIdOrderByDisplayOrderAsc(UUID frameworkId);
}
