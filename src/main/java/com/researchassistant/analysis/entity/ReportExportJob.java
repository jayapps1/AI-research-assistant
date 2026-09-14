package com.researchassistant.analysis.entity;

import com.researchassistant.identity.entity.User;
import jakarta.persistence.*;
import lombok.Getter; import lombok.NoArgsConstructor; import lombok.Setter;
import java.time.OffsetDateTime; import java.util.UUID;

@Entity @Table(name="report_export_jobs")
@Getter @Setter @NoArgsConstructor
public class ReportExportJob {
    @Id @Column(nullable=false, updatable=false) private UUID id;
    @ManyToOne(fetch=FetchType.LAZY, optional=false) @JoinColumn(name="report_id", nullable=false) private ResearchReport report;
    @Enumerated(EnumType.STRING) @Column(nullable=false, length=40) private ReportExportFormat format;
    @Enumerated(EnumType.STRING) @Column(nullable=false, length=40) private ReportExportStatus status = ReportExportStatus.QUEUED;
    @Column(name="storage_key", length=1000) private String storageKey;
    @Column(nullable=false, length=500) private String filename;
    @Column(name="mime_type", nullable=false, length=255) private String mimeType;
    @Column(name="file_size_bytes") private Long fileSizeBytes;
    @Column(name="checksum_sha256", length=64) private String checksumSha256;
    @Column(name="report_revision_number", nullable=false) private int reportRevisionNumber;
    @Enumerated(EnumType.STRING) @Column(name="citation_style", nullable=false, length=60) private CitationStyle citationStyle;
    @Column(name="template_id") private UUID templateId;
    @Column(name="style_configuration_snapshot", columnDefinition="TEXT") private String styleConfigurationSnapshot;
    @ManyToOne(fetch=FetchType.LAZY, optional=false) @JoinColumn(name="requested_by", nullable=false) private User requestedBy;
    @Column(name="requested_at", nullable=false, updatable=false) private OffsetDateTime requestedAt;
    @Column(name="started_at") private OffsetDateTime startedAt;
    @Column(name="completed_at") private OffsetDateTime completedAt;
    @Column(name="error_code", length=100) private String errorCode;
    @Column(name="error_message", columnDefinition="TEXT") private String errorMessage;
    @PrePersist void onCreate(){ if(id==null) id=UUID.randomUUID(); if(requestedAt==null) requestedAt=OffsetDateTime.now(); if(status==null) status=ReportExportStatus.QUEUED; }
}
