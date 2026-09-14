package com.researchassistant.instruments.dto;

import com.researchassistant.common.enums.ContentOrigin;
import com.researchassistant.instruments.entity.*;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public final class InstrumentResponses {
    private InstrumentResponses() {}

    public record InstrumentResponse(UUID id, UUID projectId, UUID methodologyId, UUID dataCollectionMethodId, ResearchInstrumentType type, String title, ResearchInstrumentStatus status, ContentOrigin origin, int revisionNumber, OffsetDateTime createdAt) {}
    public record QuestionnaireResponse(UUID id, InstrumentResponse instrument, String introduction, boolean anonymous, Integer estimatedCompletionMinutes, String targetRespondentDescription) {}
    public record SectionResponse(UUID id, UUID ownerId, String title, String description, int displayOrder) {}
    public record ItemResponse(UUID id, String code, String text, String type, boolean required, int displayOrder, ContentOrigin origin) {}
    public record OptionResponse(UUID id, String value, String label, int displayOrder, Double numericScore) {}
    public record ScaleResponse(UUID id, UUID projectId, String name, ResponseScaleType type, Integer minimumValue, Integer maximumValue) {}
    public record GuideResponse(UUID id, InstrumentResponse instrument, String openingScript, String closingScript, Integer durationMinutes, String guideType) {}
    public record PilotStudyResponse(UUID id, UUID instrumentId, String title, PilotStudyStatus status, Integer plannedParticipantCount, Integer actualParticipantCount) {}
    public record ValidityAssessmentResponse(UUID id, UUID instrumentId, ValidityType type, AssessmentStatus status, Double score, ContentOrigin origin) {}
    public record ReliabilityAssessmentResponse(UUID id, UUID instrumentId, ReliabilityMethod method, AssessmentStatus status, Double coefficient, ContentOrigin origin) {}
    public record ExpertReviewResponse(UUID id, UUID instrumentId, String reviewerCode) {}
    public record ExpertRatingResponse(UUID id, UUID reviewId, InstrumentItemTargetType targetType, UUID targetId, ExpertRatingCriterion criterion, int rating) {}
    public record InstrumentValidationResult(boolean validForActivation, List<String> errors, List<String> warnings, List<String> information) {
        public InstrumentValidationResult { errors = errors == null ? List.of() : List.copyOf(errors); warnings = warnings == null ? List.of() : List.copyOf(warnings); information = information == null ? List.of() : List.copyOf(information); }
    }
    public record CoverageRow(UUID objectiveId, UUID researchQuestionId, UUID instrumentId, String instrumentTitle, String itemCode) {}
    public record CoverageMatrixResponse(List<CoverageRow> rows, List<String> warnings) {}
    public record CviResult(double scaleContentValidityAverage, int expertCount, int relevanceThreshold, String calculationMethod) {}
    public record CronbachAlphaResult(Double coefficient, int itemCount, int observationCount, String missingDataPolicy, String calculationMethod, List<String> warnings) {}
}
