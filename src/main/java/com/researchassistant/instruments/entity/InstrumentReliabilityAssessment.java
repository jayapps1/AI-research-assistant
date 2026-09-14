package com.researchassistant.instruments.entity;

import com.researchassistant.common.enums.ContentOrigin;
import com.researchassistant.identity.entity.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity @Table(name = "instrument_reliability_assessments")
@Getter @Setter @NoArgsConstructor
public class InstrumentReliabilityAssessment {
    @Id @Column(nullable = false, updatable = false) private UUID id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "instrument_id", nullable = false) private ResearchInstrument instrument;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 40) private ReliabilityMethod method;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 40) private AssessmentStatus status = AssessmentStatus.PLANNED;
    @Column private Double coefficient;
    @Column(name = "item_count") private Integer itemCount;
    @Column(name = "respondent_count") private Integer respondentCount;
    @Column(columnDefinition = "TEXT") private String assumptions;
    @Column(columnDefinition = "TEXT") private String interpretation;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 30) private ContentOrigin origin = ContentOrigin.USER;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "created_by", nullable = false) private User createdBy;
    @Column(name = "created_at", nullable = false, updatable = false) private OffsetDateTime createdAt;
    @Column(name = "updated_at", nullable = false) private OffsetDateTime updatedAt;
    @PrePersist void onCreate() { if (id == null) id = UUID.randomUUID(); OffsetDateTime now = OffsetDateTime.now(); if (createdAt == null) createdAt = now; updatedAt = now; if (origin == null) origin = ContentOrigin.USER; if (status == null) status = AssessmentStatus.PLANNED; }
    @PreUpdate void onUpdate() { updatedAt = OffsetDateTime.now(); }
}
