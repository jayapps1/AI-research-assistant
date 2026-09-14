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
        name = "document_pages",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_document_pages_version_page",
                columnNames = {"document_version_id", "page_number"}
        ),
        indexes = {
                @Index(name = "idx_document_pages_version_id", columnList = "document_version_id"),
                @Index(name = "idx_document_pages_version_page", columnList = "document_version_id,page_number")
        }
)
@Getter
@Setter
@NoArgsConstructor
public class DocumentPage {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "document_version_id", nullable = false)
    private DocumentVersion documentVersion;

    @Column(name = "page_number", nullable = false)
    private int pageNumber;

    @Column(name = "source_label", length = 100)
    private String sourceLabel;

    @Column(name = "text_content", nullable = false, columnDefinition = "TEXT")
    private String textContent;

    @Column(name = "character_count", nullable = false)
    private long characterCount;

    @Column(name = "content_checksum_sha256", nullable = false, length = 64)
    private String contentChecksumSha256;

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
