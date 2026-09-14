package com.researchassistant.instruments.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity @Table(name = "expert_reviews")
@Getter @Setter @NoArgsConstructor
public class ExpertReview {
    @Id @Column(nullable = false, updatable = false) private UUID id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "instrument_id", nullable = false) private ResearchInstrument instrument;
    @Column(name = "reviewer_code", nullable = false, length = 100) private String reviewerCode;
    @Column(name = "reviewer_role_or_expertise") private String reviewerRoleOrExpertise;
    @Column(name = "review_date") private LocalDate reviewDate;
    @Column(name = "overall_comments", columnDefinition = "TEXT") private String overallComments;
    @Column(name = "created_at", nullable = false, updatable = false) private OffsetDateTime createdAt;
    @PrePersist void onCreate() { if (id == null) id = UUID.randomUUID(); if (createdAt == null) createdAt = OffsetDateTime.now(); }
}
