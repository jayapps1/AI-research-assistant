package com.researchassistant.participant.repository;

import com.researchassistant.participant.model.ParticipantIdentity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ParticipantIdentityRepository extends JpaRepository<ParticipantIdentity, UUID> {
    Optional<ParticipantIdentity> findByParticipantId(UUID participantId);
}
