package com.researchassistant.document.entity;

import com.researchassistant.identity.entity.User;
import com.researchassistant.project.entity.ResearchProject;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Logical research source inside a project.
 *
 * <p>{@code documentNumber} and {@code documentCode} are permanent
 * project-scoped identifiers. They are allocated from
 * {@code ResearchProject.nextDocumentNumber} under a row lock and
 * must never be recalculated from document counts, reused after
 * archive/failure, or changed by user input.</p>
 *
 * <p>{@code nextVersionNumber} is the document-local allocation
 * counter for immutable file revisions. It is advanced while holding
 * a pessimistic lock on this document row.</p>
 */
@Entity
@Table(
        name = "documents",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_documents_project_number",
                        columnNames = {"project_id", "document_number"}
                ),
                @UniqueConstraint(
                        name = "uk_documents_project_code",
                        columnNames = {"project_id", "document_code"}
                )
        },
        indexes = {
                @Index(name = "idx_documents_project_id", columnList = "project_id"),
                @Index(name = "idx_documents_project_status", columnList = "project_id,status"),
                @Index(name = "idx_documents_created_by", columnList = "created_by"),
                @Index(name = "idx_documents_document_code", columnList = "document_code")
        }
)
@Getter
@Setter
@NoArgsConstructor
public class Document {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "project_id", nullable = false)
    private ResearchProject project;

    @Column(name = "document_number", nullable = false)
    private long documentNumber;

    @Column(name = "document_code", nullable = false, length = 32)
    private String documentCode;

    @Column(name = "title", nullable = false, length = 500)
    private String title;

    @Column(name = "bibliographic_title", length = 1000)
    private String bibliographicTitle;

    @Column(name = "authors", columnDefinition = "TEXT")
    private String authors;

    @Column(name = "publication_year")
    private Integer publicationYear;

    @Column(name = "journal", length = 500)
    private String journal;

    @Column(name = "conference", length = 500)
    private String conference;

    @Column(name = "publisher", length = 500)
    private String publisher;

    @Column(name = "volume", length = 100)
    private String volume;

    @Column(name = "issue", length = 100)
    private String issue;

    @Column(name = "pages", length = 100)
    private String pages;

    @Column(name = "doi", length = 500)
    private String doi;

    @Column(name = "url", length = 1000)
    private String url;

    @Column(name = "source_type", length = 80)
    private String sourceType;

    @Column(name = "keywords", columnDefinition = "TEXT")
    private String keywords;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 40)
    private DocumentType type;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 40)
    private DocumentStatus status = DocumentStatus.UPLOADING;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "created_by", nullable = false)
    private User createdBy;

    @Column(name = "next_version_number", nullable = false)
    private int nextVersionNumber = 1;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "current_version_id")
    private DocumentVersion currentVersion;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @Column(name = "archived_at")
    private OffsetDateTime archivedAt;

    @PrePersist
    protected void onCreate() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        OffsetDateTime now = OffsetDateTime.now();
        if (createdAt == null) {
            createdAt = now;
        }
        updatedAt = now;
        if (nextVersionNumber < 1) {
            nextVersionNumber = 1;
        }
        if (status == null) {
            status = DocumentStatus.UPLOADING;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = OffsetDateTime.now();
    }
}
