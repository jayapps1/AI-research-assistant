package com.researchassistant.common.storage;

import java.io.InputStream;

/**
 * Storage abstraction for binary assets such as user profile images.
 * Implementations enforce strict path resolution and key control.
 */
public interface ObjectStorageService {

    StoredObject store(String key, InputStream inputStream);

    StorageObject open(String key);

    boolean exists(String key);

    void delete(String key);
}
