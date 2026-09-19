package com.researchassistant.common.storage;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

@Service
public class LocalObjectStorageService implements ObjectStorageService {

    private final Path rootDirectory;

    public LocalObjectStorageService(
            @Value("${app.profile-image.storage-directory:./data/profiles}") String storageDirectory
    ) {
        this.rootDirectory = Path.of(storageDirectory).toAbsolutePath().normalize();
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
                bytes = Files.copy(digestInputStream, target, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
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
        Path target = resolveInsideRoot(key);

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
        return Files.exists(resolveInsideRoot(key));
    }

    @Override
    public void delete(String key) {
        try {
            Files.deleteIfExists(resolveInsideRoot(key));
        } catch (IOException exception) {
            throw new StorageException("Unable to delete object: " + key, exception);
        }
    }

    private Path resolveInsideRoot(String key) {
        if (key == null || key.isBlank() || key.contains("..")) {
            throw new StorageException("Invalid storage key: " + key);
        }

        Path resolved = rootDirectory.resolve(key).toAbsolutePath().normalize();
        if (!resolved.startsWith(rootDirectory)) {
            throw new StorageException("Invalid storage key traversal: " + key);
        }

        return resolved;
    }
}
