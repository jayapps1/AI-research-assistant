package com.researchassistant.common.storage;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(
        name = "storage_objects",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_storage_objects_provider_key",
                        columnNames = {"provider", "storage_key"}
                )
        },
        indexes = {
                @Index(name = "idx_storage_objects_owner", columnList = "owner_user_id"),
                @Index(name = "idx_storage_objects_workspace", columnList = "workspace_id"),
                @Index(name = "idx_storage_objects_project", columnList = "project_id"),
                @Index(name = "idx_storage_objects_provider_file", columnList = "provider,provider_file_id"),
                @Index(name = "idx_storage_objects_status", columnList = "status")
        }
)
@Getter
@Setter
@NoArgsConstructor
public class StorageObjectEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "owner_user_id", nullable = false)
    private UUID ownerUserId;

    @Column(name = "workspace_id")
    private UUID workspaceId;

    @Column(name = "project_id")
    private UUID projectId;

    @Enumerated(EnumType.STRING)
    @Column(name = "provider", nullable = false, length = 40)
    private StorageProvider provider;

    @Column(name = "provider_file_id", length = 500)
    private String providerFileId;

    @Column(name = "provider_parent_id", length = 500)
    private String providerParentId;

    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false, length = 60)
    private StorageObjectCategory category;

    @Column(name = "storage_key", nullable = false, length = 1000)
    private String storageKey;

    @Column(name = "original_filename", length = 500)
    private String originalFilename;

    @Column(name = "stored_filename", nullable = false, length = 500)
    private String storedFilename;

    @Column(name = "media_type", length = 255)
    private String mediaType;

    @Column(name = "size_bytes", nullable = false)
    private long sizeBytes;

    @Column(name = "checksum_sha256", length = 64)
    private String checksumSha256;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 40)
    private StorageObjectStatus status = StorageObjectStatus.PENDING;

    @Column(name = "failure_code", length = 100)
    private String failureCode;

    @Column(name = "failure_message", length = 1000)
    private String failureMessage;

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
            status = StorageObjectStatus.PENDING;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = OffsetDateTime.now();
    }
}
