package com.researchassistant.publicsite.dto;

import com.researchassistant.publicsite.entity.PublicStatisticValueSource;
import com.researchassistant.publicsite.entity.PublicSystemMetric;

import java.time.OffsetDateTime;
import java.util.UUID;

public record AdminPublicStatisticResponse(
        UUID id,
        String code,
        String label,
        String description,
        PublicStatisticValueSource valueSource,
        String manualValue,
        PublicSystemMetric systemMetric,
        String resolvedValue,
        String prefix,
        String suffix,
        String iconKey,
        boolean enabled,
        boolean featured,
        int displayOrder,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}
