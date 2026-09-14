package com.researchassistant.retrieval.dto;

import jakarta.validation.constraints.NotBlank;

import java.util.Set;
import java.util.UUID;

public record RetrievalSearchRequest(
        @NotBlank String query,
        Set<UUID> documentIds,
        Integer limit
) {
}
