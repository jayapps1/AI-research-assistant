package com.researchassistant.rag.entity;

import com.researchassistant.document.entity.DocumentChunk;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(
        name = "rag_query_evidence",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_rag_query_evidence_ordinal", columnNames = {"query_id", "evidence_ordinal"}),
                @UniqueConstraint(name = "uk_rag_query_evidence_chunk", columnNames = {"query_id", "chunk_id"})
        },
        indexes = {
                @Index(name = "idx_rag_query_evidence_query_id", columnList = "query_id"),
                @Index(name = "idx_rag_query_evidence_document_id", columnList = "document_id")
        }
)
@Getter
@Setter
@NoArgsConstructor
public class RagQueryEvidence {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "query_id", nullable = false)
    private RagQuery query;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "chunk_id", nullable = false)
    private DocumentChunk chunk;

    @Column(name = "document_id", nullable = false)
    private UUID documentId;

    @Column(name = "document_code", nullable = false, length = 32)
    private String documentCode;

    @Column(name = "document_title", nullable = false, length = 500)
    private String documentTitle;

    @Column(name = "document_version_id", nullable = false)
    private UUID documentVersionId;

    @Column(name = "version_number", nullable = false)
    private int versionNumber;

    @Column(name = "page_number", nullable = false)
    private int pageNumber;

    @Column(name = "chunk_number", nullable = false)
    private int chunkNumber;

    @Column(name = "evidence_ordinal", nullable = false)
    private int evidenceOrdinal;

    @Column(name = "text_snapshot", nullable = false, columnDefinition = "TEXT")
    private String textSnapshot;

    @Column(name = "lexical_score")
    private Double lexicalScore;

    @Column(name = "semantic_score")
    private Double semanticScore;

    @Column(name = "fused_score")
    private Double fusedScore;

    @Column(name = "rerank_score")
    private Double rerankScore;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        if (createdAt == null) {
            createdAt = OffsetDateTime.now();
        }
    }
}
