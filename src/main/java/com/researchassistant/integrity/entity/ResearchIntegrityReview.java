package com.researchassistant.integrity.entity;

import com.researchassistant.analysis.entity.ResearchReport;
import com.researchassistant.identity.entity.User;
import com.researchassistant.project.entity.ResearchProject;
import jakarta.persistence.*;
import lombok.Getter; import lombok.NoArgsConstructor; import lombok.Setter;
import java.time.OffsetDateTime; import java.util.UUID;

@Entity @Table(name="research_integrity_reviews")
@Getter @Setter @NoArgsConstructor
public class ResearchIntegrityReview {
    @Id @Column(nullable=false, updatable=false) private UUID id;
    @ManyToOne(fetch=FetchType.LAZY, optional=false) @JoinColumn(name="project_id", nullable=false) private ResearchProject project;
    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="report_id") private ResearchReport report;
    @Enumerated(EnumType.STRING) @Column(nullable=false, length=40) private IntegrityReviewStatus status = IntegrityReviewStatus.REQUESTED;
    @ManyToOne(fetch=FetchType.LAZY, optional=false) @JoinColumn(name="requested_by", nullable=false) private User requestedBy;
    @Column(name="created_at", nullable=false, updatable=false) private OffsetDateTime createdAt;
    @Column(name="completed_at") private OffsetDateTime completedAt;
    @PrePersist void onCreate(){ if(id==null) id=UUID.randomUUID(); if(createdAt==null) createdAt=OffsetDateTime.now(); if(status==null) status=IntegrityReviewStatus.REQUESTED; }
}
