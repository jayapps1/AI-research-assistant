package com.researchassistant.framework.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "conceptual_variables", indexes = @Index(name = "idx_conceptual_variables_framework", columnList = "framework_id"))
@Getter @Setter @NoArgsConstructor
public class ConceptualVariable {
    @Id @Column(nullable = false, updatable = false)
    private UUID id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "framework_id", nullable = false)
    private ConceptualFramework framework;
    @Column(nullable = false, length = 255)
    private String name;
    @Column(columnDefinition = "TEXT")
    private String description;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 40)
    private ConceptualVariableType type = ConceptualVariableType.OTHER;
    @Column(name = "operational_definition", columnDefinition = "TEXT")
    private String operationalDefinition;
    @Column(name = "display_order", nullable = false)
    private int displayOrder = 1;
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;
    @PrePersist void onCreate() { if (id == null) id = UUID.randomUUID(); OffsetDateTime now = OffsetDateTime.now(); if (createdAt == null) createdAt = now; updatedAt = now; if (type == null) type = ConceptualVariableType.OTHER; if (displayOrder < 1) displayOrder = 1; }
    @PreUpdate void onUpdate() { updatedAt = OffsetDateTime.now(); }
}
