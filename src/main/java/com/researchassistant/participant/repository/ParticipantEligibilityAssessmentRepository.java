package com.researchassistant.participant.repository;

import com.researchassistant.participant.model.ParticipantEligibilityAssessment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ParticipantEligibilityAssessmentRepository extends JpaRepository<ParticipantEligibilityAssessment, UUID> {
    List<ParticipantEligibilityAssessment> findAllByParticipantIdOrderByAssessedAtDesc(UUID participantId);
}
