package com.researchassistant.framework.dto;

import com.researchassistant.common.enums.ContentOrigin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateTheoreticalFrameworkRequest(
        @NotBlank @Size(max = 255) String title,
        @NotBlank String overview,
        ContentOrigin origin
) {
}
