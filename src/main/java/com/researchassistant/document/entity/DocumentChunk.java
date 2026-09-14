package com.researchassistant.document.entity;

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
        name = "document_chunks",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_document_chunks_version_number",
                columnNames = {"document_version_id", "chunk_number"}
        ),
        indexes = {
                @Index(name = "idx_document_chunks_version_id", columnList = "document_version_id"),
                @Index(name = "idx_document_chunks_page_id", columnList = "page_id"),
                @Index(name = "idx_document_chunks_version_number", columnList = "document_version_id,chunk_number")
        }
)
@Getter
@Setter
@NoArgsConstructor
public class DocumentChunk {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "document_version_id", nullable = false)
    private DocumentVersion documentVersion;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "page_id", nullable = false)
    private DocumentPage page;

    @Column(name = "chunk_number", nullable = false)
    private int chunkNumber;

    @Column(name = "text_content", nullable = false, columnDefinition = "TEXT")
    private String textContent;

    /**
     * Inclusive start offset in the normalized DocumentPage text.
     */
    @Column(name = "character_start", nullable = false)
    private int characterStart;

    /**
     * Exclusive end offset in the normalized DocumentPage text.
     */
    @Column(name = "character_end", nullable = false)
    private int characterEnd;

    @Column(name = "character_count", nullable = false)
    private long characterCount;

    @Column(name = "estimated_token_count")
    private Integer estimatedTokenCount;

    @Column(name = "content_checksum_sha256", nullable = false, length = 64)
    private String contentChecksumSha256;

    @Column(name = "heading", length = 1000)
    private String heading;

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
