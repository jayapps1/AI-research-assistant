package com.researchassistant.common.storage;

public record StoredObject(
        String key,
        long sizeBytes,
        String checksumSha256,
        String providerFileId,
        String providerParentId
) {
    public StoredObject(String key, long sizeBytes, String checksumSha256) {
        this(key, sizeBytes, checksumSha256, key, null);
    }
}
