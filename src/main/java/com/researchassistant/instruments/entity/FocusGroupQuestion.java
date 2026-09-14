package com.researchassistant.instruments.entity;

import com.researchassistant.common.enums.ContentOrigin;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.util.UUID;

@Entity @Table(name = "focus_group_questions", indexes = @Index(name = "idx_focus_questions_section", columnList = "section_id"))
@Getter @Setter @NoArgsConstructor
public class FocusGroupQuestion {
    @Id @Column(nullable = false, updatable = false) private UUID id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "section_id", nullable = false) private FocusGroupSection section;
    @Column(name = "question_code", nullable = false, length = 40) private String questionCode;
    @Column(name = "question_text", nullable = false, columnDefinition = "TEXT") private String questionText;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 40) private FocusGroupQuestionType type = FocusGroupQuestionType.OTHER;
    @Column(name = "display_order", nullable = false) private int displayOrder = 1;
    @Column(name = "moderator_notes", columnDefinition = "TEXT") private String moderatorNotes;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 30) private ContentOrigin origin = ContentOrigin.USER;
    @PrePersist void onCreate() { if (id == null) id = UUID.randomUUID(); if (origin == null) origin = ContentOrigin.USER; if (type == null) type = FocusGroupQuestionType.OTHER; if (displayOrder < 1) displayOrder = 1; if (questionCode == null || questionCode.isBlank()) questionCode = "FGQ" + displayOrder; }
}
