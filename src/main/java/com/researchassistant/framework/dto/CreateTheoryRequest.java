package com.researchassistant.framework.dto;

import com.researchassistant.common.enums.ContentOrigin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateTheoryRequest(
        @NotBlank @Size(max = 255) String theoryName,
        @Size(max = 255) String theorist,
        Integer originalYear,
        String description,
        String keyConstructs,
        String relevanceToStudy,
        String limitations,
        @Min(1) Integer displayOrder,
        ContentOrigin origin
) {
}
