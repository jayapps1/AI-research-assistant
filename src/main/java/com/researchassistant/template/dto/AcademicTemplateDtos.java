package com.researchassistant.template.dto;

import com.researchassistant.analysis.entity.ReportChapterType;
import com.researchassistant.analysis.entity.SectionGenerationPolicy;
import com.researchassistant.analysis.entity.SectionRequirementLevel;
import com.researchassistant.analysis.entity.SectionSemanticPurpose;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class AcademicTemplateDtos {

    public static final String NOT_SPECIFIED = "NOT_SPECIFIED";

    public record ExtractedAcademicTemplate(
            String institution,
            String department,
            String programme,
            String documentType,
            String citationStyle,
            List<TemplateSectionDefinitionDto> frontMatter,
            List<TemplateChapterDefinitionDto> chapters,
            List<TemplateSectionDefinitionDto> appendices,
            TemplateFormattingRulesDto formattingRules,
            List<UncertainItemDto> uncertainItems
    ) {
        public ExtractedAcademicTemplate {
            frontMatter = frontMatter != null ? frontMatter : List.of();
            chapters = chapters != null ? chapters : List.of();
            appendices = appendices != null ? appendices : List.of();
            uncertainItems = uncertainItems != null ? uncertainItems : List.of();
            formattingRules = formattingRules != null ? formattingRules : TemplateFormattingRulesDto.defaultUnspecified();
            institution = institution != null && !institution.isBlank() ? institution : NOT_SPECIFIED;
            department = department != null && !department.isBlank() ? department : NOT_SPECIFIED;
            programme = programme != null && !programme.isBlank() ? programme : NOT_SPECIFIED;
            documentType = documentType != null && !documentType.isBlank() ? documentType : NOT_SPECIFIED;
            citationStyle = citationStyle != null && !citationStyle.isBlank() ? citationStyle : NOT_SPECIFIED;
        }
    }

    public record TemplateFormattingRulesDto(
            String fontFamily,
            String bodyFontSize,
            String heading1FontSize,
            String heading2FontSize,
            String lineSpacing,
            TemplateMarginsDto margins,
            String numberingStyle,
            String pageNumbering,
            String chapterBreak
    ) {
        public static TemplateFormattingRulesDto defaultUnspecified() {
            return new TemplateFormattingRulesDto(
                    NOT_SPECIFIED, NOT_SPECIFIED, NOT_SPECIFIED, NOT_SPECIFIED,
                    NOT_SPECIFIED, TemplateMarginsDto.defaultUnspecified(),
                    NOT_SPECIFIED, NOT_SPECIFIED, NOT_SPECIFIED
            );
        }
    }

    public record TemplateMarginsDto(
            String leftMargin,
            String rightMargin,
            String topMargin,
            String bottomMargin
    ) {
        public static TemplateMarginsDto defaultUnspecified() {
            return new TemplateMarginsDto(NOT_SPECIFIED, NOT_SPECIFIED, NOT_SPECIFIED, NOT_SPECIFIED);
        }
    }

    public record TemplateChapterDefinitionDto(
            Integer chapterNumber,
            String title,
            ReportChapterType type,
            boolean required,
            List<TemplateSectionDefinitionDto> sections
    ) {
        public TemplateChapterDefinitionDto {
            sections = sections != null ? sections : List.of();
            type = type != null ? type : ReportChapterType.CUSTOM;
        }
    }

    public record TemplateSectionDefinitionDto(
            String sectionNumber,
            String heading,
            SectionRequirementLevel requirementLevel,
            SectionSemanticPurpose semanticPurpose,
            SectionGenerationPolicy generationPolicy,
            String description,
            List<TemplateSectionDefinitionDto> subsections
    ) {
        public TemplateSectionDefinitionDto {
            subsections = subsections != null ? subsections : List.of();
            requirementLevel = requirementLevel != null ? requirementLevel : SectionRequirementLevel.REQUIRED;
            semanticPurpose = semanticPurpose != null ? semanticPurpose : SectionSemanticPurpose.CUSTOM;
            generationPolicy = generationPolicy != null ? generationPolicy : semanticPurpose.defaultPolicy();
        }
    }

    public record UncertainItemDto(
            String itemType,
            String rawText,
            String reason,
            double confidence
    ) {}

    public record AcademicDocumentGuidelineResponse(
            UUID id,
            UUID workspaceId,
            UUID projectId,
            UUID documentId,
            String originalFileName,
            String sourceType,
            int version,
            String status,
            String institution,
            String department,
            String programme,
            String documentType,
            String citationStyle,
            ExtractedAcademicTemplate extractedTemplate,
            String uploadedAt,
            String approvedAt,
            String approvedBy
    ) {}

    public record UpdateAcademicGuidelineRequest(
            String institution,
            String department,
            String programme,
            String documentType,
            String citationStyle,
            ExtractedAcademicTemplate templateStructure
    ) {}

    public record ApproveGuidelineRequest(
            UUID targetProjectId,
            boolean applyToProject,
            boolean preserveExistingContent
    ) {}

    public record DocumentStructureValidationResponse(
            boolean valid,
            List<ValidationWarning> warnings,
            int totalSections,
            int requiredSections,
            int presentRequiredSections,
            int missingRequiredSections
    ) {}

    public record ValidationWarning(
            String code,
            String message,
            String sectionHeading,
            String chapterTitle,
            SectionRequirementLevel level
    ) {}

    public record DynamicTableOfContentsResponse(
            UUID reportId,
            String reportTitle,
            List<TocChapterItem> chapters,
            int totalChapters,
            int totalSections
    ) {}

    public record TocChapterItem(
            UUID id,
            Integer chapterNumber,
            String title,
            ReportChapterType type,
            int displayOrder,
            int estimatedPageNumber,
            List<TocSectionItem> sections
    ) {}

    public record TocSectionItem(
            UUID id,
            UUID parentSectionId,
            String sectionNumber,
            String heading,
            int level,
            int displayOrder,
            int estimatedPageNumber,
            SectionRequirementLevel requirementLevel,
            SectionSemanticPurpose semanticPurpose,
            List<TocSectionItem> subsections
    ) {}

    public record RecommendedTemplateResponse(
            UUID id,
            String name,
            String type,
            String institution,
            String department,
            String supportedWorkspaceTypes,
            String description,
            boolean systemTemplate,
            int relevanceScore,
            String relevanceReason
    ) {}
}
