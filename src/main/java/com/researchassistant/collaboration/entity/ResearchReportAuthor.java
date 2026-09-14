package com.researchassistant.collaboration.entity;

import com.researchassistant.analysis.entity.ResearchReport;
import com.researchassistant.identity.entity.User;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "research_report_authors", indexes = @Index(name = "idx_report_authors_report_order", columnList = "report_id,author_order"))
@Getter @Setter @NoArgsConstructor
public class ResearchReportAuthor {
    @Id @Column(nullable = false, updatable = false)
    private UUID id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "report_id", nullable = false)
    private ResearchReport report;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "user_id")
    private User user;
    @Column(name = "display_name", nullable = false, length = 255)
    private String displayName;
    @Column(name = "student_number", length = 100)
    private String studentNumber;
    @Column(name = "index_number", length = 100)
    private String indexNumber;
    @Column(length = 255)
    private String programme;
    @Column(name = "author_order", nullable = false)
    private int authorOrder = 1;
    @Column(name = "corresponding_author", nullable = false)
    private boolean correspondingAuthor;
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;
    @PrePersist void onCreate() { if (id == null) id = UUID.randomUUID(); if (createdAt == null) createdAt = OffsetDateTime.now(); if (authorOrder < 1) authorOrder = 1; }
}
