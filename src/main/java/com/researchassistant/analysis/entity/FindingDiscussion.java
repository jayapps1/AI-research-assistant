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
@Table(name = "finding_discussions")
@Getter @Setter @NoArgsConstructor
public class FindingDiscussion {
    @Id @Column(nullable = false, updatable = false)
    private UUID id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "project_id", nullable = false)
    private ResearchProject project;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "finding_id", nullable = false)
    private ResearchFinding finding;
    @Column(length = 255)
    private String title;
    @Column(name = "discussion_text", nullable = false, columnDefinition = "TEXT")
    private String discussionText;
    @Column(nullable = false, columnDefinition = "TEXT")
    private String interpretation;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 40)
    private DiscussionStatus status = DiscussionStatus.DRAFT;
    @Column(name = "relation_to_literature", columnDefinition = "TEXT")
    private String relationToLiterature;
    @Column(columnDefinition = "TEXT")
    private String implications;
    @Column(columnDefinition = "TEXT")
    private String limitations;
    @Column(name = "display_order", nullable = false)
    private int displayOrder = 1;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 30)
    private ContentOrigin origin = ContentOrigin.USER;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "created_by", nullable = false)
    private User createdBy;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "updated_by")
    private User updatedBy;
    @Column(name = "revision_number", nullable = false)
    private int revisionNumber = 1;
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;
    @PrePersist void onCreate() { if (id == null) id = UUID.randomUUID(); OffsetDateTime now = OffsetDateTime.now(); if (createdAt == null) createdAt = now; updatedAt = now; if (origin == null) origin = ContentOrigin.USER; if (status == null) status = DiscussionStatus.DRAFT; if (revisionNumber < 1) revisionNumber = 1; if (displayOrder < 1) displayOrder = 1; if (discussionText == null) discussionText = interpretation; if (interpretation == null) interpretation = discussionText; }
    @PreUpdate void onUpdate() { updatedAt = OffsetDateTime.now(); }
}
