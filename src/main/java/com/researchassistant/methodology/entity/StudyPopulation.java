package com.researchassistant.methodology.entity;

import com.researchassistant.common.enums.ContentOrigin;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "study_populations", indexes = @Index(name = "idx_study_populations_methodology", columnList = "methodology_id"))
@Getter @Setter @NoArgsConstructor
public class StudyPopulation {
    @Id @Column(nullable = false, updatable = false)
    private UUID id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "methodology_id", nullable = false)
    private Methodology methodology;
    @Column(name = "target_population_description", nullable = false, columnDefinition = "TEXT")
    private String targetPopulationDescription;
    @Column(name = "target_population_size")
    private Long targetPopulationSize;
    @Column(name = "accessible_population_description", columnDefinition = "TEXT")
    private String accessiblePopulationDescription;
    @Column(name = "accessible_population_size")
    private Long accessiblePopulationSize;
    @Column(name = "inclusion_criteria", columnDefinition = "TEXT")
    private String inclusionCriteria;
    @Column(name = "exclusion_criteria", columnDefinition = "TEXT")
    private String exclusionCriteria;
    @Column(name = "geographic_scope", columnDefinition = "TEXT")
    private String geographicScope;
    @Column(name = "demographic_characteristics", columnDefinition = "TEXT")
    private String demographicCharacteristics;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 30)
    private ContentOrigin origin = ContentOrigin.USER;
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;
    @PrePersist void onCreate() { if (id == null) id = UUID.randomUUID(); OffsetDateTime now = OffsetDateTime.now(); if (createdAt == null) createdAt = now; updatedAt = now; if (origin == null) origin = ContentOrigin.USER; }
    @PreUpdate void onUpdate() { updatedAt = OffsetDateTime.now(); }
}
