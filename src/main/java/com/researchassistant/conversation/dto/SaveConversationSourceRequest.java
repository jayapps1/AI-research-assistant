package com.researchassistant.conversation.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record SaveConversationSourceRequest(
        @NotNull UUID projectId
) {
}
