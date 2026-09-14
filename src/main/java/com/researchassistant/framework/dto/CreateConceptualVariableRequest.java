package com.researchassistant.framework.dto;

import com.researchassistant.framework.entity.ConceptualVariableType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateConceptualVariableRequest(
        @NotBlank @Size(max = 255) String name,
        String description,
        ConceptualVariableType type,
        String operationalDefinition,
        @Min(1) Integer displayOrder
) {
}
