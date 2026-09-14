package com.researchassistant.instruments.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity @Table(name = "interview_guides")
@Getter @Setter @NoArgsConstructor
public class InterviewGuide {
    @Id @Column(nullable = false, updatable = false) private UUID id;
    @OneToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "instrument_id", nullable = false, unique = true) private ResearchInstrument instrument;
    @Column(name = "opening_script", columnDefinition = "TEXT") private String openingScript;
    @Column(name = "closing_script", columnDefinition = "TEXT") private String closingScript;
    @Column(name = "estimated_duration_minutes") private Integer estimatedDurationMinutes;
    @Enumerated(EnumType.STRING) @Column(name = "interview_type", nullable = false, length = 40) private InterviewType interviewType = InterviewType.OTHER;
    @Column(name = "interviewer_instructions", columnDefinition = "TEXT") private String interviewerInstructions;
    @Column(name = "created_at", nullable = false, updatable = false) private OffsetDateTime createdAt;
    @Column(name = "updated_at", nullable = false) private OffsetDateTime updatedAt;
    @PrePersist void onCreate() { if (id == null) id = UUID.randomUUID(); OffsetDateTime now = OffsetDateTime.now(); if (createdAt == null) createdAt = now; updatedAt = now; if (interviewType == null) interviewType = InterviewType.OTHER; }
    @PreUpdate void onUpdate() { updatedAt = OffsetDateTime.now(); }
}
