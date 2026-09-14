package com.researchassistant.integrity.entity;

import jakarta.persistence.*;
import lombok.Getter; import lombok.NoArgsConstructor; import lombok.Setter;
import java.time.OffsetDateTime; import java.util.UUID;

@Entity @Table(name="research_integrity_issues")
@Getter @Setter @NoArgsConstructor
public class ResearchIntegrityIssue {
    @Id @Column(nullable=false, updatable=false) private UUID id;
    @ManyToOne(fetch=FetchType.LAZY, optional=false) @JoinColumn(name="review_id", nullable=false) private ResearchIntegrityReview review;
    @Enumerated(EnumType.STRING) @Column(nullable=false, length=80) private ResearchIntegrityIssueType type = ResearchIntegrityIssueType.OTHER;
    @Enumerated(EnumType.STRING) @Column(nullable=false, length=20) private IssueSeverity severity = IssueSeverity.WARNING;
    @Column(nullable=false, columnDefinition="TEXT") private String message;
    @Column(name="artifact_type", length=100) private String artifactType;
    @Column(name="artifact_id") private UUID artifactId;
    @Column(nullable=false) private boolean resolved;
    @Column(name="created_at", nullable=false, updatable=false) private OffsetDateTime createdAt;
    @PrePersist void onCreate(){ if(id==null) id=UUID.randomUUID(); if(createdAt==null) createdAt=OffsetDateTime.now(); if(type==null) type=ResearchIntegrityIssueType.OTHER; if(severity==null) severity=IssueSeverity.WARNING; }
}
