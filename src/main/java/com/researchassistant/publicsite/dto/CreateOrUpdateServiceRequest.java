package com.researchassistant.publicsite.dto;

import com.researchassistant.publicsite.entity.ServiceOfferingStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateOrUpdateServiceRequest(
        @NotBlank(message = "Service code is required.")
        @Size(max = 80, message = "Code must not exceed 80 characters.")
        String code,

        @NotBlank(message = "Service name is required.")
        @Size(max = 200, message = "Name must not exceed 200 characters.")
        String name,

        @NotBlank(message = "Short description is required.")
        @Size(max = 500, message = "Short description must not exceed 500 characters.")
        String shortDescription,

        String description,

        @Size(max = 100)
        String iconKey,

        String featureListJson,

        @Size(max = 100)
        String ctaLabel,

        @Size(max = 500)
        String ctaUrl,

        Boolean featured,

        ServiceOfferingStatus status,

        Integer displayOrder
) {}
