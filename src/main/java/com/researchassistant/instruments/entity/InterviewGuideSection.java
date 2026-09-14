package com.researchassistant.instruments.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.util.UUID;

@Entity @Table(name = "interview_guide_sections", indexes = @Index(name = "idx_interview_sections_guide", columnList = "guide_id"))
@Getter @Setter @NoArgsConstructor
public class InterviewGuideSection {
    @Id @Column(nullable = false, updatable = false) private UUID id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "guide_id", nullable = false) private InterviewGuide guide;
    @Column(nullable = false, length = 255) private String title;
    @Column(columnDefinition = "TEXT") private String description;
    @Column(name = "display_order", nullable = false) private int displayOrder = 1;
    @PrePersist void onCreate() { if (id == null) id = UUID.randomUUID(); if (displayOrder < 1) displayOrder = 1; }
}
