package com.researchassistant.reference.entity;

import com.researchassistant.identity.entity.User;
import com.researchassistant.project.entity.ResearchProject;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "project_references", uniqueConstraints = @UniqueConstraint(name = "uk_project_references_key", columnNames = {"project_id","citation_key"}))
@Getter @Setter @NoArgsConstructor
public class ProjectReference {
    @Id @Column(nullable = false, updatable = false) private UUID id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "project_id", nullable = false) private ResearchProject project;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "reference_id", nullable = false) private ReferenceEntry reference;
    @Column(name = "citation_key", nullable = false, length = 120) private String citationKey;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 40) private ProjectReferenceStatus status = ProjectReferenceStatus.ACTIVE;
    @Column(name = "available_for_research_ai", nullable = false) private boolean availableForResearchAi = true;
    @Column(name = "available_for_citation", nullable = false) private boolean availableForCitation = true;
    @Column(name = "include_when_cited_in_bibliography", nullable = false) private boolean includeWhenCitedInBibliography = true;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "added_by", nullable = false) private User addedBy;
    @Column(name = "created_at", nullable = false, updatable = false) private OffsetDateTime createdAt;
    @PrePersist void onCreate() { if (id == null) id = UUID.randomUUID(); if (createdAt == null) createdAt = OffsetDateTime.now(); if (status == null) status = ProjectReferenceStatus.ACTIVE; }
}
