package com.researchassistant.document.storage;

public record StoredDocumentObject(
        String storageKey,
        long fileSizeBytes,
        String checksumSha256
) {
}
