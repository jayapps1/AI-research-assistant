package com.researchassistant.integrity.dto;

import com.researchassistant.integrity.entity.*;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.UUID;

public final class IntegrityDtos {
    private IntegrityDtos() {}
    public record SimilarityRequest(@NotNull SimilarityTargetType targetType, UUID targetId, String customText) {}
    public record SimilarityResponse(UUID id, SimilarityTargetType targetType, SimilarityProvider provider, SimilarityCheckStatus status,
            Double overallSimilarityPercent, String limitation, List<SimilarityMatchResponse> matches) {}
    public record SimilarityMatchResponse(UUID id, SimilarityMatchType type, double score, String matchedText, String sourceTextSnapshot, UUID documentId) {}
    public record WritingReviewResponse(UUID id, AcademicWritingReviewStatus status, List<WritingIssueResponse> issues) {}
    public record WritingIssueResponse(WritingIssueType type, IssueSeverity severity, String message, String suggestion) {}
    public record IntegrityReviewResponse(UUID id, IntegrityReviewStatus status, List<IntegrityIssueResponse> issues) {}
    public record IntegrityIssueResponse(ResearchIntegrityIssueType type, IssueSeverity severity, String message, String artifactType, UUID artifactId) {}
    public record AiUsageSummary(UUID projectId, long aiGeneratedArtifacts, long aiAssistedArtifacts, List<String> artifactSummaries) {}
}
