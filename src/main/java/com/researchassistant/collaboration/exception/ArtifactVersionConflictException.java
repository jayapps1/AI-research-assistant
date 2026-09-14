package com.researchassistant.collaboration.exception;

import java.util.UUID;

public class ArtifactVersionConflictException extends RuntimeException {
    private final UUID artifactId;
    private final Long expectedVersion;
    private final Long currentVersion;

    public ArtifactVersionConflictException(UUID artifactId, Long expectedVersion, Long currentVersion) {
        super("ARTIFACT_VERSION_CONFLICT");
        this.artifactId = artifactId;
        this.expectedVersion = expectedVersion;
        this.currentVersion = currentVersion;
    }

    public UUID artifactId() { return artifactId; }
    public Long expectedVersion() { return expectedVersion; }
    public Long currentVersion() { return currentVersion; }
}
