package com.researchassistant.participant.dto;

import com.researchassistant.participant.model.Participant;
import jakarta.validation.constraints.Size;

import java.time.OffsetDateTime;
import java.util.UUID;

public final class ParticipantDtos {
    private ParticipantDtos() {}
    public record CreateParticipantRequest(
            UUID populationId,
            UUID samplingPlanId,
            Participant.Status status,
            IdentityRequest identity
    ) {}
    public record IdentityRequest(
            @Size(max = 500) String fullName,
            @Size(max = 500) String email,
            @Size(max = 100) String phone,
            @Size(max = 500) String externalIdentifier
    ) {}
    public record UpdateParticipantRequest(Participant.Status status, UUID populationId, UUID samplingPlanId) {}
    public record EligibilityRequest(boolean eligible, String reasonCode, String notes) {}
    public record WithdrawParticipantRequest(String reason) {}
    public record ParticipantResponse(
            UUID id,
            UUID projectId,
            String participantCode,
            Participant.Status status,
            UUID populationId,
            UUID samplingPlanId,
            OffsetDateTime enrolledAt,
            OffsetDateTime withdrawnAt
    ) {
        public static ParticipantResponse from(Participant participant) {
            return new ParticipantResponse(
                    participant.getId(),
                    participant.getProject().getId(),
                    participant.getParticipantCode(),
                    participant.getStatus(),
                    participant.getPopulation() == null ? null : participant.getPopulation().getId(),
                    participant.getSamplingPlan() == null ? null : participant.getSamplingPlan().getId(),
                    participant.getEnrolledAt(),
                    participant.getWithdrawnAt()
            );
        }
    }
}
