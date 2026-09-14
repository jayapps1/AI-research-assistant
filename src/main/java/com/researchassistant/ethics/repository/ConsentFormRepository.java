package com.researchassistant.ethics.repository;

import com.researchassistant.ethics.model.ConsentForm;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ConsentFormRepository extends JpaRepository<ConsentForm, UUID> {
    List<ConsentForm> findAllByProjectIdOrderByCreatedAtDesc(UUID projectId);
    Optional<ConsentForm> findFirstByProjectIdAndLanguageCodeAndStatus(UUID projectId, String languageCode, ConsentForm.Status status);
    boolean existsByProjectIdAndStatus(UUID projectId, ConsentForm.Status status);
}
