package com.researchassistant.framework.dto;

import com.researchassistant.framework.entity.ConceptualRelationshipType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record CreateConceptualRelationshipRequest(
        @NotNull UUID sourceVariableId,
        @NotNull UUID targetVariableId,
        ConceptualRelationshipType type,
        @Size(max = 255) String label,
        String rationale
) {
}
