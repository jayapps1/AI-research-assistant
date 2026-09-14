package com.researchassistant.integrity.entity;

import com.researchassistant.identity.entity.User;
import com.researchassistant.project.entity.ResearchProject;
import jakarta.persistence.*;
import lombok.Getter; import lombok.NoArgsConstructor; import lombok.Setter;
import java.time.OffsetDateTime; import java.util.UUID;

@Entity @Table(name = "similarity_checks")
@Getter @Setter @NoArgsConstructor
public class SimilarityCheck {
    @Id @Column(nullable=false, updatable=false) private UUID id;
    @ManyToOne(fetch=FetchType.LAZY, optional=false) @JoinColumn(name="project_id", nullable=false) private ResearchProject project;
    @Enumerated(EnumType.STRING) @Column(name="target_type", nullable=false, length=60) private SimilarityTargetType targetType;
    @Column(name="target_id") private UUID targetId;
    @Column(name="custom_text", columnDefinition="TEXT") private String customText;
    @Enumerated(EnumType.STRING) @Column(nullable=false, length=40) private SimilarityProvider provider = SimilarityProvider.LOCAL_PROJECT;
    @Enumerated(EnumType.STRING) @Column(nullable=false, length=40) private SimilarityCheckStatus status = SimilarityCheckStatus.REQUESTED;
    @Column(name="overall_similarity_percent") private Double overallSimilarityPercent;
    @Column(name="provider_report_reference", length=500) private String providerReportReference;
    @ManyToOne(fetch=FetchType.LAZY, optional=false) @JoinColumn(name="requested_by", nullable=false) private User requestedBy;
    @Column(name="requested_at", nullable=false, updatable=false) private OffsetDateTime requestedAt;
    @Column(name="completed_at") private OffsetDateTime completedAt;
    @Column(name="error_code", length=100) private String errorCode;
    @Column(name="error_message", columnDefinition="TEXT") private String errorMessage;
    @PrePersist void onCreate(){ if(id==null) id=UUID.randomUUID(); if(requestedAt==null) requestedAt=OffsetDateTime.now(); if(provider==null) provider=SimilarityProvider.LOCAL_PROJECT; if(status==null) status=SimilarityCheckStatus.REQUESTED; }
}
