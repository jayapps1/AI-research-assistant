package com.researchassistant.instruments.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity @Table(name = "focus_group_guides")
@Getter @Setter @NoArgsConstructor
public class FocusGroupGuide {
    @Id @Column(nullable = false, updatable = false) private UUID id;
    @OneToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "instrument_id", nullable = false, unique = true) private ResearchInstrument instrument;
    @Column(name = "facilitator_instructions", columnDefinition = "TEXT") private String facilitatorInstructions;
    @Column(name = "opening_script", columnDefinition = "TEXT") private String openingScript;
    @Column(name = "closing_script", columnDefinition = "TEXT") private String closingScript;
    @Column(name = "planned_duration_minutes") private Integer plannedDurationMinutes;
    @Column(name = "recommended_min_participants") private Integer recommendedMinParticipants;
    @Column(name = "recommended_max_participants") private Integer recommendedMaxParticipants;
    @Column(name = "group_composition_guidance", columnDefinition = "TEXT") private String groupCompositionGuidance;
    @Column(name = "created_at", nullable = false, updatable = false) private OffsetDateTime createdAt;
    @Column(name = "updated_at", nullable = false) private OffsetDateTime updatedAt;
    @PrePersist void onCreate() { if (id == null) id = UUID.randomUUID(); OffsetDateTime now = OffsetDateTime.now(); if (createdAt == null) createdAt = now; updatedAt = now; }
    @PreUpdate void onUpdate() { updatedAt = OffsetDateTime.now(); }
}
