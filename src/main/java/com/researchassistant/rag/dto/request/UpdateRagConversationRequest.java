package com.researchassistant.rag.dto.request;

import jakarta.validation.constraints.Size;

public record UpdateRagConversationRequest(
        @Size(max = 255) String title
) {
}
