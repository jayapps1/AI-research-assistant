package com.researchassistant.ethics.model;

import com.researchassistant.common.enums.ContentOrigin;
import com.researchassistant.identity.entity.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "consent_form_revisions", indexes = @Index(name = "idx_consent_revisions_form", columnList = "consent_form_id"))
@Getter @Setter @NoArgsConstructor
public class ConsentFormRevision {
    @Id @Column(nullable = false, updatable = false)
    private UUID id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "consent_form_id", nullable = false)
    private ConsentForm consentForm;
    @Column(name = "revision_number", nullable = false)
    private int revisionNumber = 1;
    @Column(nullable = false, columnDefinition = "TEXT")
    private String introduction;
    @Column(name = "study_purpose", nullable = false, columnDefinition = "TEXT")
    private String studyPurpose;
    @Column(nullable = false, columnDefinition = "TEXT")
    private String procedures;
    @Column(nullable = false, columnDefinition = "TEXT")
    private String risks;
    @Column(nullable = false, columnDefinition = "TEXT")
    private String benefits;
    @Column(nullable = false, columnDefinition = "TEXT")
    private String confidentiality;
    @Column(name = "voluntary_participation", nullable = false, columnDefinition = "TEXT")
    private String voluntaryParticipation;
    @Column(name = "withdrawal_rights", nullable = false, columnDefinition = "TEXT")
    private String withdrawalRights;
    @Column(name = "contact_information", columnDefinition = "TEXT")
    private String contactInformation;
    @Column(name = "data_usage_statement", columnDefinition = "TEXT")
    private String dataUsageStatement;
    @Column(name = "data_retention_statement", columnDefinition = "TEXT")
    private String dataRetentionStatement;
    @Column(name = "consent_statement", nullable = false, columnDefinition = "TEXT")
    private String consentStatement;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 30)
    private ContentOrigin origin = ContentOrigin.USER;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "created_by", nullable = false)
    private User createdBy;
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;
    @PrePersist void onCreate() { if (id == null) id = UUID.randomUUID(); if (createdAt == null) createdAt = OffsetDateTime.now(); if (revisionNumber < 1) revisionNumber = 1; if (origin == null) origin = ContentOrigin.USER; }
}
