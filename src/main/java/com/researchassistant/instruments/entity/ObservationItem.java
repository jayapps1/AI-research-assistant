package com.researchassistant.instruments.entity;

import com.researchassistant.common.enums.ContentOrigin;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.util.UUID;

@Entity @Table(name = "observation_items", indexes = @Index(name = "idx_observation_items_section", columnList = "section_id"))
@Getter @Setter @NoArgsConstructor
public class ObservationItem {
    @Id @Column(nullable = false, updatable = false) private UUID id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "section_id", nullable = false) private ObservationChecklistSection section;
    @Column(name = "item_code", nullable = false, length = 40) private String itemCode;
    @Column(nullable = false, columnDefinition = "TEXT") private String description;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 40) private ObservationItemType type = ObservationItemType.OTHER;
    @Column(nullable = false) private boolean required;
    @Column(name = "display_order", nullable = false) private int displayOrder = 1;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 30) private ContentOrigin origin = ContentOrigin.USER;
    @PrePersist void onCreate() { if (id == null) id = UUID.randomUUID(); if (origin == null) origin = ContentOrigin.USER; if (type == null) type = ObservationItemType.OTHER; if (displayOrder < 1) displayOrder = 1; if (itemCode == null || itemCode.isBlank()) itemCode = "OI" + displayOrder; }
}
