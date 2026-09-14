package com.researchassistant.reference.entity;

import com.researchassistant.document.entity.Document;
import com.researchassistant.document.entity.DocumentVersion;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "reference_source_links")
@Getter @Setter @NoArgsConstructor
public class ReferenceSourceLink {
    @Id @Column(nullable = false, updatable = false) private UUID id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "reference_id", nullable = false) private ReferenceEntry reference;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "document_id", nullable = false) private Document document;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "document_version_id") private DocumentVersion documentVersion;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 60) private ReferenceSourceLinkType type = ReferenceSourceLinkType.OTHER;
    @Column(name = "created_at", nullable = false, updatable = false) private OffsetDateTime createdAt;
    @PrePersist void onCreate() { if (id == null) id = UUID.randomUUID(); if (createdAt == null) createdAt = OffsetDateTime.now(); if (type == null) type = ReferenceSourceLinkType.OTHER; }
}
