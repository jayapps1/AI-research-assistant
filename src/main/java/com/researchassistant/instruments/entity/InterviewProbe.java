package com.researchassistant.instruments.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.util.UUID;

@Entity @Table(name = "interview_probes", indexes = @Index(name = "idx_interview_probes_question", columnList = "question_id"))
@Getter @Setter @NoArgsConstructor
public class InterviewProbe {
    @Id @Column(nullable = false, updatable = false) private UUID id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "question_id", nullable = false) private InterviewQuestion question;
    @Column(name = "probe_text", nullable = false, columnDefinition = "TEXT") private String probeText;
    @Column(name = "display_order", nullable = false) private int displayOrder = 1;
    @PrePersist void onCreate() { if (id == null) id = UUID.randomUUID(); if (displayOrder < 1) displayOrder = 1; }
}
