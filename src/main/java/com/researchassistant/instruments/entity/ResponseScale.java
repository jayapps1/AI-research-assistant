package com.researchassistant.instruments.entity;

import com.researchassistant.project.entity.ResearchProject;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "response_scales", indexes = @Index(name = "idx_response_scales_project", columnList = "project_id"))
@Getter @Setter @NoArgsConstructor
public class ResponseScale {
    @Id @Column(nullable = false, updatable = false) private UUID id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "project_id", nullable = false) private ResearchProject project;
    @Column(nullable = false, length = 255) private String name;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 40) private ResponseScaleType type = ResponseScaleType.OTHER;
    @Column(name = "minimum_value") private Integer minimumValue;
    @Column(name = "maximum_value") private Integer maximumValue;
    @Column(name = "created_at", nullable = false, updatable = false) private OffsetDateTime createdAt;
    @PrePersist void onCreate() { if (id == null) id = UUID.randomUUID(); if (createdAt == null) createdAt = OffsetDateTime.now(); if (type == null) type = ResponseScaleType.OTHER; }
}
