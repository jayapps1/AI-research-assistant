package com.researchassistant.publicsite.dto;

import com.researchassistant.publicsite.entity.ContactResponse;
import com.researchassistant.publicsite.entity.ContactResponseChannel;
import com.researchassistant.publicsite.entity.ContactResponseStatus;

import java.time.OffsetDateTime;
import java.util.UUID;

public record AdminContactResponseDto(
        UUID id,
        String responseMessage,
        ContactResponseChannel channel,
        ContactResponseStatus status,
        String providerMessageId,
        UUID respondedById,
        String respondedByName,
        OffsetDateTime createdAt,
        OffsetDateTime sentAt
) {
    public static AdminContactResponseDto fromEntity(ContactResponse response) {
        String responderName = response.getRespondedBy() != null
                ? (response.getRespondedBy().getFirstName() != null
                    ? response.getRespondedBy().getFirstName() + " " + response.getRespondedBy().getLastName()
                    : response.getRespondedBy().getEmail())
                : "System";

        return new AdminContactResponseDto(
                response.getId(),
                response.getResponseMessage(),
                response.getChannel(),
                response.getStatus(),
                response.getProviderMessageId(),
                response.getRespondedBy() != null ? response.getRespondedBy().getId() : null,
                responderName,
                response.getCreatedAt(),
                response.getSentAt()
        );
    }
}
