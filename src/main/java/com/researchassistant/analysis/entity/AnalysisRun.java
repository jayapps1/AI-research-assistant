package com.researchassistant.analysis.entity;

import com.researchassistant.dataset.model.ResearchDataset;
import com.researchassistant.identity.entity.User;
import com.researchassistant.project.entity.ResearchProject;
import com.researchassistant.researchdesign.entity.ResearchHypothesis;
import com.researchassistant.researchdesign.entity.ResearchObjective;
import com.researchassistant.researchdesign.entity.ResearchQuestion;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "analysis_runs", indexes = {
        @Index(name = "idx_analysis_runs_project", columnList = "project_id"),
        @Index(name = "idx_analysis_runs_status", columnList = "status")
})
@Getter @Setter @NoArgsConstructor
public class AnalysisRun {
    public enum QualitativeSourceType { DOCUMENT, LITERATURE_REVIEW, LITERATURE_MATRIX, FIELD_NOTES, TRANSCRIPT, OTHER }
    public enum AnalysisType { DATASET_PROFILE, DESCRIPTIVE_SUMMARY, QUALITATIVE_CODING, DOCUMENT_SYNTHESIS, MIXED_METHODS_SYNTHESIS, CUSTOM }
    public enum Status { PLANNED, RUNNING, COMPLETED, FAILED, CANCELLED }

    @Id @Column(nullable = false, updatable = false)
    private UUID id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "project_id", nullable = false)
    private ResearchProject project;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "objective_id")
    private ResearchObjective objective;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "question_id")
    private ResearchQuestion question;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "hypothesis_id")
    private ResearchHypothesis hypothesis;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "dataset_id")
    private ResearchDataset dataset;
    @Enumerated(EnumType.STRING) @Column(name = "qualitative_source_type", length = 40)
    private QualitativeSourceType qualitativeSourceType;
    @Column(name = "qualitative_source_reference")
    private UUID qualitativeSourceReference;
    @Column(nullable = false, length = 255)
    private String title;
    @Enumerated(EnumType.STRING) @Column(name = "analysis_type", nullable = false, length = 50)
    private AnalysisType analysisType;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 40)
    private Status status = Status.PLANNED;
    @Column(name = "method_description", columnDefinition = "TEXT")
    private String methodDescription;
    @Column(name = "parameters_json", columnDefinition = "TEXT")
    private String parametersJson;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "initiated_by", nullable = false)
    private User initiatedBy;
    @Column(name = "started_at", nullable = false)
    private OffsetDateTime startedAt;
    @Column(name = "completed_at")
    private OffsetDateTime completedAt;
    @Column(name = "error_code", length = 100)
    private String errorCode;
    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @PrePersist void onCreate() {
        if (id == null) id = UUID.randomUUID();
        OffsetDateTime now = OffsetDateTime.now();
        if (startedAt == null) startedAt = now;
        if (createdAt == null) createdAt = now;
        updatedAt = now;
        if (status == null) status = Status.PLANNED;
    }
    @PreUpdate void onUpdate() { updatedAt = OffsetDateTime.now(); }
}
