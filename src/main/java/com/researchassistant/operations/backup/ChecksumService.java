package com.researchassistant.operations.backup;

import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.io.OutputStream;

@Service
public class ChecksumService {

    public ChecksumService() {
    }

    public String sha256(Path path) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            try (InputStream inputStream = Files.newInputStream(path);
                 DigestInputStream digestInputStream =
                         new DigestInputStream(inputStream, digest)) {
                digestInputStream.transferTo(OutputStream.nullOutputStream());
            }
            return HexFormat.of().formatHex(digest.digest());
        } catch (IOException | NoSuchAlgorithmException exception) {
            throw new IllegalStateException("Unable to checksum file.", exception);
        }
    }

    public String sha256(BackupManifest manifest) {
        try {
            byte[] bytes = canonicalManifestString(manifest)
                    .getBytes(java.nio.charset.StandardCharsets.UTF_8);
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(bytes));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException(
                    "Unable to checksum backup manifest.",
                    exception
            );
        }
    }

    private String canonicalManifestString(BackupManifest manifest) {
        return String.join(
                "\n",
                stringValue(manifest.backupRunId()),
                stringValue(manifest.createdAt()),
                stringValue(manifest.applicationVersion()),
                stringValue(manifest.flywaySchemaVersion()),
                stringValue(manifest.databaseBackupReference()),
                stringValue(manifest.databaseChecksum()),
                stringValue(manifest.objectStorageBackupReference()),
                stringValue(manifest.objectStorageChecksum()),
                stringValue(manifest.objectCount()),
                stringValue(manifest.backupPolicyName()),
                Boolean.toString(manifest.encrypted()),
                stringValue(manifest.verificationStatus())
        );
    }

    private String stringValue(Object value) {
        return value == null ? "" : value.toString();
    }
}
