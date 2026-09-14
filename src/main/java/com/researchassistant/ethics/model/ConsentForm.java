package com.researchassistant.ethics.model;

import com.researchassistant.identity.entity.User;
import com.researchassistant.project.entity.ResearchProject;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "consent_forms", indexes = @Index(name = "idx_consent_forms_project", columnList = "project_id"))
@Getter @Setter @NoArgsConstructor
public class ConsentForm {
    public enum Status { DRAFT, ACTIVE, SUPERSEDED, ARCHIVED }
    @Id @Column(nullable = false, updatable = false)
    private UUID id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "project_id", nullable = false)
    private ResearchProject project;
    @Column(nullable = false, length = 255)
    private String title;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 30)
    private Status status = Status.DRAFT;
    @Column(name = "language_code", nullable = false, length = 20)
    private String languageCode = "en";
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "created_by", nullable = false)
    private User createdBy;
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;
    @PrePersist void onCreate() { if (id == null) id = UUID.randomUUID(); OffsetDateTime now = OffsetDateTime.now(); if (createdAt == null) createdAt = now; updatedAt = now; if (status == null) status = Status.DRAFT; if (languageCode == null || languageCode.isBlank()) languageCode = "en"; }
    @PreUpdate void onUpdate() { updatedAt = OffsetDateTime.now(); }
}
