package com.researchassistant.analysis.entity;

import com.researchassistant.identity.entity.User;
import com.researchassistant.project.entity.ResearchProject;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "report_document_versions",
        uniqueConstraints = @UniqueConstraint(name = "uk_report_document_versions_number", columnNames = {"report_id", "version_number"}))
@Getter @Setter @NoArgsConstructor
public class ReportDocumentVersion {
    @Id @Column(nullable = false, updatable = false)
    private UUID id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "report_id", nullable = false)
    private ResearchReport report;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "project_id", nullable = false)
    private ResearchProject project;
    @Column(name = "version_number", nullable = false)
    private int versionNumber = 1;
    @Column(nullable = false, length = 255)
    private String title;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 40)
    private ReportDocumentVersionStatus status = ReportDocumentVersionStatus.FINAL_REVIEW;
    @Enumerated(EnumType.STRING) @Column(name = "citation_style", nullable = false, length = 60)
    private CitationStyle citationStyle = CitationStyle.APA_7;
    @Column(name = "content_json", nullable = false, columnDefinition = "TEXT")
    private String contentJson;
    @Column(name = "plain_text", columnDefinition = "TEXT")
    private String plainText;
    @Column(name = "source_report_revision_number", nullable = false)
    private int sourceReportRevisionNumber = 1;
    @Column(name = "section_revision_snapshot_json", columnDefinition = "TEXT")
    private String sectionRevisionSnapshotJson;
    @Column(name = "references_snapshot_json", columnDefinition = "TEXT")
    private String referencesSnapshotJson;
    @Column(name = "template_snapshot_json", columnDefinition = "TEXT")
    private String templateSnapshotJson;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "created_by", nullable = false)
    private User createdBy;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "updated_by")
    private User updatedBy;
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @PrePersist void onCreate() {
        if (id == null) id = UUID.randomUUID();
        OffsetDateTime now = OffsetDateTime.now();
        if (createdAt == null) createdAt = now;
        updatedAt = now;
        if (versionNumber < 1) versionNumber = 1;
        if (status == null) status = ReportDocumentVersionStatus.FINAL_REVIEW;
        if (citationStyle == null) citationStyle = CitationStyle.APA_7;
    }

    @PreUpdate void onUpdate() {
        updatedAt = OffsetDateTime.now();
    }
}
