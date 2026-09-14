package com.researchassistant.integrity.entity;

import jakarta.persistence.*;
import lombok.Getter; import lombok.NoArgsConstructor; import lombok.Setter;
import java.time.OffsetDateTime; import java.util.UUID;

@Entity @Table(name="academic_writing_issues")
@Getter @Setter @NoArgsConstructor
public class AcademicWritingIssue {
    @Id @Column(nullable=false, updatable=false) private UUID id;
    @ManyToOne(fetch=FetchType.LAZY, optional=false) @JoinColumn(name="review_id", nullable=false) private AcademicWritingReview review;
    @Enumerated(EnumType.STRING) @Column(nullable=false, length=80) private WritingIssueType type = WritingIssueType.OTHER;
    @Enumerated(EnumType.STRING) @Column(nullable=false, length=20) private IssueSeverity severity = IssueSeverity.WARNING;
    @Column(name="start_offset") private Integer startOffset;
    @Column(name="end_offset") private Integer endOffset;
    @Column(name="text_snapshot", columnDefinition="TEXT") private String textSnapshot;
    @Column(nullable=false, columnDefinition="TEXT") private String message;
    @Column(columnDefinition="TEXT") private String suggestion;
    @Column(nullable=false) private boolean resolved;
    @Column(name="created_at", nullable=false, updatable=false) private OffsetDateTime createdAt;
    @PrePersist void onCreate(){ if(id==null) id=UUID.randomUUID(); if(createdAt==null) createdAt=OffsetDateTime.now(); if(type==null) type=WritingIssueType.OTHER; if(severity==null) severity=IssueSeverity.WARNING; }
}
