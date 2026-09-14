package com.researchassistant.ethics.model;

import com.researchassistant.identity.entity.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "ethics_approvals", indexes = @Index(name = "idx_ethics_approvals_protocol", columnList = "protocol_id"))
@Getter @Setter @NoArgsConstructor
public class EthicsApproval {
    @Id @Column(nullable = false, updatable = false)
    private UUID id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "protocol_id", nullable = false)
    private EthicsProtocol protocol;
    @Column(name = "approval_reference", nullable = false, length = 255)
    private String approvalReference;
    @Column(name = "approval_date")
    private LocalDate approvalDate;
    @Column(name = "expiry_date")
    private LocalDate expiryDate;
    @Column(name = "approving_body", length = 255)
    private String approvingBody;
    @Column(columnDefinition = "TEXT")
    private String conditions;
    @Column(columnDefinition = "TEXT")
    private String notes;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "recorded_by", nullable = false)
    private User recordedBy;
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;
    @PrePersist void onCreate() { if (id == null) id = UUID.randomUUID(); if (createdAt == null) createdAt = OffsetDateTime.now(); }
}
