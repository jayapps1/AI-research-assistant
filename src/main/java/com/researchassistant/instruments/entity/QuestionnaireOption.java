package com.researchassistant.instruments.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.util.UUID;

@Entity
@Table(name = "questionnaire_options", indexes = @Index(name = "idx_questionnaire_options_item", columnList = "item_id"))
@Getter @Setter @NoArgsConstructor
public class QuestionnaireOption {
    @Id @Column(nullable = false, updatable = false) private UUID id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "item_id", nullable = false) private QuestionnaireItem item;
    @Column(nullable = false, length = 255) private String value;
    @Column(nullable = false, length = 500) private String label;
    @Column(name = "display_order", nullable = false) private int displayOrder = 1;
    @Column(name = "numeric_score") private Double numericScore;
    @PrePersist void onCreate() { if (id == null) id = UUID.randomUUID(); if (displayOrder < 1) displayOrder = 1; }
}
