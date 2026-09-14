package com.researchassistant.framework.dto;

import com.researchassistant.framework.entity.ConceptualRelationshipType;

import java.util.UUID;

public record ConceptualRelationshipResponse(
        UUID id,
        UUID frameworkId,
        UUID sourceVariableId,
        UUID targetVariableId,
        ConceptualRelationshipType type,
        String label,
        String rationale
) {
}
