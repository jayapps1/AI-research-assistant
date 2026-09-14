package com.researchassistant.methodology.entity;

import com.researchassistant.common.enums.ContentOrigin;
import com.researchassistant.identity.entity.User;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "data_collection_methods", indexes = @Index(name = "idx_data_collection_methods_methodology", columnList = "methodology_id"))
@Getter @Setter @NoArgsConstructor
public class DataCollectionMethod {
    @Id @Column(nullable = false, updatable = false)
    private UUID id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "methodology_id", nullable = false)
    private Methodology methodology;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 50)
    private DataCollectionMethodType type = DataCollectionMethodType.OTHER;
    @Column(nullable = false, length = 255)
    private String name;
    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;
    @Column(columnDefinition = "TEXT")
    private String rationale;
    @Enumerated(EnumType.STRING) @Column(name = "source_type", nullable = false, length = 30)
    private DataSourceType sourceType = DataSourceType.PRIMARY;
    @Column(name = "administration_mode", length = 255)
    private String administrationMode;
    @Column(columnDefinition = "TEXT")
    private String setting;
    @Column(length = 255)
    private String timing;
    @Column(name = "primary_method", nullable = false)
    private boolean primaryMethod;
    @Column(name = "display_order", nullable = false)
    private int displayOrder = 1;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 30)
    private ContentOrigin origin = ContentOrigin.USER;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "created_by", nullable = false)
    private User createdBy;
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;
    @PrePersist void onCreate() { if (id == null) id = UUID.randomUUID(); OffsetDateTime now = OffsetDateTime.now(); if (createdAt == null) createdAt = now; updatedAt = now; if (origin == null) origin = ContentOrigin.USER; if (type == null) type = DataCollectionMethodType.OTHER; if (sourceType == null) sourceType = DataSourceType.PRIMARY; if (displayOrder < 1) displayOrder = 1; }
    @PreUpdate void onUpdate() { updatedAt = OffsetDateTime.now(); }
}
