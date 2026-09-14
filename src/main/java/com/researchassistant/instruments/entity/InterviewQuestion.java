package com.researchassistant.instruments.entity;

import com.researchassistant.common.enums.ContentOrigin;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity @Table(name = "interview_questions", indexes = @Index(name = "idx_interview_questions_section", columnList = "section_id"))
@Getter @Setter @NoArgsConstructor
public class InterviewQuestion {
    @Id @Column(nullable = false, updatable = false) private UUID id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "section_id", nullable = false) private InterviewGuideSection section;
    @Column(name = "question_code", nullable = false, length = 40) private String questionCode;
    @Column(name = "question_text", nullable = false, columnDefinition = "TEXT") private String questionText;
    @Column(name = "display_order", nullable = false) private int displayOrder = 1;
    @Column(nullable = false) private boolean required;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 30) private ContentOrigin origin = ContentOrigin.USER;
    @Column(name = "created_at", nullable = false, updatable = false) private OffsetDateTime createdAt;
    @Column(name = "updated_at", nullable = false) private OffsetDateTime updatedAt;
    @PrePersist void onCreate() { if (id == null) id = UUID.randomUUID(); OffsetDateTime now = OffsetDateTime.now(); if (createdAt == null) createdAt = now; updatedAt = now; if (origin == null) origin = ContentOrigin.USER; if (displayOrder < 1) displayOrder = 1; if (questionCode == null || questionCode.isBlank()) questionCode = "IQ" + displayOrder; }
    @PreUpdate void onUpdate() { updatedAt = OffsetDateTime.now(); }
}
