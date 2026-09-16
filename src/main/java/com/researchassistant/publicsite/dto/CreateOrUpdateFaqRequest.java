package com.researchassistant.publicsite.dto;

import com.researchassistant.publicsite.entity.FaqCategory;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateOrUpdateFaqRequest(
        @NotBlank(message = "Question is required.")
        @Size(max = 500, message = "Question must not exceed 500 characters.")
        String question,

        @NotBlank(message = "Answer is required.")
        String answer,

        @NotNull(message = "Category is required.")
        FaqCategory category,

        Boolean featured,

        Boolean published,

        Integer displayOrder
) {}
