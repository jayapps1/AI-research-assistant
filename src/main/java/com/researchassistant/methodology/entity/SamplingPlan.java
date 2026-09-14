package com.researchassistant.methodology.entity;

import com.researchassistant.common.enums.ContentOrigin;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "sampling_plans", indexes = @Index(name = "idx_sampling_plans_methodology", columnList = "methodology_id"))
@Getter @Setter @NoArgsConstructor
public class SamplingPlan {
    @Id @Column(nullable = false, updatable = false)
    private UUID id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "methodology_id", nullable = false)
    private Methodology methodology;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "population_id")
    private StudyPopulation population;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 40)
    private SamplingApproach approach = SamplingApproach.OTHER;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 40)
    private SamplingTechnique technique = SamplingTechnique.OTHER;
    @Column(name = "planned_sample_size")
    private Integer plannedSampleSize;
    @Column(columnDefinition = "TEXT")
    private String rationale;
    @Column(name = "sampling_frame", columnDefinition = "TEXT")
    private String samplingFrame;
    @Column(name = "recruitment_strategy", columnDefinition = "TEXT")
    private String recruitmentStrategy;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 30)
    private ContentOrigin origin = ContentOrigin.USER;
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;
    @PrePersist void onCreate() { if (id == null) id = UUID.randomUUID(); OffsetDateTime now = OffsetDateTime.now(); if (createdAt == null) createdAt = now; updatedAt = now; if (origin == null) origin = ContentOrigin.USER; if (approach == null) approach = SamplingApproach.OTHER; if (technique == null) technique = SamplingTechnique.OTHER; }
    @PreUpdate void onUpdate() { updatedAt = OffsetDateTime.now(); }
}
