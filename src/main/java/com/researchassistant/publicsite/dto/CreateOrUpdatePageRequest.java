package com.researchassistant.publicsite.dto;

import com.researchassistant.publicsite.entity.PublicPageStatus;
import com.researchassistant.publicsite.entity.PublicPageType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateOrUpdatePageRequest(
        @NotNull(message = "Page type is required.")
        PublicPageType type,

        @NotBlank(message = "Slug is required.")
        @Pattern(regexp = "^[a-z0-9-]+$", message = "Slug must contain only lowercase alphanumeric characters and hyphens.")
        @Size(max = 120, message = "Slug must not exceed 120 characters.")
        String slug,

        @NotBlank(message = "Title is required.")
        @Size(max = 200, message = "Title must not exceed 200 characters.")
        String title,

        @Size(max = 500)
        String subtitle,

        @Size(max = 255)
        String metaTitle,

        String metaDescription,

        PublicPageStatus status,

        Boolean showInNavigation,

        Integer navigationOrder
) {}
