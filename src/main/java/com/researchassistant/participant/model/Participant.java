package com.researchassistant.participant.model;

import com.researchassistant.identity.entity.User;
import com.researchassistant.methodology.entity.SamplingPlan;
import com.researchassistant.methodology.entity.StudyPopulation;
import com.researchassistant.project.entity.ResearchProject;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "participants", indexes = @Index(name = "idx_participants_project", columnList = "project_id"))
@Getter @Setter @NoArgsConstructor
public class Participant {
    public enum Status { SCREENED, ELIGIBLE, ENROLLED, COMPLETED, WITHDRAWN, EXCLUDED, LOST_TO_FOLLOW_UP }
    @Id @Column(nullable = false, updatable = false)
    private UUID id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "project_id", nullable = false)
    private ResearchProject project;
    @Column(name = "participant_code", nullable = false, length = 40)
    private String participantCode;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 40)
    private Status status = Status.ENROLLED;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "population_id")
    private StudyPopulation population;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "sampling_plan_id")
    private SamplingPlan samplingPlan;
    @Column(name = "enrolled_at", nullable = false)
    private OffsetDateTime enrolledAt;
    @Column(name = "withdrawn_at")
    private OffsetDateTime withdrawnAt;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "enrolled_by", nullable = false)
    private User enrolledBy;
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;
    @PrePersist void onCreate() { if (id == null) id = UUID.randomUUID(); OffsetDateTime now = OffsetDateTime.now(); if (createdAt == null) createdAt = now; updatedAt = now; if (enrolledAt == null) enrolledAt = now; if (status == null) status = Status.ENROLLED; }
    @PreUpdate void onUpdate() { updatedAt = OffsetDateTime.now(); }
}
