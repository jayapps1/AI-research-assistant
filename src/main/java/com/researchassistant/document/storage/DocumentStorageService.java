package com.researchassistant.document.storage;

import java.io.InputStream;

/**
 * Storage boundary for uploaded document binaries.
 *
 * <p>Domain services depend on this abstraction rather than local
 * disk, MinIO or S3 directly. Implementations must treat storage
 * keys as controlled application data and must not use raw user
 * filenames as filesystem or object-storage paths.</p>
 */
public interface DocumentStorageService {

    StoredDocumentObject store(String storageKey, InputStream inputStream);

    DocumentStorageObject open(String storageKey);

    boolean exists(String storageKey);

    void delete(String storageKey);
}
