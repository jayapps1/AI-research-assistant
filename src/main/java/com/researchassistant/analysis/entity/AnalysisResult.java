package com.researchassistant.analysis.entity;

import com.researchassistant.common.enums.ContentOrigin;
import com.researchassistant.identity.entity.User;
import com.researchassistant.project.entity.ResearchProject;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "analysis_results", indexes = @Index(name = "idx_analysis_results_run", columnList = "analysis_run_id"))
@Getter @Setter @NoArgsConstructor
public class AnalysisResult {
    @Id @Column(nullable = false, updatable = false)
    private UUID id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "analysis_run_id", nullable = false)
    private AnalysisRun analysisRun;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "project_id", nullable = false)
    private ResearchProject project;
    @Column(nullable = false, columnDefinition = "TEXT")
    private String summary;
    @Column(name = "result_payload_json", columnDefinition = "TEXT")
    private String resultPayloadJson;
    @Column(columnDefinition = "TEXT")
    private String limitations;
    @Enumerated(EnumType.STRING) @Column(name = "generated_by", nullable = false, length = 30)
    private ContentOrigin generatedBy = ContentOrigin.USER;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "created_by", nullable = false)
    private User createdBy;
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;
    @PrePersist void onCreate() { if (id == null) id = UUID.randomUUID(); if (createdAt == null) createdAt = OffsetDateTime.now(); if (generatedBy == null) generatedBy = ContentOrigin.USER; }
}
