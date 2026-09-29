package com.researchassistant.common.storage;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.security.KeyPairGenerator;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

class GoogleDriveObjectStorageServiceTest {

    private HttpServer server;

    @AfterEach
    void stopServer() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void storesDownloadsAndDeletesThroughGoogleDriveApiWithoutRealDriveAccount() throws Exception {
        FakeDriveApi fakeDrive = startFakeDriveApi();
        String baseUrl = "http://localhost:" + server.getAddress().getPort();
        ObjectStorageProperties properties = new ObjectStorageProperties(
                StorageProvider.GOOGLE_DRIVE,
                "./target/object-storage",
                "./target/profile-storage",
                new ObjectStorageProperties.GoogleDrive(
                        "root-folder",
                        "storage-test@example.iam.gserviceaccount.com",
                        privateKeyPem(),
                        "",
                        "test-key",
                        baseUrl + "/oauth2/token",
                        baseUrl + "/drive/v3",
                        baseUrl + "/upload/drive/v3",
                        false,
                        Duration.ofSeconds(1),
                        Duration.ofSeconds(5),
                        1
                )
        );
        GoogleDriveObjectStorageService service = new GoogleDriveObjectStorageService(
                properties,
                new ObjectMapper(),
                HttpClient.newHttpClient()
        );

        String key = "projects/project-1/documents/document-1/example.txt";
        StoredObject stored = service.store(
                key,
                new ByteArrayInputStream("hello-drive".getBytes(StandardCharsets.UTF_8)),
                "text/plain"
        );

        assertThat(stored.key()).isEqualTo(key);
        assertThat(stored.providerFileId()).isEqualTo("file-1");
        assertThat(fakeDrive.createdFolders).containsExactly("projects", "project-1", "documents", "document-1");
        assertThat(service.exists(key)).isTrue();
        try (StorageObject object = service.open(key)) {
            assertThat(object.inputStream().readAllBytes()).isEqualTo("hello-drive".getBytes(StandardCharsets.UTF_8));
            assertThat(object.sizeBytes()).isEqualTo("hello-drive".length());
        }

        service.delete(key);

        assertThat(service.exists(key)).isFalse();
    }

    private FakeDriveApi startFakeDriveApi() throws IOException {
        FakeDriveApi fakeDrive = new FakeDriveApi();
        server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        server.createContext("/", fakeDrive::handle);
        server.start();
        return fakeDrive;
    }

    private String privateKeyPem() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        byte[] encoded = generator.generateKeyPair().getPrivate().getEncoded();
        return "-----BEGIN PRIVATE KEY-----\n"
                + Base64.getMimeEncoder(64, "\n".getBytes(StandardCharsets.UTF_8)).encodeToString(encoded)
                + "\n-----END PRIVATE KEY-----\n";
    }

    private static final class FakeDriveApi {
        private final ObjectMapper objectMapper = new ObjectMapper();
        private final AtomicInteger folderCounter = new AtomicInteger();
        private final List<String> createdFolders = new ArrayList<>();
        private byte[] uploadedBytes;
        private boolean deleted;

        private void handle(HttpExchange exchange) throws IOException {
            URI uri = exchange.getRequestURI();
            String path = uri.getPath();
            String query = uri.getRawQuery() == null ? "" : java.net.URLDecoder.decode(uri.getRawQuery(), StandardCharsets.UTF_8);
            if ("/oauth2/token".equals(path)) {
                json(exchange, 200, "{\"access_token\":\"fake-token\",\"expires_in\":3600}");
                return;
            }
            if (path.startsWith("/upload/drive/v3/files")) {
                byte[] body = exchange.getRequestBody().readAllBytes();
                uploadedBytes = extractUploadBytes(body);
                deleted = false;
                json(exchange, 200, "{\"id\":\"file-1\"}");
                return;
            }
            if (path.equals("/drive/v3/files") && "POST".equals(exchange.getRequestMethod())) {
                var body = objectMapper.readTree(exchange.getRequestBody());
                String name = body.get("name").asText();
                createdFolders.add(name);
                json(exchange, 200, "{\"id\":\"folder-" + folderCounter.incrementAndGet() + "\"}");
                return;
            }
            if (path.equals("/drive/v3/files") && query.contains("mimeType")) {
                json(exchange, 200, "{\"files\":[]}");
                return;
            }
            if (path.equals("/drive/v3/files") && query.contains("appProperties")) {
                json(exchange, 200, deleted || uploadedBytes == null ? "{\"files\":[]}" : "{\"files\":[{\"id\":\"file-1\",\"name\":\"example.txt\",\"size\":\"" + uploadedBytes.length + "\"}]}");
                return;
            }
            if (path.equals("/drive/v3/files/root-folder")) {
                json(exchange, 200, "{\"id\":\"root-folder\",\"name\":\"root\",\"mimeType\":\"application/vnd.google-apps.folder\",\"trashed\":false}");
                return;
            }
            if (path.equals("/drive/v3/files/file-1") && "GET".equals(exchange.getRequestMethod()) && query.contains("alt=media")) {
                bytes(exchange, 200, uploadedBytes == null ? new byte[0] : uploadedBytes);
                return;
            }
            if (path.equals("/drive/v3/files/file-1") && "GET".equals(exchange.getRequestMethod())) {
                json(exchange, 200, deleted || uploadedBytes == null ? "{\"id\":\"file-1\",\"trashed\":true}" : "{\"id\":\"file-1\",\"name\":\"example.txt\",\"size\":\"" + uploadedBytes.length + "\",\"trashed\":false}");
                return;
            }
            if (path.equals("/drive/v3/files/file-1") && "DELETE".equals(exchange.getRequestMethod())) {
                deleted = true;
                bytes(exchange, 204, new byte[0]);
                return;
            }
            json(exchange, 404, "{}");
        }

        private byte[] extractUploadBytes(byte[] multipartBody) {
            byte[] separator = "\r\n\r\n".getBytes(StandardCharsets.UTF_8);
            int first = indexOf(multipartBody, separator, 0);
            int second = indexOf(multipartBody, separator, first + separator.length);
            int end = indexOf(multipartBody, "\r\n--".getBytes(StandardCharsets.UTF_8), second + separator.length);
            return java.util.Arrays.copyOfRange(multipartBody, second + separator.length, end);
        }

        private int indexOf(byte[] bytes, byte[] pattern, int start) {
            for (int i = Math.max(0, start); i <= bytes.length - pattern.length; i++) {
                boolean match = true;
                for (int j = 0; j < pattern.length; j++) {
                    if (bytes[i + j] != pattern[j]) {
                        match = false;
                        break;
                    }
                }
                if (match) {
                    return i;
                }
            }
            return -1;
        }

        private void json(HttpExchange exchange, int status, String body) throws IOException {
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            bytes(exchange, status, body.getBytes(StandardCharsets.UTF_8));
        }

        private void bytes(HttpExchange exchange, int status, byte[] body) throws IOException {
            exchange.sendResponseHeaders(status, status == 204 ? -1 : body.length);
            if (status != 204) {
                exchange.getResponseBody().write(body);
            }
            exchange.close();
        }
    }
}
