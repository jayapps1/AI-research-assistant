package com.researchassistant.ethics.model;

import com.researchassistant.identity.entity.User;
import com.researchassistant.participant.model.Participant;
import com.researchassistant.project.entity.ResearchProject;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "participant_consents", indexes = @Index(name = "idx_participant_consents_participant", columnList = "participant_id"))
@Getter @Setter @NoArgsConstructor
public class ParticipantConsent {
    public enum Decision { CONSENTED, DECLINED, WITHDRAWN }
    public enum Method { WRITTEN, ELECTRONIC, VERBAL, WITNESSED_VERBAL, OTHER }
    @Id @Column(nullable = false, updatable = false)
    private UUID id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "project_id", nullable = false)
    private ResearchProject project;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "participant_id", nullable = false)
    private Participant participant;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "consent_revision_id", nullable = false)
    private ConsentFormRevision consentRevision;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 30)
    private Decision decision;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 30)
    private Method method = Method.ELECTRONIC;
    @Column(name = "consented_at", nullable = false)
    private OffsetDateTime consentedAt;
    @Column(name = "withdrawn_at")
    private OffsetDateTime withdrawnAt;
    @Column(name = "witness_code", length = 100)
    private String witnessCode;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "recorded_by")
    private User recordedBy;
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;
    @PrePersist void onCreate() { if (id == null) id = UUID.randomUUID(); OffsetDateTime now = OffsetDateTime.now(); if (consentedAt == null) consentedAt = now; if (createdAt == null) createdAt = now; if (method == null) method = Method.ELECTRONIC; }
}
