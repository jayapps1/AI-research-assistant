package com.researchassistant.framework.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "conceptual_relationships", indexes = @Index(name = "idx_conceptual_relationships_framework", columnList = "framework_id"))
@Getter @Setter @NoArgsConstructor
public class ConceptualRelationship {
    @Id @Column(nullable = false, updatable = false)
    private UUID id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "framework_id", nullable = false)
    private ConceptualFramework framework;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "source_variable_id", nullable = false)
    private ConceptualVariable sourceVariable;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "target_variable_id", nullable = false)
    private ConceptualVariable targetVariable;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 40)
    private ConceptualRelationshipType type = ConceptualRelationshipType.OTHER;
    @Column(length = 255)
    private String label;
    @Column(columnDefinition = "TEXT")
    private String rationale;
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;
    @PrePersist void onCreate() { if (id == null) id = UUID.randomUUID(); if (createdAt == null) createdAt = OffsetDateTime.now(); if (type == null) type = ConceptualRelationshipType.OTHER; }
}
