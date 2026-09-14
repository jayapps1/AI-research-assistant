package com.researchassistant.analysis.entity;

import com.researchassistant.common.enums.ContentOrigin;
import com.researchassistant.identity.entity.User;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "research_report_sections")
@Getter @Setter @NoArgsConstructor
public class ResearchReportSection {
    @Id @Column(nullable = false, updatable = false)
    private UUID id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "chapter_id", nullable = false)
    private ResearchReportChapter chapter;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 80)
    private ReportSectionType type = ReportSectionType.CUSTOM;
    @Column(nullable = false, length = 255)
    private String heading;
    @Column(columnDefinition = "TEXT")
    private String content;
    @Column(name = "display_order", nullable = false)
    private int displayOrder = 1;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 30)
    private ContentOrigin origin = ContentOrigin.USER;
    @Column(name = "source_artifact_type", length = 100)
    private String sourceArtifactType;
    @Column(name = "source_artifact_id")
    private UUID sourceArtifactId;
    @Column(name = "source_revision_number")
    private Integer sourceRevisionNumber;
    @Column(name = "source_out_of_date", nullable = false)
    private boolean sourceOutOfDate;
    @Column(name = "manually_edited", nullable = false)
    private boolean manuallyEdited;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "created_by")
    private User createdBy;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "updated_by")
    private User updatedBy;
    @Column(name = "revision_number", nullable = false)
    private int revisionNumber = 1;
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;
    @PrePersist void onCreate() { if (id == null) id = UUID.randomUUID(); OffsetDateTime now = OffsetDateTime.now(); if (createdAt == null) createdAt = now; updatedAt = now; if (type == null) type = ReportSectionType.CUSTOM; if (origin == null) origin = ContentOrigin.USER; if (revisionNumber < 1) revisionNumber = 1; if (displayOrder < 1) displayOrder = 1; }
    @PreUpdate void onUpdate() { updatedAt = OffsetDateTime.now(); }
}
