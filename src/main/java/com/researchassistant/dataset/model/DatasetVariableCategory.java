package com.researchassistant.dataset.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "dataset_variable_categories")
@Getter @Setter @NoArgsConstructor
public class DatasetVariableCategory {
    @Id @Column(nullable = false, updatable = false)
    private UUID id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "variable_id", nullable = false)
    private DatasetVariable variable;
    @Column(nullable = false, length = 255)
    private String code;
    @Column(nullable = false, length = 500)
    private String label;
    @Column(name = "numeric_value")
    private BigDecimal numericValue;
    @Column(name = "display_order", nullable = false)
    private int displayOrder = 1;
    @PrePersist void onCreate() { if (id == null) id = UUID.randomUUID(); if (displayOrder < 1) displayOrder = 1; }
}
