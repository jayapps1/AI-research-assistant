package com.researchassistant.instruments.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "questionnaires")
@Getter @Setter @NoArgsConstructor
public class Questionnaire {
    @Id @Column(nullable = false, updatable = false)
    private UUID id;
    @OneToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "instrument_id", nullable = false, unique = true)
    private ResearchInstrument instrument;
    @Column(columnDefinition = "TEXT")
    private String introduction;
    @Column(nullable = false)
    private boolean anonymous;
    @Column(name = "estimated_completion_minutes")
    private Integer estimatedCompletionMinutes;
    @Column(name = "target_respondent_description", columnDefinition = "TEXT")
    private String targetRespondentDescription;
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;
    @PrePersist void onCreate() { if (id == null) id = UUID.randomUUID(); OffsetDateTime now = OffsetDateTime.now(); if (createdAt == null) createdAt = now; updatedAt = now; }
    @PreUpdate void onUpdate() { updatedAt = OffsetDateTime.now(); }
}
