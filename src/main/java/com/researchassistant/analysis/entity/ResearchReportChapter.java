package com.researchassistant.analysis.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "research_report_chapters")
@Getter @Setter @NoArgsConstructor
public class ResearchReportChapter {
    @Id @Column(nullable = false, updatable = false)
    private UUID id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "report_id", nullable = false)
    private ResearchReport report;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 60)
    private ReportChapterType type = ReportChapterType.CUSTOM;
    @Column(nullable = false, length = 255)
    private String title;
    @Column(name = "chapter_number")
    private Integer chapterNumber;
    @Column(name = "display_order", nullable = false)
    private int displayOrder = 1;
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;
    @PrePersist void onCreate() { if (id == null) id = UUID.randomUUID(); OffsetDateTime now = OffsetDateTime.now(); if (createdAt == null) createdAt = now; updatedAt = now; if (type == null) type = ReportChapterType.CUSTOM; if (displayOrder < 1) displayOrder = 1; }
    @PreUpdate void onUpdate() { updatedAt = OffsetDateTime.now(); }
}
