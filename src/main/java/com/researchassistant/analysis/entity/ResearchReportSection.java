package com.researchassistant.analysis.entity;

import com.researchassistant.common.enums.ContentOrigin;
import com.researchassistant.identity.entity.User;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "research_report_sections")
@Getter @Setter @NoArgsConstructor
public class ResearchReportSection {
    @Id @Column(nullable = false, updatable = false)
    private UUID id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "chapter_id", nullable = false)
    private ResearchReportChapter chapter;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "parent_section_id")
    private ResearchReportSection parentSection;
    @Column(name = "section_number", length = 32)
    private String sectionNumber;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 80)
    private ReportSectionType type = ReportSectionType.CUSTOM;
    @Column(nullable = false, length = 255)
    private String heading;
    @Column(columnDefinition = "TEXT")
    private String content;
    @Column(name = "content_json", columnDefinition = "TEXT")
    private String contentJson;
    @Column(name = "plain_text", columnDefinition = "TEXT")
    private String plainText;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 40)
    private ReportSectionStatus status = ReportSectionStatus.NOT_STARTED;
    @Column(name = "display_order", nullable = false)
    private int displayOrder = 1;
    @Column(name = "required", nullable = false)
    private boolean required = false;
    @Enumerated(EnumType.STRING) @Column(name = "requirement_level", nullable = false, length = 30)
    private SectionRequirementLevel requirementLevel = SectionRequirementLevel.REQUIRED;
    @Column(name = "template_guidance", columnDefinition = "TEXT")
    private String templateGuidance;
    @Column(name = "system_defined", nullable = false)
    private boolean systemDefined = false;
    @Column(name = "ai_enabled", nullable = false)
    private boolean aiEnabled = true;
    @Enumerated(EnumType.STRING) @Column(name = "semantic_purpose", length = 80)
    private SectionSemanticPurpose semanticPurpose;
    @Enumerated(EnumType.STRING) @Column(name = "generation_policy", length = 80)
    private SectionGenerationPolicy generationPolicy;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 30)
    private ContentOrigin origin = ContentOrigin.USER;
    @Column(name = "source_artifact_type", length = 100)
    private String sourceArtifactType;
    @Column(name = "source_artifact_id")
    private UUID sourceArtifactId;
    @Column(name = "source_revision_number")
    private Integer sourceRevisionNumber;
    @Column(name = "source_out_of_date", nullable = false)
    private boolean sourceOutOfDate;
    @Column(name = "manually_edited", nullable = false)
    private boolean manuallyEdited;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "created_by")
    private User createdBy;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "updated_by")
    private User updatedBy;
    @Column(name = "revision_number", nullable = false)
    private int revisionNumber = 1;
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    public SectionSemanticPurpose resolveSemanticPurpose() {
        if (semanticPurpose != null) {
            return semanticPurpose;
        }
        if (type != null) {
            switch (type) {
                case TITLE_PAGE -> { return SectionSemanticPurpose.TITLE_PAGE; }
                case TABLE_OF_CONTENTS -> { return SectionSemanticPurpose.TABLE_OF_CONTENTS; }
                case LIST_OF_FIGURES -> { return SectionSemanticPurpose.LIST_OF_FIGURES; }
                case LIST_OF_TABLES -> { return SectionSemanticPurpose.LIST_OF_TABLES; }
                case DECLARATION -> { return SectionSemanticPurpose.DECLARATION; }
                case CERTIFICATION -> { return SectionSemanticPurpose.CERTIFICATION; }
                case DEDICATION -> { return SectionSemanticPurpose.DEDICATION; }
                case ACKNOWLEDGEMENTS -> { return SectionSemanticPurpose.ACKNOWLEDGEMENTS; }
                case ABSTRACT -> { return SectionSemanticPurpose.ABSTRACT; }
                case BACKGROUND -> { return SectionSemanticPurpose.BACKGROUND; }
                case PROBLEM_STATEMENT -> { return SectionSemanticPurpose.PROBLEM_STATEMENT; }
                case OBJECTIVES -> { return SectionSemanticPurpose.OBJECTIVES; }
                case RESEARCH_QUESTIONS -> { return SectionSemanticPurpose.RESEARCH_QUESTIONS; }
                case HYPOTHESES -> { return SectionSemanticPurpose.HYPOTHESES; }
                case SIGNIFICANCE -> { return SectionSemanticPurpose.SIGNIFICANCE; }
                case SCOPE -> { return SectionSemanticPurpose.SCOPE; }
                case LITERATURE_REVIEW -> { return SectionSemanticPurpose.LITERATURE_REVIEW; }
                case RESEARCH_GAP -> { return SectionSemanticPurpose.RESEARCH_GAP; }
                case CONCEPTUAL_FRAMEWORK, CONCEPTUAL_REVIEW, EMPIRICAL_REVIEW -> { return SectionSemanticPurpose.CONCEPTUAL_FRAMEWORK; }
                case THEORETICAL_REVIEW -> { return SectionSemanticPurpose.THEORETICAL_FRAMEWORK; }
                case RELATED_SYSTEMS -> { return SectionSemanticPurpose.RELATED_SYSTEMS; }
                case METHODOLOGY -> { return SectionSemanticPurpose.METHODOLOGY; }
                case SYSTEM_REQUIREMENTS -> { return SectionSemanticPurpose.SYSTEM_REQUIREMENTS; }
                case SYSTEM_DESIGN -> { return SectionSemanticPurpose.SYSTEM_DESIGN; }
                case IMPLEMENTATION -> { return SectionSemanticPurpose.IMPLEMENTATION; }
                case TESTING -> { return SectionSemanticPurpose.TESTING; }
                case RESULTS, FINDINGS -> { return SectionSemanticPurpose.FINDINGS; }
                case DISCUSSION -> { return SectionSemanticPurpose.DISCUSSION; }
                case CONCLUSIONS -> { return SectionSemanticPurpose.CONCLUSIONS; }
                case RECOMMENDATIONS, FUTURE_WORK -> { return SectionSemanticPurpose.RECOMMENDATIONS; }
                case REFERENCES -> { return SectionSemanticPurpose.REFERENCES; }
                case APPENDIX -> { return SectionSemanticPurpose.APPENDIX; }
                default -> {}
            }
        }
        String h = heading != null ? heading.trim().toLowerCase(java.util.Locale.ROOT) : "";
        ReportChapterType chType = chapter != null ? chapter.getType() : null;
        if (chType == ReportChapterType.PRELIMINARY) {
            if (h.contains("title page")) return SectionSemanticPurpose.TITLE_PAGE;
            if (h.contains("table of contents") || h.equals("contents")) return SectionSemanticPurpose.TABLE_OF_CONTENTS;
            if (h.contains("list of figures") || h.contains("figures")) return SectionSemanticPurpose.LIST_OF_FIGURES;
            if (h.contains("list of tables") || h.contains("tables")) return SectionSemanticPurpose.LIST_OF_TABLES;
            if (h.contains("declaration")) return SectionSemanticPurpose.DECLARATION;
            if (h.contains("certification") || h.contains("approval")) return SectionSemanticPurpose.CERTIFICATION;
            if (h.contains("dedication")) return SectionSemanticPurpose.DEDICATION;
            if (h.contains("acknowledgement") || h.contains("acknowledgment")) return SectionSemanticPurpose.ACKNOWLEDGEMENTS;
            if (h.contains("abstract")) return SectionSemanticPurpose.ABSTRACT;
        }
        if (chType == ReportChapterType.REFERENCES || h.equals("references") || h.equals("bibliography")) {
            return SectionSemanticPurpose.REFERENCES;
        }
        if (h.contains("literature review") || h.contains("related literature") || h.contains("previous studies")) {
            return SectionSemanticPurpose.LITERATURE_REVIEW;
        }
        if (h.contains("research gap") || h.equals("gap") || h.contains("gap analysis")) {
            return SectionSemanticPurpose.RESEARCH_GAP;
        }
        if (h.contains("conceptual framework") || h.contains("conceptual model")) {
            return SectionSemanticPurpose.CONCEPTUAL_FRAMEWORK;
        }
        if (h.contains("theoretical framework") || h.contains("theory") || h.contains("theoretical review")) {
            return SectionSemanticPurpose.THEORETICAL_FRAMEWORK;
        }
        if (h.contains("related systems") || h.contains("existing systems")) {
            return SectionSemanticPurpose.RELATED_SYSTEMS;
        }
        if (h.contains("requirement") || h.contains("design criteria") || h.contains("specification") || h.contains("use case")) {
            return SectionSemanticPurpose.SYSTEM_REQUIREMENTS;
        }
        if (h.contains("architecture") || h.contains("system design") || h.contains("solution design")
                || h.contains("process design") || h.contains("system, process, or solution design")
                || h.contains("database design") || h.contains("ui design")) {
            return SectionSemanticPurpose.SYSTEM_DESIGN;
        }
        if (h.contains("implementation") || h.contains("coding") || h.contains("development")) {
            return SectionSemanticPurpose.IMPLEMENTATION;
        }
        if (h.contains("testing") || h.contains("test plan") || h.contains("test case") || h.contains("evaluation")) {
            return SectionSemanticPurpose.TESTING;
        }
        if (h.contains("background")) return SectionSemanticPurpose.BACKGROUND;
        if (h.contains("problem")) return SectionSemanticPurpose.PROBLEM_STATEMENT;
        if (h.contains("objective") || h.contains("aim")) return SectionSemanticPurpose.OBJECTIVES;
        if (h.contains("research question")) return SectionSemanticPurpose.RESEARCH_QUESTIONS;
        if (h.contains("hypothes")) return SectionSemanticPurpose.HYPOTHESES;
        if (h.contains("significance")) return SectionSemanticPurpose.SIGNIFICANCE;
        if (h.contains("scope") || h.contains("delimitation")) return SectionSemanticPurpose.SCOPE;
        if (h.contains("methodology") || h.contains("method")) return SectionSemanticPurpose.METHODOLOGY;
        if (h.contains("finding") || h.contains("result")) return SectionSemanticPurpose.FINDINGS;
        if (h.contains("discussion")) return SectionSemanticPurpose.DISCUSSION;
        if (h.contains("conclusion")) return SectionSemanticPurpose.CONCLUSIONS;
        if (h.contains("recommendation")) return SectionSemanticPurpose.RECOMMENDATIONS;
        if (h.contains("appendix")) return SectionSemanticPurpose.APPENDIX;
        return SectionSemanticPurpose.CUSTOM;
    }

    public SectionGenerationPolicy resolveGenerationPolicy() {
        if (generationPolicy != null) {
            return generationPolicy;
        }
        SectionSemanticPurpose purpose = resolveSemanticPurpose();
        return switch (purpose) {
            case TITLE_PAGE, TABLE_OF_CONTENTS, LIST_OF_FIGURES, LIST_OF_TABLES, REFERENCES ->
                SectionGenerationPolicy.DETERMINISTIC;
            case DECLARATION, CERTIFICATION, DEDICATION, ACKNOWLEDGEMENTS ->
                SectionGenerationPolicy.USER_AUTHORED_FRONT_MATTER;
            case LITERATURE_REVIEW, RESEARCH_GAP, THEORETICAL_FRAMEWORK ->
                SectionGenerationPolicy.SOURCE_GROUNDED_AI;
            case TESTING, FINDINGS, DISCUSSION ->
                SectionGenerationPolicy.PROJECT_EVIDENCE_REQUIRED;
            case BACKGROUND, PROBLEM_STATEMENT, OBJECTIVES, RESEARCH_QUESTIONS, HYPOTHESES, SIGNIFICANCE, SCOPE,
                 ORGANIZATION_OF_STUDY,
                 CONCEPTUAL_FRAMEWORK, RELATED_SYSTEMS, METHODOLOGY, POPULATION_SAMPLING, DATA_COLLECTION_METHOD,
                 RESEARCH_INSTRUMENT, SYSTEM_REQUIREMENTS, SYSTEM_DESIGN, IMPLEMENTATION,
                 CONCLUSIONS, RECOMMENDATIONS, ABSTRACT ->
                SectionGenerationPolicy.PROJECT_DERIVED_AI;
            case APPENDIX ->
                SectionGenerationPolicy.USER_AUTHORED_FRONT_MATTER;
            case CUSTOM ->
                SectionGenerationPolicy.CONTEXTUAL_AI;
        };
    }

    @PrePersist void onCreate() {
        if (id == null) id = UUID.randomUUID();
        OffsetDateTime now = OffsetDateTime.now();
        if (createdAt == null) createdAt = now;
        updatedAt = now;
        if (type == null) type = ReportSectionType.CUSTOM;
        if (semanticPurpose == null) semanticPurpose = resolveSemanticPurpose();
        if (generationPolicy == null) generationPolicy = resolveGenerationPolicy();
        if (resolveGenerationPolicy() == SectionGenerationPolicy.DETERMINISTIC) {
            aiEnabled = false;
        }
        if (origin == null) origin = ContentOrigin.USER;
        if (status == null) status = ReportSectionStatus.NOT_STARTED;
        if (revisionNumber < 1) revisionNumber = 1;
        if (displayOrder < 1) displayOrder = 1;
    }
    @PreUpdate void onUpdate() {
        updatedAt = OffsetDateTime.now();
        if (semanticPurpose == null) semanticPurpose = resolveSemanticPurpose();
        if (generationPolicy == null) generationPolicy = resolveGenerationPolicy();
        if (resolveGenerationPolicy() == SectionGenerationPolicy.DETERMINISTIC) {
            aiEnabled = false;
        }
    }
}
