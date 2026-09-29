package com.researchassistant.common.storage;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.DigestInputStream;
import java.security.KeyFactory;
import java.security.MessageDigest;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.spec.PKCS8EncodedKeySpec;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

@Service
@ConditionalOnProperty(prefix = "app.storage", name = "provider", havingValue = "GOOGLE_DRIVE")
public class GoogleDriveObjectStorageService implements ObjectStorageService {

    private static final String DRIVE_SCOPE = "https://www.googleapis.com/auth/drive";
    private static final String FOLDER_MIME_TYPE = "application/vnd.google-apps.folder";

    private final ObjectStorageProperties.GoogleDrive properties;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;
    private final Map<String, String> folderCache = new ConcurrentHashMap<>();

    private volatile String accessToken;
    private volatile Instant accessTokenExpiresAt = Instant.EPOCH;

    public GoogleDriveObjectStorageService(
            ObjectStorageProperties properties,
            ObjectMapper objectMapper
    ) {
        this(properties, objectMapper, HttpClient.newBuilder()
                .connectTimeout(properties.effectiveGoogleDrive().effectiveConnectTimeout())
                .build());
    }

    GoogleDriveObjectStorageService(
            ObjectStorageProperties properties,
            ObjectMapper objectMapper,
            HttpClient httpClient
    ) {
        this.properties = properties.effectiveGoogleDrive();
        this.objectMapper = objectMapper;
        this.httpClient = httpClient;
    }

    @Override
    public StoredObject store(String key, InputStream inputStream) {
        return store(key, inputStream, "application/octet-stream");
    }

    @Override
    public StoredObject store(String key, InputStream inputStream, String mediaType) {
        requireConfigured();
        validateKey(key);
        try {
            byte[] bytes = readAndClose(inputStream);
            String checksum = sha256(bytes);
            String parentId = ensureFolderPath(parentSegments(key));
            String existingId = findFileIdByStorageKey(key, parentId);
            if (existingId != null) {
                return new StoredObject(key, bytes.length, checksum, existingId, parentId);
            }

            String fileId = uploadFile(
                    key,
                    leafName(key),
                    parentId,
                    mediaType == null || mediaType.isBlank() ? "application/octet-stream" : mediaType,
                    bytes
            );
            return new StoredObject(key, bytes.length, checksum, fileId, parentId);
        } catch (StorageException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new StorageException(
                    "GOOGLE_DRIVE_UPLOAD_FAILED",
                    "Google Drive upload failed.",
                    exception
            );
        }
    }

    @Override
    public StorageObject open(String key) {
        requireConfigured();
        validateKey(key);
        try {
            DriveFile file = requireFileByStorageKey(key);
            HttpRequest request = authorizedRequest(downloadUri(file.id()))
                    .GET()
                    .build();
            HttpResponse<byte[]> response = sendWithRetry(request, "GOOGLE_DRIVE_DOWNLOAD_FAILED");
            if (response.statusCode() == 404) {
                throw new StorageException("STORAGE_OBJECT_NOT_FOUND", "Stored object was not found.");
            }
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw driveFailure(response.statusCode(), "GOOGLE_DRIVE_DOWNLOAD_FAILED");
            }
            long size = file.size() == null ? response.body().length : file.size();
            return new StorageObject(new java.io.ByteArrayInputStream(response.body()), size);
        } catch (StorageException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new StorageException(
                    "GOOGLE_DRIVE_DOWNLOAD_FAILED",
                    "Google Drive download failed.",
                    exception
            );
        }
    }

    @Override
    public boolean exists(String key) {
        requireConfigured();
        validateKey(key);
        try {
            return findFileByStorageKey(key) != null;
        } catch (StorageException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new StorageException(
                    "GOOGLE_DRIVE_LOOKUP_FAILED",
                    "Google Drive lookup failed.",
                    exception
            );
        }
    }

    @Override
    public void delete(String key) {
        requireConfigured();
        validateKey(key);
        try {
            DriveFile file = findFileByStorageKey(key);
            if (file == null) {
                return;
            }
            HttpRequest request = authorizedRequest(fileUri(file.id(), ""))
                    .DELETE()
                    .build();
            HttpResponse<byte[]> response = sendWithRetry(request, "GOOGLE_DRIVE_DELETE_FAILED");
            if (response.statusCode() == 404) {
                return;
            }
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw driveFailure(response.statusCode(), "GOOGLE_DRIVE_DELETE_FAILED");
            }
        } catch (StorageException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new StorageException(
                    "GOOGLE_DRIVE_DELETE_FAILED",
                    "Google Drive delete failed.",
                    exception
            );
        }
    }

    @Override
    public StorageProvider provider() {
        return StorageProvider.GOOGLE_DRIVE;
    }

    @Override
    public ObjectStorageHealthSnapshot health() {
        if (!properties.configured()) {
            return ObjectStorageHealthSnapshot.down(
                    StorageProvider.GOOGLE_DRIVE,
                    false,
                    "Google Drive storage is not fully configured."
            );
        }
        try {
            HttpRequest request = authorizedRequest(fileUri(
                    properties.rootFolderId(),
                    "fields=id,name,mimeType,trashed"
            )).GET().build();
            HttpResponse<byte[]> response = sendWithRetry(request, "GOOGLE_DRIVE_HEALTH_FAILED");
            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                return ObjectStorageHealthSnapshot.up(
                        StorageProvider.GOOGLE_DRIVE,
                        "Google Drive root folder is reachable."
                );
            }
            return ObjectStorageHealthSnapshot.down(
                    StorageProvider.GOOGLE_DRIVE,
                    true,
                    "Google Drive root folder check failed."
            );
        } catch (RuntimeException exception) {
            return ObjectStorageHealthSnapshot.down(
                    StorageProvider.GOOGLE_DRIVE,
                    true,
                    "Google Drive storage is unavailable."
            );
        }
    }

    private void requireConfigured() {
        if (!properties.configured()) {
            throw new StorageException(
                    "GOOGLE_DRIVE_NOT_CONFIGURED",
                    "Google Drive storage is not configured."
            );
        }
    }

    private HttpRequest.Builder authorizedRequest(URI uri) {
        return HttpRequest.newBuilder(uri)
                .timeout(properties.effectiveRequestTimeout())
                .header("Authorization", "Bearer " + accessToken())
                .header("Accept", "application/json");
    }

    private String accessToken() {
        Instant now = Instant.now();
        if (accessToken != null && accessTokenExpiresAt.isAfter(now.plusSeconds(60))) {
            return accessToken;
        }
        synchronized (this) {
            now = Instant.now();
            if (accessToken != null && accessTokenExpiresAt.isAfter(now.plusSeconds(60))) {
                return accessToken;
            }
            TokenResponse tokenResponse = requestAccessToken(now);
            accessToken = tokenResponse.accessToken();
            accessTokenExpiresAt = now.plusSeconds(Math.max(60, tokenResponse.expiresIn()));
            return accessToken;
        }
    }

    private TokenResponse requestAccessToken(Instant now) {
        try {
            String assertion = serviceAccountAssertion(now);
            String body = "grant_type="
                    + encode("urn:ietf:params:oauth:grant-type:jwt-bearer")
                    + "&assertion="
                    + encode(assertion);
            HttpRequest request = HttpRequest.newBuilder(URI.create(properties.effectiveTokenUri()))
                    .timeout(properties.effectiveRequestTimeout())
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();
            HttpResponse<byte[]> response = sendWithRetry(request, "GOOGLE_DRIVE_AUTHENTICATION_FAILED", false);
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new StorageException(
                        "GOOGLE_DRIVE_AUTHENTICATION_FAILED",
                        "Google Drive storage could not authenticate."
                );
            }
            Map<String, Object> parsed = readJsonObject(response.body());
            Object token = parsed.get("access_token");
            Object expires = parsed.get("expires_in");
            if (!(token instanceof String tokenValue) || tokenValue.isBlank()) {
                throw new StorageException(
                        "GOOGLE_DRIVE_AUTHENTICATION_FAILED",
                        "Google Drive storage did not return an access token."
                );
            }
            long expiresIn = expires instanceof Number number ? number.longValue() : 3600L;
            return new TokenResponse(tokenValue, expiresIn);
        } catch (StorageException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new StorageException(
                    "GOOGLE_DRIVE_AUTHENTICATION_FAILED",
                    "Google Drive storage could not authenticate.",
                    exception
            );
        }
    }

    private String serviceAccountAssertion(Instant now) throws Exception {
        Map<String, Object> header = properties.privateKeyId() == null || properties.privateKeyId().isBlank()
                ? Map.of("alg", "RS256", "typ", "JWT")
                : Map.of("alg", "RS256", "typ", "JWT", "kid", properties.privateKeyId());
        Map<String, Object> claims = Map.of(
                "iss", properties.clientEmail(),
                "scope", DRIVE_SCOPE,
                "aud", properties.effectiveTokenUri(),
                "iat", now.getEpochSecond(),
                "exp", now.plusSeconds(3600).getEpochSecond()
        );
        String signingInput = base64Url(objectMapper.writeValueAsBytes(header))
                + "."
                + base64Url(objectMapper.writeValueAsBytes(claims));
        Signature signature = Signature.getInstance("SHA256withRSA");
        signature.initSign(privateKey());
        signature.update(signingInput.getBytes(StandardCharsets.UTF_8));
        return signingInput + "." + base64Url(signature.sign());
    }

    private PrivateKey privateKey() throws Exception {
        String pem = properties.effectivePrivateKey()
                .replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replaceAll("\\s", "");
        byte[] der = Base64.getDecoder().decode(pem);
        return KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(der));
    }

    private String ensureFolderPath(List<String> segments) {
        String parentId = properties.rootFolderId();
        String cacheKey = "";
        for (String segment : segments) {
            cacheKey = cacheKey + "/" + segment;
            String cached = folderCache.get(cacheKey);
            if (cached != null) {
                parentId = cached;
                continue;
            }
            String folderId = findFolder(parentId, segment);
            if (folderId == null) {
                folderId = createFolder(parentId, segment);
            }
            folderCache.put(cacheKey, folderId);
            parentId = folderId;
        }
        return parentId;
    }

    private String findFolder(String parentId, String name) {
        try {
            String query = "'%s' in parents and name = '%s' and mimeType = '%s' and trashed = false"
                    .formatted(escapeDriveQuery(parentId), escapeDriveQuery(name), FOLDER_MIME_TYPE);
            HttpRequest request = authorizedRequest(filesListUri(query, "files(id,name)", 1))
                    .GET()
                    .build();
            HttpResponse<byte[]> response = sendWithRetry(request, "GOOGLE_DRIVE_LOOKUP_FAILED");
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw driveFailure(response.statusCode(), "GOOGLE_DRIVE_LOOKUP_FAILED");
            }
            return firstFileId(response.body());
        } catch (StorageException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new StorageException(
                    "GOOGLE_DRIVE_LOOKUP_FAILED",
                    "Google Drive folder lookup failed.",
                    exception
            );
        }
    }

    private String createFolder(String parentId, String name) {
        try {
            byte[] body = objectMapper.writeValueAsBytes(Map.of(
                    "name", name,
                    "mimeType", FOLDER_MIME_TYPE,
                    "parents", List.of(parentId)
            ));
            HttpRequest request = authorizedRequest(filesCreateUri("fields=id"))
                    .header("Content-Type", "application/json; charset=UTF-8")
                    .POST(HttpRequest.BodyPublishers.ofByteArray(body))
                    .build();
            HttpResponse<byte[]> response = sendWithRetry(request, "GOOGLE_DRIVE_PERMISSION_DENIED");
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw driveFailure(response.statusCode(), "GOOGLE_DRIVE_PERMISSION_DENIED");
            }
            Map<String, Object> parsed = readJsonObject(response.body());
            Object id = parsed.get("id");
            if (!(id instanceof String value) || value.isBlank()) {
                throw new StorageException(
                        "GOOGLE_DRIVE_UPLOAD_FAILED",
                        "Google Drive did not return a folder id."
                );
            }
            return value;
        } catch (StorageException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new StorageException(
                    "GOOGLE_DRIVE_UPLOAD_FAILED",
                    "Google Drive folder creation failed.",
                    exception
            );
        }
    }

    private String uploadFile(
            String key,
            String name,
            String parentId,
            String mediaType,
            byte[] bytes
    ) throws IOException {
        String boundary = "ra-storage-" + java.util.UUID.randomUUID();
        byte[] metadata = objectMapper.writeValueAsBytes(Map.of(
                "name", name,
                "parents", List.of(parentId),
                "appProperties", Map.of("storageKey", key)
        ));

        ByteArrayOutputStream body = new ByteArrayOutputStream();
        body.write(("--" + boundary + "\r\n").getBytes(StandardCharsets.UTF_8));
        body.write("Content-Type: application/json; charset=UTF-8\r\n\r\n".getBytes(StandardCharsets.UTF_8));
        body.write(metadata);
        body.write(("\r\n--" + boundary + "\r\n").getBytes(StandardCharsets.UTF_8));
        body.write(("Content-Type: " + mediaType + "\r\n\r\n").getBytes(StandardCharsets.UTF_8));
        body.write(bytes);
        body.write(("\r\n--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8));

        HttpRequest request = authorizedRequest(uploadUri("fields=id"))
                .header("Content-Type", "multipart/related; boundary=" + boundary)
                .POST(HttpRequest.BodyPublishers.ofByteArray(body.toByteArray()))
                .build();
        HttpResponse<byte[]> response = sendWithRetry(request, "GOOGLE_DRIVE_UPLOAD_FAILED");
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw driveFailure(response.statusCode(), "GOOGLE_DRIVE_UPLOAD_FAILED");
        }
        Map<String, Object> parsed = readJsonObject(response.body());
        Object id = parsed.get("id");
        if (!(id instanceof String value) || value.isBlank()) {
            throw new StorageException(
                    "GOOGLE_DRIVE_UPLOAD_FAILED",
                    "Google Drive did not return a file id."
            );
        }
        return value;
    }

    private DriveFile requireFileByStorageKey(String key) {
        DriveFile file = findFileByStorageKey(key);
        if (file == null) {
            throw new StorageException("STORAGE_OBJECT_NOT_FOUND", "Stored object was not found.");
        }
        return file;
    }

    private DriveFile findFileByStorageKey(String key) {
        String parentId = ensureFolderPath(parentSegments(key));
        String fileId = findFileIdByStorageKey(key, parentId);
        if (fileId == null) {
            return null;
        }
        return fileMetadata(fileId);
    }

    private String findFileIdByStorageKey(String key, String parentId) {
        String query = "'%s' in parents and trashed = false and appProperties has { key = 'storageKey' and value = '%s' }"
                .formatted(escapeDriveQuery(parentId), escapeDriveQuery(key));
        HttpRequest request = authorizedRequest(filesListUri(query, "files(id,name,size)", 1))
                .GET()
                .build();
        HttpResponse<byte[]> response = sendWithRetry(request, "GOOGLE_DRIVE_LOOKUP_FAILED");
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw driveFailure(response.statusCode(), "GOOGLE_DRIVE_LOOKUP_FAILED");
        }
        return firstFileId(response.body());
    }

    private DriveFile fileMetadata(String fileId) {
        HttpRequest request = authorizedRequest(fileUri(fileId, "fields=id,name,size,trashed"))
                .GET()
                .build();
        HttpResponse<byte[]> response = sendWithRetry(request, "GOOGLE_DRIVE_LOOKUP_FAILED");
        if (response.statusCode() == 404) {
            return null;
        }
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw driveFailure(response.statusCode(), "GOOGLE_DRIVE_LOOKUP_FAILED");
        }
        Map<String, Object> parsed = readJsonObject(response.body());
        if (Boolean.TRUE.equals(parsed.get("trashed"))) {
            return null;
        }
        Object id = parsed.get("id");
        if (!(id instanceof String value) || value.isBlank()) {
            return null;
        }
        Long size = parseLong(parsed.get("size"));
        return new DriveFile(value, size);
    }

    private String firstFileId(byte[] responseBody) {
        Map<String, Object> parsed = readJsonObject(responseBody);
        Object files = parsed.get("files");
        if (!(files instanceof List<?> list) || list.isEmpty()) {
            return null;
        }
        Object first = list.getFirst();
        if (!(first instanceof Map<?, ?> firstMap)) {
            return null;
        }
        Object id = firstMap.get("id");
        return id instanceof String value && !value.isBlank() ? value : null;
    }

    private HttpResponse<byte[]> sendWithRetry(HttpRequest request, String failureCode) {
        return sendWithRetry(request, failureCode, true);
    }

    private HttpResponse<byte[]> sendWithRetry(
            HttpRequest request,
            String failureCode,
            boolean refreshTokenOnUnauthorized
    ) {
        int attempts = properties.effectiveMaxRetries();
        RuntimeException lastException = null;
        for (int attempt = 1; attempt <= attempts; attempt++) {
            try {
                HttpResponse<byte[]> response = httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());
                if (response.statusCode() == 401 && refreshTokenOnUnauthorized && attempt < attempts) {
                    synchronized (this) {
                        accessToken = null;
                        accessTokenExpiresAt = Instant.EPOCH;
                    }
                    sleep(attempt);
                    continue;
                }
                if (!isTransientStatus(response.statusCode()) || attempt == attempts) {
                    return response;
                }
            } catch (IOException exception) {
                lastException = new StorageException(
                        failureCode,
                        "Google Drive storage request failed.",
                        exception
                );
                if (attempt == attempts) {
                    throw lastException;
                }
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                throw new StorageException(
                        "GOOGLE_DRIVE_TIMEOUT",
                        "Google Drive storage request was interrupted.",
                        exception
                );
            }
            sleep(attempt);
        }
        if (lastException != null) {
            throw lastException;
        }
        throw new StorageException(failureCode, "Google Drive storage request failed.");
    }

    private boolean isTransientStatus(int status) {
        return status == 408 || status == 429 || status == 500 || status == 502 || status == 503 || status == 504;
    }

    private void sleep(int attempt) {
        try {
            Thread.sleep(Math.min(1000L, 150L * attempt));
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new StorageException(
                    "GOOGLE_DRIVE_TIMEOUT",
                    "Google Drive storage retry was interrupted.",
                    exception
            );
        }
    }

    private StorageException driveFailure(int status, String fallbackCode) {
        return switch (status) {
            case 401 -> new StorageException(
                    "GOOGLE_DRIVE_AUTHENTICATION_FAILED",
                    "Google Drive storage could not authenticate."
            );
            case 403 -> new StorageException(
                    "GOOGLE_DRIVE_PERMISSION_DENIED",
                    "Google Drive storage permission was denied."
            );
            case 404 -> new StorageException(
                    "STORAGE_OBJECT_NOT_FOUND",
                    "Stored object was not found."
            );
            case 408, 504 -> new StorageException(
                    "GOOGLE_DRIVE_TIMEOUT",
                    "Google Drive storage timed out."
            );
            case 429 -> new StorageException(
                    "GOOGLE_DRIVE_RATE_LIMITED",
                    "Google Drive storage is rate limited."
            );
            case 507 -> new StorageException(
                    "GOOGLE_DRIVE_QUOTA_EXCEEDED",
                    "Google Drive storage quota is exhausted."
            );
            default -> new StorageException(
                    fallbackCode,
                    "Google Drive storage request failed."
            );
        };
    }

    private URI filesListUri(String query, String fields, int pageSize) {
        StringBuilder builder = new StringBuilder(properties.effectiveApiBaseUrl())
                .append("/files?q=")
                .append(encode(query))
                .append("&fields=")
                .append(encode(fields))
                .append("&pageSize=")
                .append(pageSize);
        appendDriveFlags(builder);
        return URI.create(builder.toString());
    }

    private URI filesCreateUri(String fields) {
        StringBuilder builder = new StringBuilder(properties.effectiveApiBaseUrl())
                .append("/files?fields=")
                .append(encode(fields));
        appendDriveFlags(builder);
        return URI.create(builder.toString());
    }

    private URI fileUri(String fileId, String query) {
        StringBuilder builder = new StringBuilder(properties.effectiveApiBaseUrl())
                .append("/files/")
                .append(encodePath(fileId));
        if (query != null && !query.isBlank()) {
            builder.append("?").append(query);
            appendDriveFlags(builder);
        } else if (properties.supportsAllDrives()) {
            builder.append("?supportsAllDrives=true");
        }
        return URI.create(builder.toString());
    }

    private URI uploadUri(String fields) {
        StringBuilder builder = new StringBuilder(properties.effectiveUploadBaseUrl())
                .append("/files?uploadType=multipart&fields=")
                .append(encode(fields));
        appendDriveFlags(builder);
        return URI.create(builder.toString());
    }

    private URI downloadUri(String fileId) {
        StringBuilder builder = new StringBuilder(properties.effectiveApiBaseUrl())
                .append("/files/")
                .append(encodePath(fileId))
                .append("?alt=media");
        appendDriveFlags(builder);
        return URI.create(builder.toString());
    }

    private void appendDriveFlags(StringBuilder builder) {
        if (!properties.supportsAllDrives()) {
            return;
        }
        if (builder.indexOf("?") < 0) {
            builder.append('?');
        } else {
            builder.append('&');
        }
        builder.append("supportsAllDrives=true&includeItemsFromAllDrives=true");
    }

    private List<String> parentSegments(String key) {
        String[] parts = key.split("/");
        List<String> segments = new ArrayList<>();
        for (int i = 0; i < parts.length - 1; i++) {
            segments.add(parts[i]);
        }
        return segments;
    }

    private String leafName(String key) {
        int slash = key.lastIndexOf('/');
        return slash < 0 ? key : key.substring(slash + 1);
    }

    private void validateKey(String key) {
        if (key == null || key.isBlank() || key.length() > 1000) {
            throw new StorageException("INVALID_STORAGE_KEY", "Invalid storage key.");
        }
        for (String part : key.replace('\\', '/').split("/")) {
            if (part.isBlank() || ".".equals(part) || "..".equals(part)) {
                throw new StorageException("INVALID_STORAGE_KEY", "Invalid storage key.");
            }
        }
    }

    private byte[] readAndClose(InputStream inputStream) throws Exception {
        Objects.requireNonNull(inputStream, "inputStream");
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        try (DigestInputStream ignored = new DigestInputStream(inputStream, digest)) {
            return ignored.readAllBytes();
        }
    }

    private String sha256(byte[] bytes) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        return HexFormat.of().formatHex(digest.digest(bytes));
    }

    private Map<String, Object> readJsonObject(byte[] bytes) {
        try {
            return objectMapper.readValue(bytes, new TypeReference<>() {
            });
        } catch (IOException exception) {
            throw new StorageException(
                    "GOOGLE_DRIVE_RESPONSE_INVALID",
                    "Google Drive returned an invalid response.",
                    exception
            );
        }
    }

    private Long parseLong(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        if (value instanceof String string && !string.isBlank()) {
            try {
                return Long.parseLong(string);
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }

    private String escapeDriveQuery(String value) {
        return value.replace("\\", "\\\\").replace("'", "\\'");
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private String encodePath(String value) {
        return encode(value).replace("+", "%20");
    }

    private String base64Url(byte[] bytes) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private record TokenResponse(String accessToken, long expiresIn) {
    }

    private record DriveFile(String id, Long size) {
    }
}
