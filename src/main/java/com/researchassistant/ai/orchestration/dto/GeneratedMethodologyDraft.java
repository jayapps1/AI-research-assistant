package com.researchassistant.ai.orchestration.dto;

import java.util.List;

public record GeneratedMethodologyDraft(
        String researchDesign,
        String targetPopulationDescription,
        String samplingTechnique,
        Integer recommendedSampleSize,
        List<String> dataCollectionMethods,
        String analyticalStrategy
) {
}
