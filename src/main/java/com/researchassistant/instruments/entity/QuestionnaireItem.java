package com.researchassistant.instruments.entity;

import com.researchassistant.common.enums.ContentOrigin;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "questionnaire_items", indexes = @Index(name = "idx_questionnaire_items_section", columnList = "section_id"))
@Getter @Setter @NoArgsConstructor
public class QuestionnaireItem {
    @Id @Column(nullable = false, updatable = false) private UUID id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "section_id", nullable = false) private QuestionnaireSection section;
    @Column(name = "item_code", nullable = false, length = 40) private String itemCode;
    @Column(nullable = false, columnDefinition = "TEXT") private String prompt;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 40) private QuestionnaireItemType type = QuestionnaireItemType.OTHER;
    @Column(nullable = false) private boolean required;
    @Column(name = "display_order", nullable = false) private int displayOrder = 1;
    @Column(name = "help_text", columnDefinition = "TEXT") private String helpText;
    @Column(name = "validation_rules_json", columnDefinition = "TEXT") private String validationRulesJson;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "response_scale_id") private ResponseScale responseScale;
    @Column(name = "reverse_scored", nullable = false) private boolean reverseScored;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 30) private ContentOrigin origin = ContentOrigin.USER;
    @Column(name = "created_at", nullable = false, updatable = false) private OffsetDateTime createdAt;
    @Column(name = "updated_at", nullable = false) private OffsetDateTime updatedAt;
    @PrePersist void onCreate() { if (id == null) id = UUID.randomUUID(); OffsetDateTime now = OffsetDateTime.now(); if (createdAt == null) createdAt = now; updatedAt = now; if (origin == null) origin = ContentOrigin.USER; if (type == null) type = QuestionnaireItemType.OTHER; if (displayOrder < 1) displayOrder = 1; if (itemCode == null || itemCode.isBlank()) itemCode = "Q" + displayOrder; }
    @PreUpdate void onUpdate() { updatedAt = OffsetDateTime.now(); }
}
