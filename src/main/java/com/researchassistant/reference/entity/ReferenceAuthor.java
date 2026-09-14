package com.researchassistant.reference.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.util.UUID;

@Entity
@Table(name = "reference_authors")
@Getter @Setter @NoArgsConstructor
public class ReferenceAuthor {
    @Id @Column(nullable = false, updatable = false) private UUID id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "reference_id", nullable = false) private ReferenceEntry reference;
    @Column(name = "family_name", length = 255) private String familyName;
    @Column(name = "given_name", length = 255) private String givenName;
    @Column(name = "literal_name", length = 500) private String literalName;
    @Column(length = 80) private String orcid;
    @Column(name = "display_order", nullable = false) private int displayOrder = 1;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 40) private AuthorRole role = AuthorRole.AUTHOR;
    @PrePersist void onCreate() { if (id == null) id = UUID.randomUUID(); if (displayOrder < 1) displayOrder = 1; if (role == null) role = AuthorRole.AUTHOR; }
}
