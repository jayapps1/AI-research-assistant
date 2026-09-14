package com.researchassistant.document.storage;

import com.researchassistant.document.config.DocumentProperties;

import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;

@Component
public class DocumentStorageHealthIndicator implements HealthIndicator {

    private final Path rootDirectory;

    public DocumentStorageHealthIndicator(DocumentProperties properties) {
        this.rootDirectory = Path.of(properties.storage().localDirectory())
                .toAbsolutePath()
                .normalize();
    }

    @Override
    public Health health() {
        try {
            Files.createDirectories(rootDirectory);
            if (!Files.isDirectory(rootDirectory)
                    || !Files.isReadable(rootDirectory)
                    || !Files.isWritable(rootDirectory)) {
                return Health.down()
                        .withDetail("storage", "local-document-storage")
                        .withDetail("reason", "Document storage root is not readable and writable.")
                        .build();
            }
            return Health.up()
                    .withDetail("storage", "local-document-storage")
                    .withDetail("usableSpaceBytes", rootDirectory.toFile().getUsableSpace())
                    .build();
        } catch (RuntimeException | java.io.IOException exception) {
            return Health.down(exception)
                    .withDetail("storage", "local-document-storage")
                    .build();
        }
    }
}
