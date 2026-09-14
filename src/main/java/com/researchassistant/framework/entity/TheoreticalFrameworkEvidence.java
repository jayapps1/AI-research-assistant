package com.researchassistant.framework.entity;

import com.researchassistant.document.entity.Document;
import com.researchassistant.document.entity.DocumentChunk;
import com.researchassistant.document.entity.DocumentPage;
import com.researchassistant.document.entity.DocumentVersion;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "theoretical_framework_evidence", indexes = @Index(name = "idx_theory_evidence_theory", columnList = "theory_id"))
@Getter @Setter @NoArgsConstructor
public class TheoreticalFrameworkEvidence {
    @Id @Column(nullable = false, updatable = false)
    private UUID id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "theory_id", nullable = false)
    private TheoreticalFrameworkTheory theory;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "document_id", nullable = false)
    private Document document;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "document_version_id", nullable = false)
    private DocumentVersion documentVersion;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "page_id")
    private DocumentPage page;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "chunk_id")
    private DocumentChunk chunk;
    @Column(name = "evidence_text_snapshot", nullable = false, columnDefinition = "TEXT")
    private String evidenceTextSnapshot;
    @Enumerated(EnumType.STRING) @Column(name = "evidence_type", nullable = false, length = 40)
    private TheoreticalFrameworkEvidenceType evidenceType = TheoreticalFrameworkEvidenceType.OTHER;
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;
    @PrePersist void onCreate() { if (id == null) id = UUID.randomUUID(); if (createdAt == null) createdAt = OffsetDateTime.now(); if (evidenceType == null) evidenceType = TheoreticalFrameworkEvidenceType.OTHER; }
}
