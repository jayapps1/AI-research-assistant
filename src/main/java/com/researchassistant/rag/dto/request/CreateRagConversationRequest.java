package com.researchassistant.rag.dto.request;

import jakarta.validation.constraints.Size;

public record CreateRagConversationRequest(
        @Size(max = 255) String title
) {
}
