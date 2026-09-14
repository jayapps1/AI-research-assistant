package com.researchassistant.document.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
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
        name = "document_chunk_embeddings",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_document_chunk_embeddings_chunk_provider_model",
                columnNames = {"chunk_id", "provider", "model"}
        ),
        indexes = {
                @Index(name = "idx_document_chunk_embeddings_chunk_id", columnList = "chunk_id"),
                @Index(name = "idx_document_chunk_embeddings_provider_model_status", columnList = "provider,model,status")
        }
)
@Getter
@Setter
@NoArgsConstructor
public class DocumentChunkEmbedding {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "chunk_id", nullable = false)
    private DocumentChunk chunk;

    @Column(name = "provider", nullable = false, length = 100)
    private String provider;

    @Column(name = "model", nullable = false, length = 200)
    private String model;

    @Column(name = "dimensions", nullable = false)
    private int dimensions;

    @Column(name = "chunk_checksum_sha256", nullable = false, length = 64)
    private String chunkChecksumSha256;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 40)
    private EmbeddingStatus status;

    @Column(name = "vector_values", columnDefinition = "double precision[]")
    private double[] vectorValues;

    @Column(name = "failure_code", length = 100)
    private String failureCode;

    @Column(name = "failure_message", length = 2000)
    private String failureMessage;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "embedded_at")
    private OffsetDateTime embeddedAt;

    @PrePersist
    protected void onCreate() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        if (createdAt == null) {
            createdAt = OffsetDateTime.now();
        }
        if (status == null) {
            status = EmbeddingStatus.PENDING;
        }
    }
}
