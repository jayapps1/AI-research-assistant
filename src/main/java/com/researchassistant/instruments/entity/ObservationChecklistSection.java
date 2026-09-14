package com.researchassistant.instruments.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.util.UUID;

@Entity @Table(name = "observation_checklist_sections", indexes = @Index(name = "idx_observation_sections_checklist", columnList = "checklist_id"))
@Getter @Setter @NoArgsConstructor
public class ObservationChecklistSection {
    @Id @Column(nullable = false, updatable = false) private UUID id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "checklist_id", nullable = false) private ObservationChecklist checklist;
    @Column(nullable = false, length = 255) private String title;
    @Column(columnDefinition = "TEXT") private String description;
    @Column(name = "display_order", nullable = false) private int displayOrder = 1;
    @PrePersist void onCreate() { if (id == null) id = UUID.randomUUID(); if (displayOrder < 1) displayOrder = 1; }
}
