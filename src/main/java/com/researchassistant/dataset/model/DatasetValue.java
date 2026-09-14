package com.researchassistant.dataset.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "dataset_values", indexes = @Index(name = "idx_dataset_values_record", columnList = "record_id"))
@Getter @Setter @NoArgsConstructor
public class DatasetValue {
    public enum MissingReason { NOT_PROVIDED, NOT_APPLICABLE, REFUSED, UNKNOWN, IMPORT_ERROR, OTHER }
    @Id @Column(nullable = false, updatable = false)
    private UUID id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "record_id", nullable = false)
    private DatasetRecord record;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "variable_id", nullable = false)
    private DatasetVariable variable;
    @Column(name = "string_value", length = 1000)
    private String stringValue;
    @Column(name = "integer_value")
    private Long integerValue;
    @Column(name = "decimal_value")
    private BigDecimal decimalValue;
    @Column(name = "boolean_value")
    private Boolean booleanValue;
    @Column(name = "date_value")
    private LocalDate dateValue;
    @Column(name = "date_time_value")
    private OffsetDateTime dateTimeValue;
    @Column(name = "text_value", columnDefinition = "TEXT")
    private String textValue;
    @Column(name = "category_code", length = 255)
    private String categoryCode;
    @Column(nullable = false)
    private boolean missing;
    @Enumerated(EnumType.STRING) @Column(name = "missing_reason", length = 40)
    private MissingReason missingReason;
    @PrePersist void onCreate() { if (id == null) id = UUID.randomUUID(); }
}
