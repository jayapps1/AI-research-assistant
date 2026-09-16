package com.researchassistant.publicsite.dto;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;
import java.util.UUID;

public record ReorderSectionsRequest(
        @NotEmpty(message = "Section IDs are required for reordering.")
        List<UUID> sectionIds
) {}
