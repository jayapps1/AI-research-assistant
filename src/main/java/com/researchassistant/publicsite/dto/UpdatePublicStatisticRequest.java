package com.researchassistant.publicsite.dto;

import com.researchassistant.publicsite.entity.PublicStatisticValueSource;
import com.researchassistant.publicsite.entity.PublicSystemMetric;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UpdatePublicStatisticRequest(
        @NotBlank
        @Size(max = 150)
        String label,

        @Size(max = 500)
        String description,

        @NotNull
        PublicStatisticValueSource valueSource,

        @Size(max = 100)
        String manualValue,

        PublicSystemMetric systemMetric,

        @Size(max = 20)
        String prefix,

        @Size(max = 20)
        String suffix,

        @Size(max = 60)
        String iconKey,

        Boolean enabled,
        Boolean featured,
        Integer displayOrder
) {
}
