package com.researchassistant.instruments.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity @Table(name = "observation_checklists")
@Getter @Setter @NoArgsConstructor
public class ObservationChecklist {
    @Id @Column(nullable = false, updatable = false) private UUID id;
    @OneToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "instrument_id", nullable = false, unique = true) private ResearchInstrument instrument;
    @Enumerated(EnumType.STRING) @Column(name = "observation_type", nullable = false, length = 40) private ObservationType observationType = ObservationType.OTHER;
    @Column(name = "observer_instructions", columnDefinition = "TEXT") private String observerInstructions;
    @Column(name = "observation_setting", columnDefinition = "TEXT") private String observationSetting;
    @Column(name = "estimated_duration_minutes") private Integer estimatedDurationMinutes;
    @Column(name = "created_at", nullable = false, updatable = false) private OffsetDateTime createdAt;
    @Column(name = "updated_at", nullable = false) private OffsetDateTime updatedAt;
    @PrePersist void onCreate() { if (id == null) id = UUID.randomUUID(); OffsetDateTime now = OffsetDateTime.now(); if (createdAt == null) createdAt = now; updatedAt = now; if (observationType == null) observationType = ObservationType.OTHER; }
    @PreUpdate void onUpdate() { updatedAt = OffsetDateTime.now(); }
}
