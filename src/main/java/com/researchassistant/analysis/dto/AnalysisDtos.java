package com.researchassistant.analysis.dto;

import com.researchassistant.analysis.entity.*;
import com.researchassistant.common.enums.ContentOrigin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public final class AnalysisDtos {
    private AnalysisDtos() {}

    public record CreateAnalysisRunRequest(UUID objectiveId, UUID questionId, UUID hypothesisId, UUID datasetId,
                                           AnalysisRun.QualitativeSourceType qualitativeSourceType,
                                           UUID qualitativeSourceReference, @NotBlank String title,
                                           @NotNull AnalysisRun.AnalysisType analysisType,
                                           String methodDescription, String parametersJson) {}
    public record CompleteAnalysisRunRequest(@NotBlank String summary, String resultPayloadJson, String limitations, ContentOrigin generatedBy) {}

    public record CreateFindingRequest(UUID objectiveId, UUID questionId, UUID hypothesisId,
                                       @NotEmpty List<UUID> analysisResultIds,
                                       @NotNull ResearchFindingType type, @NotBlank String title,
                                       @NotBlank String findingText, String evidenceSummary,
                                       String resultValueSnapshot, @Positive Integer displayOrder,
                                       ContentOrigin origin) {}
    public record UpdateFindingRequest(UUID objectiveId, UUID questionId, UUID hypothesisId, ResearchFindingType type,
                                       String title, String findingText, String evidenceSummary,
                                       String resultValueSnapshot, Integer displayOrder, ContentOrigin origin) {}
    public record GenerateFindingRequest(@NotNull UUID objectiveId, UUID questionId, UUID hypothesisId,
                                         @NotEmpty List<UUID> analysisResultIds, String instructions) {}
    public record GeneratedDraftResponse(String draftText, List<UUID> sourceIds, String safetyPolicy) {}

    public record CreateDiscussionRequest(String title, @NotBlank String discussionText, String relationToLiterature,
                                          String implications, String limitations, @Positive Integer displayOrder,
                                          ContentOrigin origin) {}
    public record UpdateDiscussionRequest(String title, String discussionText, String relationToLiterature,
                                          String implications, String limitations, DiscussionStatus status,
                                          Integer displayOrder, ContentOrigin origin) {}

    public record CreateConclusionRequest(UUID objectiveId, UUID questionId, ResearchConclusionType type,
                                          @NotEmpty List<UUID> findingIds, String title,
                                          @NotBlank String conclusionText, String scopeNote,
                                          @Positive Integer displayOrder, ContentOrigin origin) {}
    public record UpdateConclusionRequest(UUID objectiveId, UUID questionId, ResearchConclusionType type,
                                          List<UUID> findingIds, String title, String conclusionText,
                                          String scopeNote, Integer displayOrder, ContentOrigin origin) {}

    public record CreateRecommendationRequest(ResearchRecommendationType type, String title,
                                              @NotBlank String recommendationText, String targetAudience,
                                              RecommendationPriority priority, List<UUID> findingIds,
                                              List<UUID> conclusionIds, String rationale,
                                              @Positive Integer displayOrder, ContentOrigin origin) {}
    public record UpdateRecommendationRequest(ResearchRecommendationType type, String title, String recommendationText,
                                              String targetAudience, RecommendationPriority priority,
                                              List<UUID> findingIds, List<UUID> conclusionIds,
                                              String rationale, Integer displayOrder, ContentOrigin origin) {}

    public record CreateReportRequest(@NotBlank String title, ResearchReportType type, UUID templateId,
                                      String institutionName, String departmentName, String authorName,
                                      String supervisorName, String degreeProgram, Integer submissionYear,
                                      CitationStyle citationStyle, ContentOrigin origin) {}
    public record UpdateReportRequest(String title, String institutionName, String departmentName, String authorName,
                                      String supervisorName, String degreeProgram, Integer submissionYear,
                                      CitationStyle citationStyle) {}
    public record CreateChapterRequest(@NotNull ReportChapterType type, @NotBlank String title,
                                       Integer chapterNumber, @Positive Integer displayOrder) {}
    public record UpdateChapterRequest(ReportChapterType type, String title, Integer chapterNumber, Integer displayOrder) {}
    public record CreateSectionRequest(@NotNull ReportSectionType type, @NotBlank String heading, String content,
                                       @Positive Integer displayOrder, ContentOrigin origin,
                                       String sourceArtifactType, UUID sourceArtifactId) {}
    public record UpdateSectionRequest(ReportSectionType type, String heading, String content, Integer displayOrder,
                                       ContentOrigin origin, Boolean sourceOutOfDate) {}

    public record AnalysisRunResponse(UUID id, UUID projectId, UUID objectiveId, UUID questionId, UUID hypothesisId,
                                      UUID datasetId, AnalysisRun.QualitativeSourceType qualitativeSourceType,
                                      UUID qualitativeSourceReference, String title,
                                      AnalysisRun.AnalysisType analysisType, AnalysisRun.Status status,
                                      OffsetDateTime startedAt, OffsetDateTime completedAt) {
        public static AnalysisRunResponse from(AnalysisRun run) {
            return new AnalysisRunResponse(run.getId(), run.getProject().getId(), AnalysisDtos.id(run.getObjective()), AnalysisDtos.id(run.getQuestion()),
                    AnalysisDtos.id(run.getHypothesis()), AnalysisDtos.id(run.getDataset()), run.getQualitativeSourceType(), run.getQualitativeSourceReference(),
                    run.getTitle(), run.getAnalysisType(), run.getStatus(), run.getStartedAt(), run.getCompletedAt());
        }
    }
    public record AnalysisResultResponse(UUID id, UUID analysisRunId, UUID projectId, String summary, String resultPayloadJson,
                                         String limitations, ContentOrigin generatedBy, OffsetDateTime createdAt) {
        public static AnalysisResultResponse from(AnalysisResult result) {
            return new AnalysisResultResponse(result.getId(), result.getAnalysisRun().getId(), result.getProject().getId(),
                    result.getSummary(), result.getResultPayloadJson(), result.getLimitations(), result.getGeneratedBy(), result.getCreatedAt());
        }
    }
    public record FindingResponse(UUID id, UUID projectId, UUID objectiveId, UUID questionId, UUID hypothesisId,
                                  List<UUID> analysisResultIds, ResearchFindingType type, ResearchFindingStatus status,
                                  String title, String findingText, String evidenceSummary, String resultValueSnapshot,
                                  ContentOrigin origin, int displayOrder, int revisionNumber, OffsetDateTime updatedAt) {
        public static FindingResponse from(ResearchFinding finding) {
            List<UUID> resultIds = finding.getAnalysisResults().stream().map(AnalysisResult::getId).toList();
            if (resultIds.isEmpty() && finding.getAnalysisResult() != null) resultIds = List.of(finding.getAnalysisResult().getId());
            return new FindingResponse(finding.getId(), finding.getProject().getId(), AnalysisDtos.id(finding.getObjective()), AnalysisDtos.id(finding.getQuestion()),
                    AnalysisDtos.id(finding.getHypothesis()), resultIds, finding.getType(), finding.getStatus(), finding.getTitle(),
                    finding.getFindingText(), finding.getEvidenceSummary(), finding.getResultValueSnapshot(), finding.getOrigin(),
                    finding.getDisplayOrder(), finding.getRevisionNumber(), finding.getUpdatedAt());
        }
    }
    public record DiscussionResponse(UUID id, UUID projectId, UUID findingId, String title, String discussionText,
                                     DiscussionStatus status, ContentOrigin origin, int displayOrder,
                                     int revisionNumber, OffsetDateTime updatedAt) {
        public static DiscussionResponse from(FindingDiscussion discussion) {
            return new DiscussionResponse(discussion.getId(), discussion.getProject().getId(), discussion.getFinding().getId(),
                    discussion.getTitle(), discussion.getDiscussionText(), discussion.getStatus(), discussion.getOrigin(),
                    discussion.getDisplayOrder(), discussion.getRevisionNumber(), discussion.getUpdatedAt());
        }
    }
    public record DiscussionEvidenceResponse(UUID id, UUID discussionId, UUID documentId, UUID documentVersionId,
                                             UUID pageId, UUID chunkId, UUID ragEvidenceId, String evidenceSnapshot,
                                             DiscussionEvidenceRelationship relationship, int citationOrdinal) {
        public static DiscussionEvidenceResponse from(DiscussionEvidence evidence) {
            return new DiscussionEvidenceResponse(evidence.getId(), evidence.getDiscussion().getId(), evidence.getDocument().getId(),
                    evidence.getDocumentVersion().getId(), AnalysisDtos.id(evidence.getPage()), AnalysisDtos.id(evidence.getChunk()), AnalysisDtos.id(evidence.getRagEvidence()),
                    evidence.getEvidenceSnapshot(), evidence.getRelationship(), evidence.getCitationOrdinal());
        }
    }
    public record ConclusionResponse(UUID id, UUID projectId, UUID objectiveId, UUID questionId, ResearchConclusionType type,
                                     List<UUID> findingIds, ResearchConclusionStatus status, String title,
                                     String conclusionText, int displayOrder, int revisionNumber) {
        public static ConclusionResponse from(ResearchConclusion conclusion) {
            return new ConclusionResponse(conclusion.getId(), conclusion.getProject().getId(), AnalysisDtos.id(conclusion.getObjective()),
                    AnalysisDtos.id(conclusion.getQuestion()), conclusion.getType(), conclusion.getFindings().stream().map(ResearchFinding::getId).toList(),
                    conclusion.getStatus(), conclusion.getTitle(), conclusion.getConclusionText(), conclusion.getDisplayOrder(),
                    conclusion.getRevisionNumber());
        }
    }
    public record RecommendationResponse(UUID id, UUID projectId, ResearchRecommendationType type, String title,
                                         String recommendationText, String targetAudience, RecommendationPriority priority,
                                         ResearchRecommendationStatus status, List<UUID> findingIds, List<UUID> conclusionIds,
                                         int displayOrder, int revisionNumber) {
        public static RecommendationResponse from(ResearchRecommendation recommendation) {
            return new RecommendationResponse(recommendation.getId(), recommendation.getProject().getId(), recommendation.getType(),
                    recommendation.getTitle(), recommendation.getRecommendationText(), recommendation.getTargetAudience(),
                    recommendation.getPriority(), recommendation.getStatus(),
                    recommendation.getFindings().stream().map(ResearchFinding::getId).toList(),
                    recommendation.getConclusions().stream().map(ResearchConclusion::getId).toList(),
                    recommendation.getDisplayOrder(), recommendation.getRevisionNumber());
        }
    }
    public record ReportResponse(UUID id, UUID projectId, String title, ResearchReportType type, ResearchReportStatus status,
                                 CitationStyle citationStyle, int revisionNumber, OffsetDateTime updatedAt) {
        public static ReportResponse from(ResearchReport report) {
            return new ReportResponse(report.getId(), report.getProject().getId(), report.getTitle(), report.getType(),
                    report.getStatus(), report.getCitationStyle(), report.getRevisionNumber(), report.getUpdatedAt());
        }
    }
    public record ChapterResponse(UUID id, UUID reportId, ReportChapterType type, String title, Integer chapterNumber, int displayOrder) {
        public static ChapterResponse from(ResearchReportChapter chapter) {
            return new ChapterResponse(chapter.getId(), chapter.getReport().getId(), chapter.getType(), chapter.getTitle(),
                    chapter.getChapterNumber(), chapter.getDisplayOrder());
        }
    }
    public record SectionResponse(UUID id, UUID chapterId, ReportSectionType type, String heading, String content,
                                  int displayOrder, ContentOrigin origin, String sourceArtifactType, UUID sourceArtifactId,
                                  Integer sourceRevisionNumber, boolean sourceOutOfDate, boolean manuallyEdited,
                                  int revisionNumber) {
        public static SectionResponse from(ResearchReportSection section) {
            return new SectionResponse(section.getId(), section.getChapter().getId(), section.getType(), section.getHeading(),
                    section.getContent(), section.getDisplayOrder(), section.getOrigin(), section.getSourceArtifactType(),
                    section.getSourceArtifactId(), section.getSourceRevisionNumber(), section.isSourceOutOfDate(),
                    section.isManuallyEdited(), section.getRevisionNumber());
        }
    }
    public record CitationResponse(UUID id, UUID sectionId, UUID documentId, UUID documentVersionId, UUID pageId, UUID chunkId,
                                   String documentCode, int citationOrdinal, String supportingTextSnapshot) {
        public static CitationResponse from(ResearchReportCitation citation) {
            return new CitationResponse(citation.getId(), citation.getSection().getId(), citation.getDocument().getId(),
                    citation.getDocumentVersion().getId(), AnalysisDtos.id(citation.getPage()), AnalysisDtos.id(citation.getChunk()), citation.getDocumentCode(),
                    citation.getCitationOrdinal(), citation.getSupportingTextSnapshot());
        }
    }
    public record ValidationIssue(String severity, String code, String message, UUID artifactId) {}
    public record ReportValidationResponse(List<ValidationIssue> errors, List<ValidationIssue> warnings, List<ValidationIssue> information) {}
    public record TraceabilityRow(UUID objectiveId, String objectiveText, UUID questionId, UUID hypothesisId,
                                  long analysisCount, long findingCount, long conclusionCount, long recommendationCount,
                                  List<String> warnings) {}
    public record TraceabilityMatrixResponse(UUID projectId, List<TraceabilityRow> rows, List<String> projectWarnings) {}

    public record TemplateResponse(UUID id, String name, ResearchReportType type, String institution, String department,
                                    boolean systemTemplate, CitationStyle defaultCitationStyle, boolean citationStyleLocked,
                                    String description, String configurationJson) {
        public static TemplateResponse from(ResearchReportTemplate template) {
            return new TemplateResponse(template.getId(), template.getName(), template.getType(), template.getInstitution(),
                    template.getDepartment(), template.isSystemTemplate(), template.getDefaultCitationStyle(),
                    template.isCitationStyleLocked(), template.getDescription(), template.getConfigurationJson());
        }
    }

    public record SectionCapability(String sectionKey, String capabilityStatus, String description, boolean requiresData, boolean requiresFindings) {}
    public record SectionCapabilitiesResponse(UUID projectId, List<SectionCapability> capabilities) {}

    public record TableOfContentsSectionItem(String heading, Integer displayOrder, ReportSectionType type) {}
    public record TableOfContentsItem(String title, Integer chapterNumber, Integer displayOrder, List<TableOfContentsSectionItem> sections) {}
    public record TableOfContentsResponse(UUID reportId, String reportTitle, List<TableOfContentsItem> chapters, String formattedMarkdown) {}

    private static UUID id(Object entity) {
        if (entity == null) return null;
        if (entity instanceof com.researchassistant.researchdesign.entity.ResearchObjective value) return value.getId();
        if (entity instanceof com.researchassistant.researchdesign.entity.ResearchQuestion value) return value.getId();
        if (entity instanceof com.researchassistant.researchdesign.entity.ResearchHypothesis value) return value.getId();
        if (entity instanceof com.researchassistant.dataset.model.ResearchDataset value) return value.getId();
        if (entity instanceof com.researchassistant.document.entity.DocumentPage value) return value.getId();
        if (entity instanceof com.researchassistant.document.entity.DocumentChunk value) return value.getId();
        if (entity instanceof com.researchassistant.rag.entity.RagQueryEvidence value) return value.getId();
        throw new IllegalArgumentException("Unsupported entity reference.");
    }
}
