package com.researchassistant.framework.entity;

import com.researchassistant.common.enums.ContentOrigin;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "theoretical_framework_theories", indexes = @Index(name = "idx_theories_framework", columnList = "framework_id"))
@Getter @Setter @NoArgsConstructor
public class TheoreticalFrameworkTheory {
    @Id @Column(nullable = false, updatable = false)
    private UUID id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "framework_id", nullable = false)
    private TheoreticalFramework framework;
    @Column(name = "theory_name", nullable = false, length = 255)
    private String theoryName;
    @Column(length = 255)
    private String theorist;
    @Column(name = "original_year")
    private Integer originalYear;
    @Column(columnDefinition = "TEXT")
    private String description;
    @Column(name = "key_constructs", columnDefinition = "TEXT")
    private String keyConstructs;
    @Column(name = "relevance_to_study", columnDefinition = "TEXT")
    private String relevanceToStudy;
    @Column(columnDefinition = "TEXT")
    private String limitations;
    @Column(name = "display_order", nullable = false)
    private int displayOrder = 1;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 30)
    private ContentOrigin origin = ContentOrigin.USER;
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;
    @PrePersist void onCreate() { if (id == null) id = UUID.randomUUID(); OffsetDateTime now = OffsetDateTime.now(); if (createdAt == null) createdAt = now; updatedAt = now; if (origin == null) origin = ContentOrigin.USER; if (displayOrder < 1) displayOrder = 1; }
    @PreUpdate void onUpdate() { updatedAt = OffsetDateTime.now(); }
}
