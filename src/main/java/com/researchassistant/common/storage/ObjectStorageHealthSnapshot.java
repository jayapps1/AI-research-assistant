package com.researchassistant.common.storage;

public record ObjectStorageHealthSnapshot(
        StorageProvider provider,
        boolean configured,
        boolean available,
        String detail
) {

    public static ObjectStorageHealthSnapshot up(StorageProvider provider, String detail) {
        return new ObjectStorageHealthSnapshot(provider, true, true, detail);
    }

    public static ObjectStorageHealthSnapshot down(StorageProvider provider, boolean configured, String detail) {
        return new ObjectStorageHealthSnapshot(provider, configured, false, detail);
    }
}
