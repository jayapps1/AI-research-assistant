package com.researchassistant.document.storage;

import com.researchassistant.common.storage.StorageHealthService;

import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class DocumentStorageHealthIndicator implements HealthIndicator {

    private final StorageHealthService storageHealthService;

    public DocumentStorageHealthIndicator(StorageHealthService storageHealthService) {
        this.storageHealthService = storageHealthService;
    }

    @Override
    public Health health() {
        Map<String, Object> details = storageHealthService.health();
        boolean available = Boolean.TRUE.equals(details.get("available"));
        Health.Builder builder = available ? Health.up() : Health.down();
        for (Map.Entry<String, Object> entry : details.entrySet()) {
            builder.withDetail(entry.getKey(), entry.getValue());
        }
        return builder.build();
    }
}
