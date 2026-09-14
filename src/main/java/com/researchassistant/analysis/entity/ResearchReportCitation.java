package com.researchassistant.analysis.entity;

import com.researchassistant.document.entity.Document;
import com.researchassistant.document.entity.DocumentChunk;
import com.researchassistant.document.entity.DocumentPage;
import com.researchassistant.document.entity.DocumentVersion;
import com.researchassistant.reference.entity.ProjectReference;
import com.researchassistant.reference.entity.ReferenceEntry;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "research_report_citations")
@Getter @Setter @NoArgsConstructor
public class ResearchReportCitation {
    @Id @Column(nullable = false, updatable = false)
    private UUID id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "section_id", nullable = false)
    private ResearchReportSection section;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "document_id", nullable = false)
    private Document document;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "document_version_id", nullable = false)
    private DocumentVersion documentVersion;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "page_id")
    private DocumentPage page;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "chunk_id")
    private DocumentChunk chunk;
    @Column(name = "document_code", nullable = false, length = 32)
    private String documentCode;
    @Column(name = "citation_ordinal", nullable = false)
    private int citationOrdinal = 1;
    @Column(name = "supporting_text_snapshot", nullable = false, columnDefinition = "TEXT")
    private String supportingTextSnapshot;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "reference_id")
    private ReferenceEntry reference;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "project_reference_id")
    private ProjectReference projectReference;
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;
    @PrePersist void onCreate() { if (id == null) id = UUID.randomUUID(); if (createdAt == null) createdAt = OffsetDateTime.now(); if (citationOrdinal < 1) citationOrdinal = 1; }
}
