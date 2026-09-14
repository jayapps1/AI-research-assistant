package com.researchassistant.operations.backup;

import com.researchassistant.document.config.DocumentProperties;

import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@Component
public class LocalDocumentStorageBackupProvider
        implements ObjectStorageBackupProvider {

    private final Path documentRoot;

    public LocalDocumentStorageBackupProvider(DocumentProperties properties) {
        this.documentRoot = Path.of(properties.storage().localDirectory())
                .toAbsolutePath()
                .normalize();
    }

    @Override
    public BackupArtifactDescriptor createSnapshotOrArchive(BackupRun backupRun) {
        long objectCount = countRegularFiles();
        return new BackupArtifactDescriptor(
                BackupArtifactType.OBJECT_STORAGE_MANIFEST,
                "local-document-storage://" + documentRoot,
                null,
                null,
                objectCount
        );
    }

    @Override
    public BackupVerificationOutcome verify(BackupArtifactDescriptor artifact) {
        if (!Files.isDirectory(documentRoot)) {
            return BackupVerificationOutcome.invalid(
                    "DOCUMENT_STORAGE_ROOT_MISSING",
                    "Configured document storage root is not available."
            );
        }
        return BackupVerificationOutcome.valid(
                "Configured local document storage root is readable."
        );
    }

    @Override
    public BackupProviderCapabilities describeCapabilities() {
        return new BackupProviderCapabilities(
                "local-document-storage",
                false,
                false,
                false,
                false,
                "Application provider records storage metadata; ops scripts create local archives."
        );
    }

    private long countRegularFiles() {
        if (!Files.isDirectory(documentRoot)) {
            return 0L;
        }
        try (var stream = Files.walk(documentRoot)) {
            return stream.filter(Files::isRegularFile).count();
        } catch (IOException exception) {
            throw new IllegalStateException(
                    "Unable to inspect document storage root.",
                    exception
            );
        }
    }
}
