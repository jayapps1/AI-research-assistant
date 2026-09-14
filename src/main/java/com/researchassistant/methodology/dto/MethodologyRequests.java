package com.researchassistant.methodology.dto;

import com.researchassistant.common.enums.ContentOrigin;
import com.researchassistant.methodology.entity.*;
import jakarta.validation.constraints.*;

import java.util.List;
import java.util.UUID;

public final class MethodologyRequests {
    private MethodologyRequests() {}

    public record CreateMethodologyRequest(
            UUID researchProblemId,
            @Size(max = 255) String title,
            ResearchApproach approach,
            ResearchDesignType designType,
            String designDescription,
            String studySetting,
            @Size(max = 255) String studyPeriod,
            String rationale,
            ContentOrigin origin,
            List<UUID> objectiveIds,
            List<UUID> questionIds
    ) {}

    public record CreatePopulationRequest(
            @NotBlank String targetPopulationDescription,
            @Min(1) Long targetPopulationSize,
            String accessiblePopulationDescription,
            @Min(1) Long accessiblePopulationSize,
            String inclusionCriteria,
            String exclusionCriteria,
            String geographicScope,
            String demographicCharacteristics,
            ContentOrigin origin
    ) {}

    public record CreateSamplingPlanRequest(
            UUID populationId,
            SamplingApproach approach,
            SamplingTechnique technique,
            @Min(1) Integer plannedSampleSize,
            String rationale,
            String samplingFrame,
            String recruitmentStrategy,
            ContentOrigin origin
    ) {}

    public record CalculateSampleSizeRequest(
            @NotNull SampleSizeMethod method,
            @Min(1) Long populationSize,
            @DecimalMin(value = "0.0", inclusive = false) @DecimalMax(value = "1.0") Double confidenceLevel,
            @DecimalMin(value = "0.0", inclusive = false) Double marginOfError,
            @DecimalMin(value = "0.0", inclusive = false) @DecimalMax(value = "1.0") Double estimatedProportion,
            @DecimalMin(value = "0.0", inclusive = false) Double designEffect,
            @DecimalMin(value = "0.0", inclusive = false) @DecimalMax(value = "1.0") Double expectedResponseRate,
            @Min(1) Integer manualSampleSize,
            String qualitativeJustification
    ) {}

    public record CreateDataCollectionMethodRequest(
            DataCollectionMethodType type,
            @NotBlank @Size(max = 255) String name,
            @NotBlank String description,
            String rationale,
            DataSourceType sourceType,
            @Size(max = 255) String administrationMode,
            String setting,
            @Size(max = 255) String timing,
            boolean primaryMethod,
            @Min(1) Integer displayOrder,
            ContentOrigin origin,
            List<UUID> objectiveIds,
            List<UUID> questionIds,
            UUID populationId,
            UUID samplingPlanId
    ) {}
}
