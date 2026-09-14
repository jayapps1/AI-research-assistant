package com.researchassistant.operations.backup;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "app.backup")
public record BackupProperties(
        boolean enabled,
        String rootDirectory,
        Retention retention,
        Verification verification,
        Target target
) {

    public BackupProperties {
        if (rootDirectory == null || rootDirectory.isBlank()) {
            rootDirectory = "./data/backups";
        }
        if (retention == null) {
            retention = new Retention(7, 4, 12);
        }
        if (verification == null) {
            verification = new Verification(true, Duration.ofHours(6));
        }
        if (target == null) {
            target = new Target(1440, 240);
        }
    }

    public record Retention(int daily, int weekly, int monthly) {
    }

    public record Verification(boolean required, Duration freshnessGrace) {
    }

    public record Target(Integer rpoMinutes, Integer rtoMinutes) {
    }
}
