package com.researchassistant.analysis.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "research_report_templates")
@Getter @Setter @NoArgsConstructor
public class ResearchReportTemplate {
    @Id @Column(nullable = false, updatable = false)
    private UUID id;
    @Column(nullable = false, length = 255)
    private String name;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 60)
    private ResearchReportType type = ResearchReportType.FINAL_YEAR_PROJECT;
    @Column(length = 255)
    private String institution;
    @Column(name = "system_template", nullable = false)
    private boolean systemTemplate;
    @Column(name = "configuration_json", nullable = false, columnDefinition = "TEXT")
    private String configurationJson;
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;
    @PrePersist void onCreate() { if (id == null) id = UUID.randomUUID(); if (createdAt == null) createdAt = OffsetDateTime.now(); if (type == null) type = ResearchReportType.FINAL_YEAR_PROJECT; }
}
