package com.researchassistant.document.storage;

import com.researchassistant.document.config.DocumentProperties;
import com.researchassistant.document.exception.DocumentStorageException;

import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * Local development storage implementation.
 *
 * <p>Every storage key is resolved against a configured root and
 * normalized. The final path must remain inside that root, which
 * blocks absolute paths and {@code ../} traversal. Raw upload
 * filenames are never used as path authority.</p>
 */
@Service
public class LocalDocumentStorageService implements DocumentStorageService {

    private final Path rootDirectory;

    public LocalDocumentStorageService(DocumentProperties properties) {
        this.rootDirectory = Path.of(properties.storage().localDirectory())
                .toAbsolutePath()
                .normalize();
    }

    @Override
    public StoredDocumentObject store(
            String storageKey,
            InputStream inputStream
    ) {
        Path target = resolveInsideRoot(storageKey);

        try {
            Files.createDirectories(target.getParent());
            MessageDigest digest = MessageDigest.getInstance("SHA-256");

            long bytes;
            try (DigestInputStream digestInputStream =
                         new DigestInputStream(inputStream, digest)) {
                bytes = Files.copy(digestInputStream, target);
            }

            return new StoredDocumentObject(
                    storageKey,
                    bytes,
                    HexFormat.of().formatHex(digest.digest())
            );
        } catch (IOException | NoSuchAlgorithmException exception) {
            throw new DocumentStorageException(
                    "Unable to store document.",
                    exception
            );
        }
    }

    @Override
    public DocumentStorageObject open(String storageKey) {
        Path target = resolveInsideRoot(storageKey);

        try {
            return new DocumentStorageObject(
                    Files.newInputStream(target),
                    Files.size(target)
            );
        } catch (IOException exception) {
            throw new DocumentStorageException(
                    "Unable to open document.",
                    exception
            );
        }
    }

    @Override
    public boolean exists(String storageKey) {
        return Files.exists(resolveInsideRoot(storageKey));
    }

    @Override
    public void delete(String storageKey) {
        try {
            Files.deleteIfExists(resolveInsideRoot(storageKey));
        } catch (IOException exception) {
            throw new DocumentStorageException(
                    "Unable to delete document.",
                    exception
            );
        }
    }

    private Path resolveInsideRoot(String storageKey) {
        Path resolved = rootDirectory.resolve(storageKey)
                .toAbsolutePath()
                .normalize();

        if (!resolved.startsWith(rootDirectory)) {
            throw new DocumentStorageException("Invalid storage key.");
        }

        return resolved;
    }
}
