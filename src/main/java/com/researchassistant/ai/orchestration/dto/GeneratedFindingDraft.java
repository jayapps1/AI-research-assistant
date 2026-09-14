package com.researchassistant.ai.orchestration.dto;

import java.util.List;

public record GeneratedFindingDraft(
        String findingTitle,
        String summaryText,
        String statisticalInterpretation,
        List<String> keyInsights
) {
}
