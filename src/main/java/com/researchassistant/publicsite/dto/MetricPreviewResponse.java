package com.researchassistant.publicsite.dto;

import com.researchassistant.publicsite.entity.PublicSystemMetric;

import java.time.Instant;

public record MetricPreviewResponse(
        PublicSystemMetric metric,
        long rawValue,
        String formattedValue,
        Instant calculatedAt
) {
}
