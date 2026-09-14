package com.researchassistant.instruments.dto;

import com.researchassistant.common.enums.ContentOrigin;
import com.researchassistant.instruments.entity.*;
import jakarta.validation.constraints.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public final class InstrumentRequests {
    private InstrumentRequests() {}

    public record CreateInstrumentRequest(@NotNull UUID methodologyId, @NotNull UUID dataCollectionMethodId, @NotBlank @Size(max = 255) String title, String description, String instructions, ContentOrigin origin, List<UUID> objectiveIds, List<UUID> questionIds, List<UUID> hypothesisIds) {}
    public record CreateQuestionnaireRequest(@NotNull UUID methodologyId, @NotNull UUID dataCollectionMethodId, @NotBlank @Size(max = 255) String title, String description, String instructions, String introduction, boolean anonymous, @Min(1) Integer estimatedCompletionMinutes, String targetRespondentDescription, ContentOrigin origin) {}
    public record CreateSectionRequest(@NotBlank @Size(max = 255) String title, String description, @Min(1) Integer displayOrder) {}
    public record CreateQuestionnaireItemRequest(String itemCode, @NotBlank String prompt, QuestionnaireItemType type, boolean required, @Min(1) Integer displayOrder, String helpText, String validationRulesJson, UUID responseScaleId, boolean reverseScored, ContentOrigin origin, List<UUID> objectiveIds, List<UUID> questionIds, List<UUID> hypothesisIds, List<UUID> conceptualVariableIds) {}
    public record CreateOptionRequest(@NotBlank @Size(max = 255) String value, @NotBlank @Size(max = 500) String label, @Min(1) Integer displayOrder, Double numericScore) {}
    public record CreateScaleRequest(@NotBlank @Size(max = 255) String name, ResponseScaleType type, Integer minimumValue, Integer maximumValue) {}
    public record CreateScaleOptionRequest(int numericValue, @NotBlank @Size(max = 255) String label, @Min(1) Integer displayOrder) {}
    public record CreateInterviewGuideRequest(@NotNull UUID methodologyId, @NotNull UUID dataCollectionMethodId, @NotBlank @Size(max = 255) String title, String description, String openingScript, String closingScript, @Min(1) Integer estimatedDurationMinutes, InterviewType interviewType, String interviewerInstructions, ContentOrigin origin) {}
    public record CreateInterviewQuestionRequest(String questionCode, @NotBlank String questionText, @Min(1) Integer displayOrder, boolean required, ContentOrigin origin, List<UUID> objectiveIds, List<UUID> questionIds, List<UUID> hypothesisIds, List<UUID> conceptualVariableIds) {}
    public record CreateProbeRequest(@NotBlank String probeText, @Min(1) Integer displayOrder) {}
    public record CreateFocusGroupGuideRequest(@NotNull UUID methodologyId, @NotNull UUID dataCollectionMethodId, @NotBlank @Size(max = 255) String title, String description, String facilitatorInstructions, String openingScript, String closingScript, @Min(1) Integer plannedDurationMinutes, @Min(1) Integer recommendedMinParticipants, @Min(1) Integer recommendedMaxParticipants, String groupCompositionGuidance, ContentOrigin origin) {}
    public record CreateFocusGroupQuestionRequest(String questionCode, @NotBlank String questionText, FocusGroupQuestionType type, @Min(1) Integer displayOrder, String moderatorNotes, ContentOrigin origin) {}
    public record CreateObservationChecklistRequest(@NotNull UUID methodologyId, @NotNull UUID dataCollectionMethodId, @NotBlank @Size(max = 255) String title, String description, ObservationType observationType, String observerInstructions, String observationSetting, @Min(1) Integer estimatedDurationMinutes, ContentOrigin origin) {}
    public record CreateObservationItemRequest(String itemCode, @NotBlank String description, ObservationItemType type, boolean required, @Min(1) Integer displayOrder, ContentOrigin origin) {}
    public record CreatePilotStudyRequest(@NotBlank @Size(max = 255) String title, PilotStudyStatus status, @Min(1) Integer plannedParticipantCount, @Min(0) Integer actualParticipantCount, String participantDescription, String location, LocalDate startDate, LocalDate endDate, String procedure, String observations, String issuesIdentified, String modificationsRecommended, String modificationsImplemented) {}
    public record CreateValidityAssessmentRequest(ValidityType type, AssessmentStatus status, @NotBlank String methodDescription, String findings, String interpretation, Double score, ContentOrigin origin) {}
    public record CreateReliabilityAssessmentRequest(ReliabilityMethod method, AssessmentStatus status, Double coefficient, Integer itemCount, Integer respondentCount, String assumptions, String interpretation, ContentOrigin origin) {}
    public record CreateExpertReviewRequest(@NotBlank @Size(max = 100) String reviewerCode, String reviewerRoleOrExpertise, LocalDate reviewDate, String overallComments) {}
    public record CreateExpertRatingRequest(InstrumentItemTargetType targetType, @NotNull UUID targetId, ExpertRatingCriterion criterion, @Min(1) int rating, String comment) {}
    public record CronbachAlphaRequest(List<List<Double>> observations) {}
}
