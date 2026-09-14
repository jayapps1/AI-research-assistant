package com.researchassistant.framework.dto;

import com.researchassistant.common.enums.ContentOrigin;

import java.util.UUID;

public record TheoryResponse(
        UUID id,
        UUID frameworkId,
        String theoryName,
        String theorist,
        Integer originalYear,
        String description,
        String keyConstructs,
        String relevanceToStudy,
        String limitations,
        int displayOrder,
        ContentOrigin origin
) {
}
