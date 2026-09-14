package com.researchassistant.integrity.entity;

import com.researchassistant.common.enums.ContentOrigin;
import com.researchassistant.identity.entity.User;
import com.researchassistant.project.entity.ResearchProject;
import jakarta.persistence.*;
import lombok.Getter; import lombok.NoArgsConstructor; import lombok.Setter;
import java.time.OffsetDateTime; import java.util.UUID;

@Entity @Table(name="academic_writing_reviews")
@Getter @Setter @NoArgsConstructor
public class AcademicWritingReview {
    @Id @Column(nullable=false, updatable=false) private UUID id;
    @ManyToOne(fetch=FetchType.LAZY, optional=false) @JoinColumn(name="project_id", nullable=false) private ResearchProject project;
    @Enumerated(EnumType.STRING) @Column(name="target_type", nullable=false, length=80) private WritingReviewTargetType targetType;
    @Column(name="target_id", nullable=false) private UUID targetId;
    @Enumerated(EnumType.STRING) @Column(nullable=false, length=40) private AcademicWritingReviewStatus status = AcademicWritingReviewStatus.REQUESTED;
    @Enumerated(EnumType.STRING) @Column(nullable=false, length=30) private ContentOrigin origin = ContentOrigin.USER;
    @Column(name="reviewer_type", nullable=false, length=80) private String reviewerType = "DETERMINISTIC";
    @ManyToOne(fetch=FetchType.LAZY, optional=false) @JoinColumn(name="requested_by", nullable=false) private User requestedBy;
    @Column(name="created_at", nullable=false, updatable=false) private OffsetDateTime createdAt;
    @Column(name="completed_at") private OffsetDateTime completedAt;
    @PrePersist void onCreate(){ if(id==null) id=UUID.randomUUID(); if(createdAt==null) createdAt=OffsetDateTime.now(); if(status==null) status=AcademicWritingReviewStatus.REQUESTED; if(origin==null) origin=ContentOrigin.USER; if(reviewerType==null) reviewerType="DETERMINISTIC"; }
}
