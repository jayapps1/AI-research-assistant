package com.researchassistant.evidence.entity;

import com.researchassistant.analysis.entity.ResearchReport;
import com.researchassistant.analysis.entity.ResearchReportSection;
import com.researchassistant.common.storage.StorageObjectEntity;
import com.researchassistant.identity.entity.User;
import com.researchassistant.project.entity.ResearchProject;
import com.researchassistant.workspace.entity.Workspace;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "project_evidence")
@Getter
@Setter
@NoArgsConstructor
public class ProjectEvidence {

    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "project_id", nullable = false)
    private ResearchProject project;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "workspace_id", nullable = false)
    private Workspace workspace;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "report_id")
    private ResearchReport report;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "section_id")
    private ResearchReportSection section;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "storage_object_id")
    private StorageObjectEntity storageObject;

    @Column(name = "storage_key", length = 512)
    private String storageKey;

    @Column(name = "original_filename", nullable = false, length = 255)
    private String originalFilename;

    @Column(name = "mime_type", nullable = false, length = 128)
    private String mimeType;

    @Column(name = "file_size_bytes", nullable = false)
    private long fileSizeBytes = 0;

    @Enumerated(EnumType.STRING)
    @Column(name = "evidence_type", nullable = false, length = 64)
    private EvidenceType evidenceType = EvidenceType.SCREENSHOT;

    @Column(name = "figure_label", length = 100)
    private String figureLabel;

    @Column(columnDefinition = "TEXT")
    private String caption;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "alt_text", columnDefinition = "TEXT")
    private String altText;

    @Column(name = "display_order", nullable = false)
    private int displayOrder = 0;

    @Column(name = "ai_visual_analysis", columnDefinition = "TEXT")
    private String aiVisualAnalysis;

    @Enumerated(EnumType.STRING)
    @Column(name = "ai_analysis_status", nullable = false, length = 32)
    private EvidenceAnalysisStatus aiAnalysisStatus = EvidenceAnalysisStatus.NOT_ANALYZED;

    @Column(name = "ai_analysis_error", columnDefinition = "TEXT")
    private String aiAnalysisError;

    @Column(name = "metadata_json", columnDefinition = "TEXT")
    private String metadataJson;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "created_by_user_id", nullable = false)
    private User createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @PrePersist
    void onCreate() {
        if (id == null) id = UUID.randomUUID();
        OffsetDateTime now = OffsetDateTime.now();
        if (createdAt == null) createdAt = now;
        updatedAt = now;
        if (aiAnalysisStatus == null) aiAnalysisStatus = EvidenceAnalysisStatus.NOT_ANALYZED;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = OffsetDateTime.now();
    }
}
