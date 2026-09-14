package com.researchassistant.datacollection.dto;

import com.researchassistant.datacollection.model.DataCollectionSession;
import com.researchassistant.datacollection.model.InstrumentResponse;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public final class DataCollectionDtos {
    private DataCollectionDtos() {}
    public record CreateSessionRequest(@NotNull UUID instrumentId, UUID participantId,
            DataCollectionSession.Type type, OffsetDateTime scheduledAt, String locationOrSetting, String notes) {}
    public record ResponseOptionRequest(String value, String label) {}
    public record RecordResponseRequest(@NotNull InstrumentResponse.ItemType itemType, @NotNull UUID itemId,
            @NotNull InstrumentResponse.ResponseType type, String textValue, BigDecimal numericValue,
            Boolean booleanValue, LocalDate dateValue, String optionValue, String rawValue,
            List<ResponseOptionRequest> options) {}
    public record SessionResponse(UUID id, UUID projectId, UUID instrumentId, int instrumentRevisionNumber,
            UUID participantId, DataCollectionSession.Type type, DataCollectionSession.Status status,
            String sessionCode, OffsetDateTime startedAt, OffsetDateTime completedAt) {
        public static SessionResponse from(DataCollectionSession s) {
            return new SessionResponse(s.getId(), s.getProject().getId(), s.getInstrument().getId(),
                    s.getInstrumentRevisionNumber(), s.getParticipant() == null ? null : s.getParticipant().getId(),
                    s.getType(), s.getStatus(), s.getSessionCode(), s.getStartedAt(), s.getCompletedAt());
        }
    }
}
