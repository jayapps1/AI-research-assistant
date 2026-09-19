package com.researchassistant.common.storage;

public record StoredObject(
        String key,
        long sizeBytes,
        String checksumSha256
) {
}
