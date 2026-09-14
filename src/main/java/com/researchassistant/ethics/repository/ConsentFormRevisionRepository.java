package com.researchassistant.ethics.repository;

import com.researchassistant.ethics.model.ConsentFormRevision;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ConsentFormRevisionRepository extends JpaRepository<ConsentFormRevision, UUID> {
    Optional<ConsentFormRevision> findFirstByConsentFormIdOrderByRevisionNumberDesc(UUID consentFormId);
    long countByConsentFormId(UUID consentFormId);
}
