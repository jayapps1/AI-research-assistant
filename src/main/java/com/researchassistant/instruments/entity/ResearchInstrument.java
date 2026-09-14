package com.researchassistant.instruments.entity;

import com.researchassistant.common.enums.ContentOrigin;
import com.researchassistant.identity.entity.User;
import com.researchassistant.methodology.entity.DataCollectionMethod;
import com.researchassistant.methodology.entity.Methodology;
import com.researchassistant.project.entity.ResearchProject;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "research_instruments", indexes = {
        @Index(name = "idx_research_instruments_project", columnList = "project_id"),
        @Index(name = "idx_research_instruments_methodology", columnList = "methodology_id"),
        @Index(name = "idx_research_instruments_method", columnList = "data_collection_method_id")
})
@Getter @Setter @NoArgsConstructor
public class ResearchInstrument {
    @Id @Column(nullable = false, updatable = false)
    private UUID id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "project_id", nullable = false)
    private ResearchProject project;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "methodology_id", nullable = false)
    private Methodology methodology;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "data_collection_method_id", nullable = false)
    private DataCollectionMethod dataCollectionMethod;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 50)
    private ResearchInstrumentType type;
    @Column(nullable = false, length = 255)
    private String title;
    @Column(columnDefinition = "TEXT")
    private String description;
    @Column(columnDefinition = "TEXT")
    private String instructions;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 40)
    private ResearchInstrumentStatus status = ResearchInstrumentStatus.DRAFT;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 30)
    private ContentOrigin origin = ContentOrigin.USER;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "created_by", nullable = false)
    private User createdBy;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "updated_by")
    private User updatedBy;
    @Column(name = "revision_number", nullable = false)
    private int revisionNumber = 1;
    @Version
    private Long version;
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;
    @PrePersist void onCreate() { if (id == null) id = UUID.randomUUID(); OffsetDateTime now = OffsetDateTime.now(); if (createdAt == null) createdAt = now; updatedAt = now; if (status == null) status = ResearchInstrumentStatus.DRAFT; if (origin == null) origin = ContentOrigin.USER; if (revisionNumber < 1) revisionNumber = 1; }
    @PreUpdate void onUpdate() { updatedAt = OffsetDateTime.now(); }
}
