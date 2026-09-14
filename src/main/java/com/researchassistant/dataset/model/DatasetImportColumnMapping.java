package com.researchassistant.dataset.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "dataset_import_column_mappings")
@Getter @Setter @NoArgsConstructor
public class DatasetImportColumnMapping {
    @Id @Column(nullable = false, updatable = false)
    private UUID id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "job_id", nullable = false)
    private DatasetImportJob job;
    @Column(name = "source_column", nullable = false, length = 255)
    private String sourceColumn;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "variable_id")
    private DatasetVariable variable;
    @Column(name = "proposed_variable_name", length = 120)
    private String proposedVariableName;
    @Enumerated(EnumType.STRING) @Column(name = "target_type", nullable = false, length = 30)
    private DatasetVariable.VariableType targetType;
    @Enumerated(EnumType.STRING) @Column(name = "measurement_level", nullable = false, length = 30)
    private DatasetVariable.MeasurementLevel measurementLevel = DatasetVariable.MeasurementLevel.UNKNOWN;
    @Column(nullable = false)
    private boolean ignored;
    @Column(columnDefinition = "TEXT")
    private String transformation;
    @PrePersist void onCreate() { if (id == null) id = UUID.randomUUID(); if (measurementLevel == null) measurementLevel = DatasetVariable.MeasurementLevel.UNKNOWN; }
}
