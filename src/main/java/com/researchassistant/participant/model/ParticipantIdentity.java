package com.researchassistant.participant.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "participant_identities")
@Getter @Setter @NoArgsConstructor
public class ParticipantIdentity {
    @Id @Column(nullable = false, updatable = false)
    private UUID id;
    @OneToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "participant_id", nullable = false)
    private Participant participant;
    @Column(name = "encrypted_full_name", columnDefinition = "TEXT")
    private String encryptedFullName;
    @Column(name = "encrypted_email", columnDefinition = "TEXT")
    private String encryptedEmail;
    @Column(name = "encrypted_phone", columnDefinition = "TEXT")
    private String encryptedPhone;
    @Column(name = "encrypted_external_identifier", columnDefinition = "TEXT")
    private String encryptedExternalIdentifier;
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;
    @PrePersist void onCreate() { if (id == null) id = UUID.randomUUID(); OffsetDateTime now = OffsetDateTime.now(); if (createdAt == null) createdAt = now; updatedAt = now; }
    @PreUpdate void onUpdate() { updatedAt = OffsetDateTime.now(); }
}
