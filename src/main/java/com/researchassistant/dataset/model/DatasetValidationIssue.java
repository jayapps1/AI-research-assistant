package com.researchassistant.dataset.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "dataset_validation_issues", indexes = @Index(name = "idx_dataset_validation_issues_dataset", columnList = "dataset_id"))
@Getter @Setter @NoArgsConstructor
public class DatasetValidationIssue {
    public enum Severity { INFO, WARNING, ERROR }
    @Id @Column(nullable = false, updatable = false)
    private UUID id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "dataset_id", nullable = false)
    private ResearchDataset dataset;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "import_job_id")
    private DatasetImportJob importJob;
    @Column(name = "row_number")
    private Long rowNumber;
    @Column(name = "variable_id")
    private UUID variableId;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20)
    private Severity severity;
    @Column(nullable = false, length = 100)
    private String code;
    @Column(nullable = false, columnDefinition = "TEXT")
    private String message;
    @Column(name = "rejected_value_snapshot", columnDefinition = "TEXT")
    private String rejectedValueSnapshot;
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;
    @PrePersist void onCreate() { if (id == null) id = UUID.randomUUID(); if (createdAt == null) createdAt = OffsetDateTime.now(); }
}
