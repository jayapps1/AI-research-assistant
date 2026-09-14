package com.researchassistant.reference.entity;

import com.researchassistant.common.enums.ContentOrigin;
import com.researchassistant.identity.entity.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "reference_entries", indexes = @Index(name = "idx_reference_entries_normalized_doi", columnList = "normalized_doi"))
@Getter @Setter @NoArgsConstructor
public class ReferenceEntry {
    @Id @Column(nullable = false, updatable = false) private UUID id;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 60) private ReferenceType type = ReferenceType.OTHER;
    @Column(nullable = false, columnDefinition = "TEXT") private String title;
    @Column(name = "container_title", columnDefinition = "TEXT") private String containerTitle;
    @Column(name = "publication_year") private Integer publicationYear;
    @Column(name = "publication_month") private Integer publicationMonth;
    @Column(name = "publication_day") private Integer publicationDay;
    @Column(length = 80) private String volume;
    @Column(length = 80) private String issue;
    @Column(length = 120) private String pages;
    @Column(length = 255) private String publisher;
    @Column(name = "publisher_place", length = 255) private String publisherPlace;
    @Column(length = 120) private String edition;
    @Column(length = 255) private String institution;
    @Column(name = "conference_name", length = 255) private String conferenceName;
    @Column(name = "abstract_text", columnDefinition = "TEXT") private String abstractText;
    @Column(name = "language_code", length = 20) private String languageCode;
    @Column(length = 500) private String doi;
    @Column(name = "normalized_doi", length = 500) private String normalizedDoi;
    @Column(columnDefinition = "TEXT") private String url;
    @Column(length = 80) private String isbn;
    @Column(length = 80) private String issn;
    @Column(length = 80) private String pmid;
    @Column(name = "arxiv_id", length = 120) private String arxivId;
    @Enumerated(EnumType.STRING) @Column(name = "metadata_status", nullable = false, length = 40) private ReferenceMetadataStatus metadataStatus = ReferenceMetadataStatus.PARTIAL;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 30) private ContentOrigin origin = ContentOrigin.USER;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "created_by", nullable = false) private User createdBy;
    @Column(name = "created_at", nullable = false, updatable = false) private OffsetDateTime createdAt;
    @Column(name = "updated_at", nullable = false) private OffsetDateTime updatedAt;
    @PrePersist void onCreate() { if (id == null) id = UUID.randomUUID(); OffsetDateTime now = OffsetDateTime.now(); if (createdAt == null) createdAt = now; updatedAt = now; if (type == null) type = ReferenceType.OTHER; if (metadataStatus == null) metadataStatus = ReferenceMetadataStatus.PARTIAL; if (origin == null) origin = ContentOrigin.USER; }
    @PreUpdate void onUpdate() { updatedAt = OffsetDateTime.now(); }
}
