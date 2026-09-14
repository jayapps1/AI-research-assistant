package com.researchassistant.methodology.entity;

import com.researchassistant.common.enums.ContentOrigin;
import com.researchassistant.identity.entity.User;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "sample_size_calculations", indexes = @Index(name = "idx_sample_size_sampling_plan", columnList = "sampling_plan_id"))
@Getter @Setter @NoArgsConstructor
public class SampleSizeCalculation {
    @Id @Column(nullable = false, updatable = false)
    private UUID id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "sampling_plan_id", nullable = false)
    private SamplingPlan samplingPlan;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 50)
    private SampleSizeMethod method;
    @Column(name = "population_size")
    private Long populationSize;
    @Column(name = "confidence_level")
    private Double confidenceLevel;
    @Column(name = "margin_of_error")
    private Double marginOfError;
    @Column(name = "estimated_proportion")
    private Double estimatedProportion;
    @Column(name = "design_effect")
    private Double designEffect;
    @Column(name = "expected_response_rate")
    private Double expectedResponseRate;
    @Column(name = "initial_sample_size")
    private Integer initialSampleSize;
    @Column(name = "adjusted_sample_size", nullable = false)
    private Integer adjustedSampleSize;
    @Column(name = "formula_description", nullable = false, columnDefinition = "TEXT")
    private String formulaDescription;
    @Column(nullable = false, columnDefinition = "TEXT")
    private String assumptions;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 30)
    private ContentOrigin origin = ContentOrigin.USER;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "created_by", nullable = false)
    private User createdBy;
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;
    @PrePersist void onCreate() { if (id == null) id = UUID.randomUUID(); if (createdAt == null) createdAt = OffsetDateTime.now(); if (origin == null) origin = ContentOrigin.USER; }
}
