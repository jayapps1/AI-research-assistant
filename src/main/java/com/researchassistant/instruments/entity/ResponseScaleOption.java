package com.researchassistant.instruments.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.util.UUID;

@Entity
@Table(name = "response_scale_options", indexes = @Index(name = "idx_response_scale_options_scale", columnList = "scale_id"))
@Getter @Setter @NoArgsConstructor
public class ResponseScaleOption {
    @Id @Column(nullable = false, updatable = false) private UUID id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "scale_id", nullable = false) private ResponseScale scale;
    @Column(name = "numeric_value", nullable = false) private int numericValue;
    @Column(nullable = false, length = 255) private String label;
    @Column(name = "display_order", nullable = false) private int displayOrder = 1;
    @PrePersist void onCreate() { if (id == null) id = UUID.randomUUID(); if (displayOrder < 1) displayOrder = 1; }
}
