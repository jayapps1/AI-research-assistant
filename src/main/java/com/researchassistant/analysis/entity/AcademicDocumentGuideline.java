package com.researchassistant.analysis.entity;

import com.researchassistant.document.entity.Document;
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
@Table(
        name = "academic_document_guidelines",
        indexes = {
                @Index(name = "idx_guidelines_project_id", columnList = "project_id"),
                @Index(name = "idx_guidelines_workspace_id", columnList = "workspace_id"),
                @Index(name = "idx_guidelines_status", columnList = "status")
        }
)
@Getter
@Setter
@NoArgsConstructor
public class AcademicDocumentGuideline {

    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "workspace_id", nullable = false)
    private Workspace workspace;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id")
    private ResearchProject project;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "document_id")
    private Document document;

    @Column(name = "original_file_name", nullable = false)
    private String originalFileName;

    @Column(nullable = false, length = 50)
    private String source = "UPLOADED";

    @Column(nullable = false)
    private int version = 1;

    @Column(nullable = false, length = 50)
    private String status = "EXTRACTED";

    @Column(length = 255)
    private String institution;

    @Column(length = 255)
    private String department;

    @Column(length = 255)
    private String programme;

    @Column(name = "document_type", length = 100)
    private String documentType;

    @Column(name = "citation_style", length = 60)
    private String citationStyle;

    @Column(name = "raw_extraction_json", columnDefinition = "TEXT")
    private String rawExtractionJson;

    @Column(name = "approved_structure_json", columnDefinition = "TEXT")
    private String approvedStructureJson;

    @Column(name = "formatting_rules_json", columnDefinition = "TEXT")
    private String formattingRulesJson;

    @Column(name = "uncertain_items_json", columnDefinition = "TEXT")
    private String uncertainItemsJson;

    @Column(name = "uploaded_at", nullable = false)
    private OffsetDateTime uploadedAt;

    @Column(name = "approved_at")
    private OffsetDateTime approvedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approved_by_user_id")
    private User approvedByUser;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        OffsetDateTime now = OffsetDateTime.now();
        if (uploadedAt == null) {
            uploadedAt = now;
        }
        if (createdAt == null) {
            createdAt = now;
        }
        updatedAt = now;
        if (version < 1) {
            version = 1;
        }
        if (source == null) {
            source = "UPLOADED";
        }
        if (status == null) {
            status = "EXTRACTED";
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = OffsetDateTime.now();
    }
}
