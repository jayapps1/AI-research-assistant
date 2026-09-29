package com.researchassistant.common.storage;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class StorageObjectMetadataService {

    private final StorageObjectRepository repository;
    private final ObjectStorageService objectStorageService;

    public StorageObjectMetadataService(
            StorageObjectRepository repository,
            ObjectStorageService objectStorageService
    ) {
        this.repository = repository;
        this.objectStorageService = objectStorageService;
    }

    @Transactional
    public StorageObjectEntity createPending(
            UUID ownerUserId,
            UUID workspaceId,
            UUID projectId,
            StorageObjectCategory category,
            String storageKey,
            String originalFilename,
            String storedFilename,
            String mediaType,
            long sizeBytes,
            String checksumSha256
    ) {
        StorageObjectEntity object = new StorageObjectEntity();
        object.setOwnerUserId(ownerUserId);
        object.setWorkspaceId(workspaceId);
        object.setProjectId(projectId);
        object.setProvider(objectStorageService.provider());
        object.setCategory(category);
        object.setStorageKey(storageKey);
        object.setOriginalFilename(originalFilename);
        object.setStoredFilename(storedFilename);
        object.setMediaType(mediaType);
        object.setSizeBytes(Math.max(0L, sizeBytes));
        object.setChecksumSha256(checksumSha256);
        object.setStatus(StorageObjectStatus.PENDING);
        return repository.saveAndFlush(object);
    }

    @Transactional
    public StorageObjectEntity markAvailable(UUID storageObjectId, StoredObject stored) {
        StorageObjectEntity object = repository.findById(storageObjectId)
                .orElseThrow(() -> new StorageException("STORAGE_METADATA_NOT_FOUND", "Storage metadata was not found."));
        object.setProviderFileId(stored.providerFileId());
        object.setProviderParentId(stored.providerParentId());
        object.setSizeBytes(stored.sizeBytes());
        object.setChecksumSha256(stored.checksumSha256());
        object.setFailureCode(null);
        object.setFailureMessage(null);
        object.setStatus(StorageObjectStatus.AVAILABLE);
        return repository.saveAndFlush(object);
    }

    @Transactional
    public void markFailed(UUID storageObjectId, String code, String message) {
        repository.findById(storageObjectId).ifPresent(object -> {
            object.setStatus(StorageObjectStatus.FAILED);
            object.setFailureCode(shortValue(code, 100));
            object.setFailureMessage(shortValue(message, 1000));
            repository.saveAndFlush(object);
        });
    }

    @Transactional
    public void markTrashed(StorageObjectEntity object) {
        if (object != null && object.getStatus() == StorageObjectStatus.AVAILABLE) {
            object.setStatus(StorageObjectStatus.TRASHED);
        }
    }

    @Transactional
    public void markAvailable(StorageObjectEntity object) {
        if (object != null && object.getStatus() == StorageObjectStatus.TRASHED) {
            object.setStatus(StorageObjectStatus.AVAILABLE);
        }
    }

    @Transactional
    public void markDeleted(StorageObjectEntity object) {
        if (object != null) {
            object.setStatus(StorageObjectStatus.DELETED);
        }
    }

    public long failedUploadCount() {
        return repository.countByStatus(StorageObjectStatus.FAILED);
    }

    private String shortValue(String value, int maxLength) {
        if (value == null) {
            return null;
        }
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
    }
}
