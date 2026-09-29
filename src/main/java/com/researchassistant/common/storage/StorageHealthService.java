package com.researchassistant.common.storage;

import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class StorageHealthService {

    private final ObjectStorageService objectStorageService;
    private final StorageObjectMetadataService metadataService;

    private volatile OffsetDateTime lastSuccessfulCheck;

    public StorageHealthService(
            ObjectStorageService objectStorageService,
            StorageObjectMetadataService metadataService
    ) {
        this.objectStorageService = objectStorageService;
        this.metadataService = metadataService;
    }

    public Map<String, Object> health() {
        ObjectStorageHealthSnapshot snapshot = objectStorageService.health();
        if (snapshot.available()) {
            lastSuccessfulCheck = OffsetDateTime.now();
        }

        Map<String, Object> map = new LinkedHashMap<>();
        map.put("provider", snapshot.provider().name());
        map.put("configured", snapshot.configured());
        map.put("available", snapshot.available());
        map.put("lastSuccessfulCheck", lastSuccessfulCheck);
        map.put("failedUploadCount", metadataService.failedUploadCount());
        map.put("detail", snapshot.detail());
        map.put("credentialsExposed", false);
        return map;
    }
}
