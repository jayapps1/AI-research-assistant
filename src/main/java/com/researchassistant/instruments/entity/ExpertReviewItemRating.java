package com.researchassistant.instruments.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity @Table(name = "expert_review_item_ratings")
@Getter @Setter @NoArgsConstructor
public class ExpertReviewItemRating {
    @Id @Column(nullable = false, updatable = false) private UUID id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "expert_review_id", nullable = false) private ExpertReview expertReview;
    @Enumerated(EnumType.STRING) @Column(name = "target_type", nullable = false, length = 50) private InstrumentItemTargetType targetType;
    @Column(name = "target_id", nullable = false) private UUID targetId;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 40) private ExpertRatingCriterion criterion;
    @Column(nullable = false) private int rating;
    @Column(columnDefinition = "TEXT") private String comment;
    @Column(name = "created_at", nullable = false, updatable = false) private OffsetDateTime createdAt;
    @PrePersist void onCreate() { if (id == null) id = UUID.randomUUID(); if (createdAt == null) createdAt = OffsetDateTime.now(); }
}
