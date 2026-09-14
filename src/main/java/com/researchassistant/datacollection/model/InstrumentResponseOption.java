package com.researchassistant.datacollection.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "instrument_response_options")
@Getter @Setter @NoArgsConstructor
public class InstrumentResponseOption {
    @Id @Column(nullable = false, updatable = false)
    private UUID id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "response_id", nullable = false)
    private InstrumentResponse response;
    @Column(name = "option_value", nullable = false, length = 255)
    private String optionValue;
    @Column(name = "option_label", length = 500)
    private String optionLabel;
    @Column(name = "display_order", nullable = false)
    private int displayOrder = 1;
    @PrePersist void onCreate() { if (id == null) id = UUID.randomUUID(); if (displayOrder < 1) displayOrder = 1; }
}
