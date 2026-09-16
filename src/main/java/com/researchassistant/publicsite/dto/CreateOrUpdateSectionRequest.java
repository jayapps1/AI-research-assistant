package com.researchassistant.publicsite.dto;

import com.researchassistant.publicsite.entity.PublicSectionType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateOrUpdateSectionRequest(
        @NotNull(message = "Section type is required.")
        PublicSectionType type,

        @NotBlank(message = "Section key is required.")
        @Size(max = 100, message = "Section key must not exceed 100 characters.")
        String sectionKey,

        @Size(max = 200)
        String eyebrow,

        @Size(max = 300)
        String heading,

        @Size(max = 500)
        String subheading,

        String body,

        @Size(max = 255)
        String imageStorageKey,

        @Size(max = 100)
        String primaryCtaLabel,

        @Size(max = 500)
        String primaryCtaUrl,

        @Size(max = 100)
        String secondaryCtaLabel,

        @Size(max = 500)
        String secondaryCtaUrl,

        String configurationJson,

        Boolean enabled,

        Integer displayOrder
) {}
