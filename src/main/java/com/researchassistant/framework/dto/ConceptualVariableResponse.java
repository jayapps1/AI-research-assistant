package com.researchassistant.framework.dto;

import com.researchassistant.framework.entity.ConceptualVariableType;

import java.util.UUID;

public record ConceptualVariableResponse(
        UUID id,
        UUID frameworkId,
        String name,
        String description,
        ConceptualVariableType type,
        String operationalDefinition,
        int displayOrder
) {
}
