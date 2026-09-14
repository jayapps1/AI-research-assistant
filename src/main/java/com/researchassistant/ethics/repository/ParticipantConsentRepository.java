package com.researchassistant.ethics.repository;

import com.researchassistant.ethics.model.ParticipantConsent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ParticipantConsentRepository extends JpaRepository<ParticipantConsent, UUID> {
    List<ParticipantConsent> findAllByParticipantIdOrderByConsentedAtDesc(UUID participantId);
    Optional<ParticipantConsent> findFirstByParticipantIdOrderByConsentedAtDesc(UUID participantId);
}
