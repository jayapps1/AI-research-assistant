package com.researchassistant.common.storage;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

@Service
@ConditionalOnProperty(prefix = "app.storage", name = "provider", havingValue = "LOCAL", matchIfMissing = true)
public class LocalObjectStorageService implements ObjectStorageService {

    private final Path rootDirectory;
    private final Path legacyProfileImageDirectory;

    public LocalObjectStorageService(ObjectStorageProperties properties) {
        this.rootDirectory = Path.of(properties.effectiveLocalDirectory())
                .toAbsolutePath()
                .normalize();
        this.legacyProfileImageDirectory = Path.of(properties.effectiveLegacyProfileImageDirectory())
                .toAbsolutePath()
                .normalize();
    }

    @Override
    public StoredObject store(String key, InputStream inputStream) {
        Path target = resolveInsideRoot(key);

        try {
            if (target.getParent() != null) {
                Files.createDirectories(target.getParent());
            }
            MessageDigest digest = MessageDigest.getInstance("SHA-256");

            long bytes;
            try (DigestInputStream digestInputStream = new DigestInputStream(inputStream, digest)) {
                bytes = Files.copy(digestInputStream, target, StandardCopyOption.REPLACE_EXISTING);
            }

            return new StoredObject(
                    key,
                    bytes,
                    HexFormat.of().formatHex(digest.digest())
            );
        } catch (IOException | NoSuchAlgorithmException exception) {
            throw new StorageException("Unable to store object: " + key, exception);
        }
    }

    @Override
    public StorageObject open(String key) {
        Path target = resolveExisting(key);

        try {
            return new StorageObject(
                    Files.newInputStream(target),
                    Files.size(target)
            );
        } catch (IOException exception) {
            throw new StorageException("Unable to open object: " + key, exception);
        }
    }

    @Override
    public boolean exists(String key) {
        return Files.exists(resolveExisting(key));
    }

    @Override
    public void delete(String key) {
        try {
            Files.deleteIfExists(resolveExisting(key));
        } catch (IOException exception) {
            throw new StorageException("Unable to delete object: " + key, exception);
        }
    }

    @Override
    public StorageProvider provider() {
        return StorageProvider.LOCAL;
    }

    @Override
    public ObjectStorageHealthSnapshot health() {
        try {
            Files.createDirectories(rootDirectory);
            if (!Files.isDirectory(rootDirectory)
                    || !Files.isReadable(rootDirectory)
                    || !Files.isWritable(rootDirectory)) {
                return ObjectStorageHealthSnapshot.down(
                        StorageProvider.LOCAL,
                        true,
                        "Local object storage root is not readable and writable."
                );
            }
            return ObjectStorageHealthSnapshot.up(
                    StorageProvider.LOCAL,
                    "Local object storage root is available."
            );
        } catch (RuntimeException | IOException exception) {
            return ObjectStorageHealthSnapshot.down(
                    StorageProvider.LOCAL,
                    true,
                    "Local object storage root is unavailable."
            );
        }
    }

    private Path resolveInsideRoot(String key) {
        if (key == null || key.isBlank()) {
            throw new StorageException("Invalid storage key: " + key);
        }
        for (String part : key.replace('\\', '/').split("/")) {
            if (part.isBlank() || ".".equals(part) || "..".equals(part)) {
                throw new StorageException("Invalid storage key: " + key);
            }
        }

        Path resolved = rootDirectory.resolve(key).toAbsolutePath().normalize();
        if (!resolved.startsWith(rootDirectory)) {
            throw new StorageException("Invalid storage key traversal: " + key);
        }

        return resolved;
    }

    private Path resolveExisting(String key) {
        Path primary = resolveInsideRoot(key);
        if (Files.exists(primary)) {
            return primary;
        }

        Path legacy = legacyProfileImageDirectory.resolve(key)
                .toAbsolutePath()
                .normalize();
        if (legacy.startsWith(legacyProfileImageDirectory) && Files.exists(legacy)) {
            return legacy;
        }

        return primary;
    }
}
