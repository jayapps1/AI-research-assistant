package com.researchassistant.methodology.dto;

import com.researchassistant.common.enums.ContentOrigin;
import com.researchassistant.methodology.entity.*;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public final class MethodologyResponses {
    private MethodologyResponses() {}

    public record MethodologyResponse(
            UUID id,
            UUID projectId,
            UUID researchProblemId,
            String title,
            ResearchApproach approach,
            ResearchDesignType designType,
            String designDescription,
            String studySetting,
            String studyPeriod,
            String rationale,
            MethodologyStatus status,
            ContentOrigin origin,
            int revisionNumber,
            OffsetDateTime createdAt,
            OffsetDateTime updatedAt
    ) {}

    public record PopulationResponse(
            UUID id,
            UUID methodologyId,
            String targetPopulationDescription,
            Long targetPopulationSize,
            String accessiblePopulationDescription,
            Long accessiblePopulationSize,
            String inclusionCriteria,
            String exclusionCriteria,
            String geographicScope,
            String demographicCharacteristics,
            ContentOrigin origin
    ) {}

    public record SamplingPlanResponse(
            UUID id,
            UUID methodologyId,
            UUID populationId,
            SamplingApproach approach,
            SamplingTechnique technique,
            Integer plannedSampleSize,
            String rationale,
            String samplingFrame,
            String recruitmentStrategy,
            ContentOrigin origin
    ) {}

    public record SampleSizeCalculationResponse(
            UUID id,
            SampleSizeMethod method,
            Long populationSize,
            Double confidenceLevel,
            Double marginOfError,
            Double estimatedProportion,
            Double designEffect,
            Double expectedResponseRate,
            Integer initialSampleSize,
            Integer adjustedSampleSize,
            String formulaDescription,
            String assumptions
    ) {}

    public record DataCollectionMethodResponse(
            UUID id,
            UUID methodologyId,
            DataCollectionMethodType type,
            String name,
            String description,
            String rationale,
            DataSourceType sourceType,
            String administrationMode,
            String setting,
            String timing,
            boolean primaryMethod,
            int displayOrder,
            ContentOrigin origin
    ) {}

    public record ResearchDesignValidationResult(
            boolean validForExecution,
            List<String> errors,
            List<String> warnings,
            List<String> information
    ) {
        public ResearchDesignValidationResult {
            errors = errors == null ? List.of() : List.copyOf(errors);
            warnings = warnings == null ? List.of() : List.copyOf(warnings);
            information = information == null ? List.of() : List.copyOf(information);
        }
    }
}
