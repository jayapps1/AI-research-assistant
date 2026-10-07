package com.researchassistant.evidence.dto;

import com.researchassistant.evidence.entity.EvidenceAnalysisStatus;
import com.researchassistant.evidence.entity.EvidenceType;
import com.researchassistant.evidence.entity.ProjectEvidence;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public class ProjectEvidenceDtos {

    public record ProjectEvidenceResponse(
            UUID id,
            UUID projectId,
            UUID workspaceId,
            UUID reportId,
            UUID sectionId,
            String sectionNumber,
            String sectionHeading,
            String chapterTitle,
            Integer chapterNumber,
            UUID storageObjectId,
            String downloadUrl,
            String originalFilename,
            String mimeType,
            long fileSizeBytes,
            EvidenceType evidenceType,
            String figureLabel,
            String caption,
            String renderedCaption,
            String description,
            String altText,
            int displayOrder,
            String aiVisualAnalysis,
            EvidenceAnalysisStatus aiAnalysisStatus,
            String aiAnalysisError,
            String metadataJson,
            UUID createdById,
            String createdByEmail,
            OffsetDateTime createdAt,
            OffsetDateTime updatedAt
    ) {
        public static ProjectEvidenceResponse from(ProjectEvidence e, String computedFigureLabel) {
            String label = computedFigureLabel != null && !computedFigureLabel.isBlank()
                    ? computedFigureLabel
                    : (e.getFigureLabel() != null ? e.getFigureLabel() : "");

            String renderedCaption;
            if (e.getCaption() != null && !e.getCaption().isBlank()) {
                renderedCaption = !label.isBlank() ? label + ": " + e.getCaption() : e.getCaption();
            } else {
                renderedCaption = label;
            }

            String downloadUrl = e.getStorageObject() != null
                    ? "/api/v1/storage-objects/" + e.getStorageObject().getId() + "/download"
                    : null;

            String secNum = e.getSection() != null ? e.getSection().getSectionNumber() : null;
            String secHead = e.getSection() != null ? e.getSection().getHeading() : null;
            String chapTitle = (e.getSection() != null && e.getSection().getChapter() != null)
                    ? e.getSection().getChapter().getTitle()
                    : null;
            Integer chapNum = (e.getSection() != null && e.getSection().getChapter() != null)
                    ? e.getSection().getChapter().getChapterNumber()
                    : null;

            return new ProjectEvidenceResponse(
                    e.getId(),
                    e.getProject().getId(),
                    e.getWorkspace().getId(),
                    e.getReport() != null ? e.getReport().getId() : null,
                    e.getSection() != null ? e.getSection().getId() : null,
                    secNum,
                    secHead,
                    chapTitle,
                    chapNum,
                    e.getStorageObject() != null ? e.getStorageObject().getId() : null,
                    downloadUrl,
                    e.getOriginalFilename(),
                    e.getMimeType(),
                    e.getFileSizeBytes(),
                    e.getEvidenceType(),
                    label,
                    e.getCaption(),
                    renderedCaption,
                    e.getDescription(),
                    e.getAltText(),
                    e.getDisplayOrder(),
                    e.getAiVisualAnalysis(),
                    e.getAiAnalysisStatus(),
                    e.getAiAnalysisError(),
                    e.getMetadataJson(),
                    e.getCreatedBy() != null ? e.getCreatedBy().getId() : null,
                    e.getCreatedBy() != null ? e.getCreatedBy().getEmail() : null,
                    e.getCreatedAt(),
                    e.getUpdatedAt()
            );
        }
    }

    public record CreateStructuredEvidenceRequest(
            @NotNull EvidenceType evidenceType,
            @NotBlank String caption,
            String description,
            UUID sectionId,
            String altText,
            String metadataJson
    ) {}

    public record UpdateProjectEvidenceRequest(
            String caption,
            String description,
            UUID sectionId,
            String altText,
            EvidenceType evidenceType,
            Integer displayOrder,
            String metadataJson
    ) {}

    public record ReorderEvidenceRequest(
            @NotNull List<UUID> evidenceIds
    ) {}

    public record TestEvidenceRecord(
            String testCase,
            String expectedResult,
            String actualResult,
            String status, // PASS, FAIL
            String notes
    ) {}

    public record TableDataRecord(
            List<String> headers,
            List<List<String>> rows
    ) {}

    public record ProjectImageAnalysisResult(
            EvidenceAnalysisStatus status,
            String visibleSummary,
            List<String> observableComponents,
            String observableWorkflow,
            String sectionRelevance,
            String rawAnalysis,
            String errorMessage
    ) {}

    public record ListOfFiguresItem(
            UUID figureId,
            String figureNumber,
            String caption,
            int pageNumber,
            UUID sectionId,
            String sectionNumber,
            String sectionHeading,
            String chapterTitle
    ) {}

    public record ListOfTablesItem(
            UUID tableId,
            String tableNumber,
            String caption,
            int pageNumber,
            UUID sectionId,
            String sectionNumber,
            String sectionHeading,
            String chapterTitle
    ) {}
}
