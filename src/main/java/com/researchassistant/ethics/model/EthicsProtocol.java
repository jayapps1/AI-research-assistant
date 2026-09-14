package com.researchassistant.ethics.model;

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
@Table(name = "ethics_protocols", indexes = @Index(name = "idx_ethics_protocols_project", columnList = "project_id"))
@Getter @Setter @NoArgsConstructor
public class EthicsProtocol {
    public enum Status { DRAFT, SUBMITTED, APPROVED, CONDITIONALLY_APPROVED, REJECTED, EXPIRED, WITHDRAWN }

    @Id @Column(nullable = false, updatable = false)
    private UUID id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "project_id", nullable = false)
    private ResearchProject project;
    @Column(nullable = false, length = 255)
    private String title;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 40)
    private Status status = Status.DRAFT;
    @Column(name = "institution_name", length = 255)
    private String institutionName;
    @Column(name = "review_board_name", length = 255)
    private String reviewBoardName;
    @Column(name = "protocol_reference", length = 255)
    private String protocolReference;
    @Column(name = "risk_level", length = 100)
    private String riskLevel;
    @Column(name = "risk_description", columnDefinition = "TEXT")
    private String riskDescription;
    @Column(name = "confidentiality_plan", columnDefinition = "TEXT")
    private String confidentialityPlan;
    @Column(name = "data_protection_plan", columnDefinition = "TEXT")
    private String dataProtectionPlan;
    @Column(name = "retention_plan", columnDefinition = "TEXT")
    private String retentionPlan;
    @Column(name = "withdrawal_procedure", columnDefinition = "TEXT")
    private String withdrawalProcedure;
    @Column(name = "vulnerable_population_considerations", columnDefinition = "TEXT")
    private String vulnerablePopulationConsiderations;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 30)
    private ContentOrigin origin = ContentOrigin.USER;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "created_by", nullable = false)
    private User createdBy;
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;
    @PrePersist void onCreate() { if (id == null) id = UUID.randomUUID(); OffsetDateTime now = OffsetDateTime.now(); if (createdAt == null) createdAt = now; updatedAt = now; if (status == null) status = Status.DRAFT; if (origin == null) origin = ContentOrigin.USER; }
    @PreUpdate void onUpdate() { updatedAt = OffsetDateTime.now(); }
}
