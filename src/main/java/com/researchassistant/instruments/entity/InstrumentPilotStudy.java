package com.researchassistant.instruments.entity;

import com.researchassistant.identity.entity.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity @Table(name = "instrument_pilot_studies")
@Getter @Setter @NoArgsConstructor
public class InstrumentPilotStudy {
    @Id @Column(nullable = false, updatable = false) private UUID id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "instrument_id", nullable = false) private ResearchInstrument instrument;
    @Column(nullable = false, length = 255) private String title;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 30) private PilotStudyStatus status = PilotStudyStatus.PLANNED;
    @Column(name = "planned_participant_count") private Integer plannedParticipantCount;
    @Column(name = "actual_participant_count") private Integer actualParticipantCount;
    @Column(name = "participant_description", columnDefinition = "TEXT") private String participantDescription;
    @Column private String location;
    @Column(name = "start_date") private LocalDate startDate;
    @Column(name = "end_date") private LocalDate endDate;
    @Column(columnDefinition = "TEXT") private String procedure;
    @Column(columnDefinition = "TEXT") private String observations;
    @Column(name = "issues_identified", columnDefinition = "TEXT") private String issuesIdentified;
    @Column(name = "modifications_recommended", columnDefinition = "TEXT") private String modificationsRecommended;
    @Column(name = "modifications_implemented", columnDefinition = "TEXT") private String modificationsImplemented;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "created_by", nullable = false) private User createdBy;
    @Column(name = "created_at", nullable = false, updatable = false) private OffsetDateTime createdAt;
    @Column(name = "updated_at", nullable = false) private OffsetDateTime updatedAt;
    @PrePersist void onCreate() { if (id == null) id = UUID.randomUUID(); OffsetDateTime now = OffsetDateTime.now(); if (createdAt == null) createdAt = now; updatedAt = now; if (status == null) status = PilotStudyStatus.PLANNED; }
    @PreUpdate void onUpdate() { updatedAt = OffsetDateTime.now(); }
}
