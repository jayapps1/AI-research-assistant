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
@Table(name = "research_report_section_versions",
        uniqueConstraints = @UniqueConstraint(name = "uk_report_section_versions_revision", columnNames = {"section_id", "revision_number"}))
@Getter @Setter @NoArgsConstructor
public class ResearchReportSectionVersion {
    @Id @Column(nullable = false, updatable = false)
    private UUID id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "section_id", nullable = false)
    private ResearchReportSection section;
    @Column(name = "revision_number", nullable = false)
    private int revisionNumber = 1;
    @Column(length = 120)
    private String label;
    @Column(columnDefinition = "TEXT")
    private String content;
    @Column(name = "content_json", columnDefinition = "TEXT")
    private String contentJson;
    @Column(name = "plain_text", columnDefinition = "TEXT")
    private String plainText;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 40)
    private ReportSectionStatus status = ReportSectionStatus.DRAFT;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 30)
    private ContentOrigin origin = ContentOrigin.USER;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "created_by")
    private User createdBy;
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @PrePersist void onCreate() {
        if (id == null) id = UUID.randomUUID();
        if (createdAt == null) createdAt = OffsetDateTime.now();
        if (status == null) status = ReportSectionStatus.DRAFT;
        if (origin == null) origin = ContentOrigin.USER;
        if (revisionNumber < 1) revisionNumber = 1;
    }
}
