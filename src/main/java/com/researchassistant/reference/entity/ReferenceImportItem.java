package com.researchassistant.reference.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "reference_import_items")
@Getter @Setter @NoArgsConstructor
public class ReferenceImportItem {
    @Id @Column(nullable = false, updatable = false) private UUID id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "import_job_id", nullable = false) private ReferenceImportJob importJob;
    @Column(name = "item_ordinal", nullable = false) private int itemOrdinal = 1;
    @Column(name = "parsed_json", nullable = false, columnDefinition = "TEXT") private String parsedJson;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 40) private ReferenceImportClassification classification = ReferenceImportClassification.NEW;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "duplicate_reference_id") private ReferenceEntry duplicateReference;
    @Column(name = "metadata_warnings", columnDefinition = "TEXT") private String metadataWarnings;
    @Column(name = "created_at", nullable = false, updatable = false) private OffsetDateTime createdAt;
    @PrePersist void onCreate() { if (id == null) id = UUID.randomUUID(); if (createdAt == null) createdAt = OffsetDateTime.now(); if (itemOrdinal < 1) itemOrdinal = 1; if (classification == null) classification = ReferenceImportClassification.NEW; }
}
