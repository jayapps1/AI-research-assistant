package com.researchassistant.common.storage;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;

@ConfigurationProperties(prefix = "app.storage")
public record ObjectStorageProperties(
        StorageProvider provider,
        String localDirectory,
        String legacyProfileImageDirectory,
        GoogleDrive googleDrive
) {

    public StorageProvider effectiveProvider() {
        return provider == null ? StorageProvider.LOCAL : provider;
    }

    public String effectiveLocalDirectory() {
        return hasText(localDirectory) ? localDirectory : "./data/documents";
    }

    public String effectiveLegacyProfileImageDirectory() {
        return hasText(legacyProfileImageDirectory)
                ? legacyProfileImageDirectory
                : "./data/profiles";
    }

    public GoogleDrive effectiveGoogleDrive() {
        return googleDrive == null ? GoogleDrive.defaults() : googleDrive;
    }

    public record GoogleDrive(
            String rootFolderId,
            String clientEmail,
            String privateKey,
            String privateKeyBase64,
            String privateKeyId,
            String tokenUri,
            String apiBaseUrl,
            String uploadBaseUrl,
            boolean supportsAllDrives,
            Duration connectTimeout,
            Duration requestTimeout,
            int maxRetries
    ) {
        static GoogleDrive defaults() {
            return new GoogleDrive(
                    "",
                    "",
                    "",
                    "",
                    "",
                    "https://oauth2.googleapis.com/token",
                    "https://www.googleapis.com/drive/v3",
                    "https://www.googleapis.com/upload/drive/v3",
                    false,
                    Duration.ofSeconds(10),
                    Duration.ofSeconds(60),
                    3
            );
        }

        public boolean configured() {
            return hasText(rootFolderId)
                    && hasText(clientEmail)
                    && (hasText(privateKey) || hasText(privateKeyBase64));
        }

        public String effectivePrivateKey() {
            if (hasText(privateKey)) {
                return privateKey.replace("\\n", "\n");
            }
            if (hasText(privateKeyBase64)) {
                return new String(Base64.getDecoder().decode(privateKeyBase64), StandardCharsets.UTF_8);
            }
            return "";
        }

        public String effectiveTokenUri() {
            return hasText(tokenUri) ? tokenUri : "https://oauth2.googleapis.com/token";
        }

        public String effectiveApiBaseUrl() {
            return trimTrailingSlash(hasText(apiBaseUrl) ? apiBaseUrl : "https://www.googleapis.com/drive/v3");
        }

        public String effectiveUploadBaseUrl() {
            return trimTrailingSlash(hasText(uploadBaseUrl) ? uploadBaseUrl : "https://www.googleapis.com/upload/drive/v3");
        }

        public Duration effectiveConnectTimeout() {
            return connectTimeout == null ? Duration.ofSeconds(10) : connectTimeout;
        }

        public Duration effectiveRequestTimeout() {
            return requestTimeout == null ? Duration.ofSeconds(60) : requestTimeout;
        }

        public int effectiveMaxRetries() {
            return maxRetries <= 0 ? 3 : Math.min(maxRetries, 5);
        }
    }

    static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    static String trimTrailingSlash(String value) {
        if (value == null || value.isBlank()) {
            return value;
        }
        String trimmed = value.trim();
        while (trimmed.endsWith("/")) {
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        }
        return trimmed;
    }
}
