package com.researchassistant.project.entity;

import com.researchassistant.identity.entity.User;
import com.researchassistant.workspace.entity.Workspace;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Workspace-scoped research project.
 *
 * <p>Projects are below workspaces in the tenant hierarchy. Code
 * must not fetch a project by UUID and expose it without proving
 * access to the parent workspace and then project-level access where
 * required.</p>
 *
 * <p>{@code nextDocumentNumber} is permanent allocation state for
 * future document identifiers such as DOC-001. It is not a count of
 * current documents and must never be reset, renumbered, derived from
 * document rows, or exposed as user-editable API data.</p>
 */
@Entity
@Table(
        name = "research_projects",
        indexes = {
                @Index(name = "idx_research_projects_workspace_id", columnList = "workspace_id"),
                @Index(name = "idx_research_projects_created_by", columnList = "created_by"),
                @Index(name = "idx_research_projects_status", columnList = "status"),
                @Index(name = "idx_research_projects_workspace_status", columnList = "workspace_id,status")
        }
)
@Getter
@Setter
@NoArgsConstructor
public class ResearchProject {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "workspace_id", nullable = false)
    private Workspace workspace;

    @Column(name = "title", nullable = false, length = 255)
    private String title;

    @Column(name = "description")
    private String description;

    @Column(name = "research_aim", columnDefinition = "TEXT")
    private String researchAim;

    @Column(name = "study_area", length = 255)
    private String studyArea;

    @Column(name = "research_type", length = 100)
    private String researchType;

    @Column(name = "keywords", length = 500)
    private String keywords;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "report_template_id")
    private com.researchassistant.analysis.entity.ResearchReportTemplate reportTemplate;

    @Enumerated(EnumType.STRING)
    @Column(name = "citation_style", length = 60)
    private com.researchassistant.analysis.entity.CitationStyle citationStyle = com.researchassistant.analysis.entity.CitationStyle.APA_7;

    @Column(name = "citation_style_locked", nullable = false)
    private boolean citationStyleLocked;

    @Enumerated(EnumType.STRING)
    @Column(name = "citation_presentation", nullable = false, length = 40)
    private com.researchassistant.analysis.entity.CitationPresentation citationPresentation = com.researchassistant.analysis.entity.CitationPresentation.PARENTHETICAL;

    @Column(name = "bibliography_sort", nullable = false, length = 40)
    private String bibliographySort = "STYLE_DEFAULT";

    @Column(name = "include_doi", nullable = false)
    private boolean includeDoi = true;

    @Column(name = "include_url", nullable = false)
    private boolean includeUrl = true;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private ResearchProjectStatus status = ResearchProjectStatus.DRAFT;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "created_by", nullable = false)
    private User createdBy;

    @Column(name = "next_document_number", nullable = false)
    private long nextDocumentNumber = 1L;

    @Column(name = "next_participant_number", nullable = false)
    private long nextParticipantNumber = 1L;

    @Column(name = "next_session_number", nullable = false)
    private long nextSessionNumber = 1L;

    @Version
    @Column(name = "version")
    private Long version;

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
        if (createdAt == null) {
            createdAt = now;
        }
        updatedAt = now;
        if (status == null) {
            status = ResearchProjectStatus.DRAFT;
        }
        if (citationPresentation == null) {
            citationPresentation = com.researchassistant.analysis.entity.CitationPresentation.PARENTHETICAL;
        }
        if (bibliographySort == null || bibliographySort.isBlank()) {
            bibliographySort = "STYLE_DEFAULT";
        }
        if (nextDocumentNumber < 1L) {
            nextDocumentNumber = 1L;
        }
        if (nextParticipantNumber < 1L) {
            nextParticipantNumber = 1L;
        }
        if (nextSessionNumber < 1L) {
            nextSessionNumber = 1L;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = OffsetDateTime.now();
    }
}
