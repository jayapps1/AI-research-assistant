package com.researchassistant.document.storage;

public record StoredDocumentObject(
        String storageKey,
        long fileSizeBytes,
        String checksumSha256,
        String providerFileId,
        String providerParentId
) {
    public StoredDocumentObject(String storageKey, long fileSizeBytes, String checksumSha256) {
        this(storageKey, fileSizeBytes, checksumSha256, storageKey, null);
    }
}
