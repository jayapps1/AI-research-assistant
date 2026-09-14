package com.researchassistant.participant.repository;

import com.researchassistant.participant.model.Participant;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ParticipantRepository extends JpaRepository<Participant, UUID> {
    Page<Participant> findAllByProjectId(UUID projectId, Pageable pageable);
    boolean existsByProjectIdAndParticipantCode(UUID projectId, String participantCode);
}
