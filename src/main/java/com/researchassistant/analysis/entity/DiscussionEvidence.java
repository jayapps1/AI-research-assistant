package com.researchassistant.analysis.entity;

import com.researchassistant.document.entity.Document;
import com.researchassistant.document.entity.DocumentChunk;
import com.researchassistant.document.entity.DocumentPage;
import com.researchassistant.document.entity.DocumentVersion;
import com.researchassistant.rag.entity.RagQueryEvidence;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "discussion_evidence", indexes = @Index(name = "idx_discussion_evidence_discussion", columnList = "discussion_id"))
@Getter @Setter @NoArgsConstructor
public class DiscussionEvidence {
    @Id @Column(nullable = false, updatable = false)
    private UUID id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "discussion_id", nullable = false)
    private FindingDiscussion discussion;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "document_id", nullable = false)
    private Document document;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "document_version_id", nullable = false)
    private DocumentVersion documentVersion;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "page_id")
    private DocumentPage page;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "chunk_id")
    private DocumentChunk chunk;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "rag_evidence_id")
    private RagQueryEvidence ragEvidence;
    @Column(name = "evidence_snapshot", nullable = false, columnDefinition = "TEXT")
    private String evidenceSnapshot;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 60)
    private DiscussionEvidenceRelationship relationship = DiscussionEvidenceRelationship.OTHER;
    @Column(name = "citation_ordinal", nullable = false)
    private int citationOrdinal = 1;
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;
    @PrePersist void onCreate() { if (id == null) id = UUID.randomUUID(); if (createdAt == null) createdAt = OffsetDateTime.now(); if (relationship == null) relationship = DiscussionEvidenceRelationship.OTHER; if (citationOrdinal < 1) citationOrdinal = 1; }
}
