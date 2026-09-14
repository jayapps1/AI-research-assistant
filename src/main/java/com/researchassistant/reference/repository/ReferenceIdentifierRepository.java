package com.researchassistant.reference.repository;

import com.researchassistant.reference.entity.ReferenceIdentifier;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ReferenceIdentifierRepository extends JpaRepository<ReferenceIdentifier, UUID> {
    List<ReferenceIdentifier> findAllByReferenceId(UUID referenceId);
}
