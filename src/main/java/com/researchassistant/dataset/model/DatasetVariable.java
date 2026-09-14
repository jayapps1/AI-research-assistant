package com.researchassistant.dataset.model;

import com.researchassistant.framework.entity.ConceptualVariable;
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
@Table(name = "dataset_variables", indexes = @Index(name = "idx_dataset_variables_dataset", columnList = "dataset_id"))
@Getter @Setter @NoArgsConstructor
public class DatasetVariable {
    public enum VariableType { STRING, INTEGER, DECIMAL, BOOLEAN, DATE, DATETIME, CATEGORY, ORDINAL, TEXT }
    public enum MeasurementLevel { NOMINAL, ORDINAL, INTERVAL, RATIO, TEXT, UNKNOWN }
    @Id @Column(nullable = false, updatable = false)
    private UUID id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "dataset_id", nullable = false)
    private ResearchDataset dataset;
    @Column(name = "variable_name", nullable = false, length = 120)
    private String variableName;
    @Column(nullable = false, length = 255)
    private String label;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 30)
    private VariableType type;
    @Enumerated(EnumType.STRING) @Column(name = "measurement_level", nullable = false, length = 30)
    private MeasurementLevel measurementLevel = MeasurementLevel.UNKNOWN;
    @Column(nullable = false)
    private boolean nullable = true;
    @Column(length = 80)
    private String unit;
    @Column(name = "missing_value_code", length = 80)
    private String missingValueCode;
    @Column(name = "display_order", nullable = false)
    private int displayOrder = 1;
    @Column(name = "source_instrument_item_id")
    private UUID sourceInstrumentItemId;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "objective_id")
    private ResearchObjective objective;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "research_question_id")
    private ResearchQuestion researchQuestion;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "hypothesis_id")
    private ResearchHypothesis hypothesis;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "conceptual_variable_id")
    private ConceptualVariable conceptualVariable;
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;
    @PrePersist void onCreate() { if (id == null) id = UUID.randomUUID(); OffsetDateTime now = OffsetDateTime.now(); if (createdAt == null) createdAt = now; updatedAt = now; if (displayOrder < 1) displayOrder = 1; if (measurementLevel == null) measurementLevel = MeasurementLevel.UNKNOWN; }
    @PreUpdate void onUpdate() { updatedAt = OffsetDateTime.now(); }
}
