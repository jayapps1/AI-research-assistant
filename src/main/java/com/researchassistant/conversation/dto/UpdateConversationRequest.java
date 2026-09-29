package com.researchassistant.conversation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateConversationRequest(
        @NotBlank
        @Size(max = 255)
        String title
) {
}
