package com.researchassistant.participant.model;

import com.researchassistant.identity.entity.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "participant_eligibility_assessments")
@Getter @Setter @NoArgsConstructor
public class ParticipantEligibilityAssessment {
    @Id @Column(nullable = false, updatable = false)
    private UUID id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "participant_id", nullable = false)
    private Participant participant;
    @Column(nullable = false)
    private boolean eligible;
    @Column(name = "reason_code", length = 100)
    private String reasonCode;
    @Column(columnDefinition = "TEXT")
    private String notes;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "assessed_by", nullable = false)
    private User assessedBy;
    @Column(name = "assessed_at", nullable = false)
    private OffsetDateTime assessedAt;
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;
    @PrePersist void onCreate() { if (id == null) id = UUID.randomUUID(); OffsetDateTime now = OffsetDateTime.now(); if (assessedAt == null) assessedAt = now; if (createdAt == null) createdAt = now; }
}
