package com.researchassistant.identity.service;

import com.researchassistant.document.security.FileScanStatus;
import com.researchassistant.document.security.FileSecurityScanner;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Set;

@Component
public class ProfileImageSecurityValidator {

    public static final long DEFAULT_MAX_SIZE_BYTES = 5 * 1024 * 1024; // 5 MB
    public static final int MAX_DIMENSION = 4096;

    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "image/jpeg",
            "image/jpg",
            "image/png",
            "image/webp"
    );

    private final long maxSizeBytes;
    private final FileSecurityScanner fileSecurityScanner;

    public ProfileImageSecurityValidator(
            @Value("${app.profile-image.max-size-bytes:5242880}") long maxSizeBytes,
            FileSecurityScanner fileSecurityScanner
    ) {
        this.maxSizeBytes = maxSizeBytes > 0 ? maxSizeBytes : DEFAULT_MAX_SIZE_BYTES;
        this.fileSecurityScanner = fileSecurityScanner;
    }

    public record ValidatedImageMetadata(
            String normalizedContentType,
            int width,
            int height,
            byte[] bytes,
            String safeOriginalFilename
    ) {
    }

    public ValidatedImageMetadata validate(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Profile image file is required.");
        }

        if (file.getSize() > maxSizeBytes) {
            throw new IllegalArgumentException(
                    "Profile image exceeds maximum allowed size of " + (maxSizeBytes / (1024 * 1024)) + " MB."
            );
        }

        String rawContentType = file.getContentType();
        if (rawContentType == null || rawContentType.isBlank()) {
            throw new IllegalArgumentException("Content-Type header is required for profile image.");
        }

        String normalizedContentType = rawContentType.toLowerCase().split(";")[0].trim();
        if (!ALLOWED_CONTENT_TYPES.contains(normalizedContentType)) {
            throw new IllegalArgumentException(
                    "Unsupported image format: " + normalizedContentType + ". Allowed formats: JPG, PNG, WebP."
            );
        }
        if ("image/jpg".equals(normalizedContentType)) {
            normalizedContentType = "image/jpeg";
        }

        byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (IOException e) {
            throw new IllegalArgumentException("Failed to read profile image data.", e);
        }

        if (bytes.length == 0) {
            throw new IllegalArgumentException("Profile image file is empty.");
        }

        // Validate magic byte signatures
        String detectedFormat = detectFormatFromMagicBytes(bytes);
        if (detectedFormat == null) {
            throw new IllegalArgumentException("File content does not match a valid image signature.");
        }

        // Validate content type matches detected magic bytes
        if (!isContentTypeMatchingFormat(normalizedContentType, detectedFormat)) {
            throw new IllegalArgumentException("Declared content type does not match image signature.");
        }

        // Decode and validate dimensions
        int[] dimensions = extractDimensions(bytes, detectedFormat);
        int width = dimensions[0];
        int height = dimensions[1];

        if (width <= 0 || height <= 0) {
            throw new IllegalArgumentException("Invalid image dimensions.");
        }

        if (width > MAX_DIMENSION || height > MAX_DIMENSION) {
            throw new IllegalArgumentException(
                    "Image dimensions exceed maximum allowed size (" + MAX_DIMENSION + "x" + MAX_DIMENSION + ")."
            );
        }

        // Perform security scan
        String filename = sanitizeFilename(file.getOriginalFilename());
        FileScanStatus scanStatus = fileSecurityScanner.scan(bytes, normalizedContentType, filename);
        if (scanStatus == FileScanStatus.INFECTED) {
            throw new IllegalArgumentException("Security scan detected harmful content in profile image.");
        }

        return new ValidatedImageMetadata(
                normalizedContentType,
                width,
                height,
                bytes,
                filename
        );
    }

    private String detectFormatFromMagicBytes(byte[] b) {
        if (b.length >= 3 && (b[0] & 0xFF) == 0xFF && (b[1] & 0xFF) == 0xD8 && (b[2] & 0xFF) == 0xFF) {
            return "JPEG";
        }

        if (b.length >= 8 &&
                (b[0] & 0xFF) == 0x89 && b[1] == 'P' && b[2] == 'N' && b[3] == 'G' &&
                b[4] == 0x0D && b[5] == 0x0A && b[6] == 0x1A && b[7] == 0x0A) {
            return "PNG";
        }

        if (b.length >= 12 &&
                b[0] == 'R' && b[1] == 'I' && b[2] == 'F' && b[3] == 'F' &&
                b[8] == 'W' && b[9] == 'E' && b[10] == 'B' && b[11] == 'P') {
            return "WEBP";
        }

        return null;
    }

    private boolean isContentTypeMatchingFormat(String contentType, String format) {
        return switch (format) {
            case "JPEG" -> "image/jpeg".equals(contentType);
            case "PNG" -> "image/png".equals(contentType);
            case "WEBP" -> "image/webp".equals(contentType);
            default -> false;
        };
    }

    private int[] extractDimensions(byte[] bytes, String format) {
        try {
            BufferedImage bufferedImage = ImageIO.read(new ByteArrayInputStream(bytes));
            if (bufferedImage != null) {
                return new int[]{bufferedImage.getWidth(), bufferedImage.getHeight()};
            }
        } catch (Exception ignored) {
        }

        // For WebP if ImageIO doesn't have a WebP reader installed
        if ("WEBP".equals(format)) {
            int[] webpDim = parseWebpDimensions(bytes);
            if (webpDim != null) {
                return webpDim;
            }
        }

        throw new IllegalArgumentException("Unable to decode image data or extract dimensions.");
    }

    private int[] parseWebpDimensions(byte[] bytes) {
        if (bytes.length < 30) return null;
        try {
            String chunkType = new String(bytes, 12, 4);
            if ("VP8 ".equals(chunkType) && bytes.length >= 30) {
                // Lossy WebP: bytes 26-27 width, 28-29 height
                int width = (bytes[26] & 0xFF | (bytes[27] & 0xFF) << 8) & 0x3FFF;
                int height = (bytes[28] & 0xFF | (bytes[29] & 0xFF) << 8) & 0x3FFF;
                return new int[]{width, height};
            } else if ("VP8L".equals(chunkType) && bytes.length >= 25) {
                // Lossless WebP: byte 21 signature 0x2F, then 28 bits: 14 bits width-1, 14 bits height-1
                if ((bytes[20] & 0xFF) == 0x2F) {
                    ByteBuffer buffer = ByteBuffer.wrap(bytes, 21, 4).order(ByteOrder.LITTLE_ENDIAN);
                    int bits = buffer.getInt();
                    int width = (bits & 0x3FFF) + 1;
                    int height = ((bits >> 14) & 0x3FFF) + 1;
                    return new int[]{width, height};
                }
            } else if ("VP8X".equals(chunkType) && bytes.length >= 30) {
                // Extended WebP: 24-bit width at 24, 24-bit height at 27
                int width = 1 + (bytes[24] & 0xFF | (bytes[25] & 0xFF) << 8 | (bytes[26] & 0xFF) << 16);
                int height = 1 + (bytes[27] & 0xFF | (bytes[28] & 0xFF) << 8 | (bytes[29] & 0xFF) << 16);
                return new int[]{width, height};
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    private String sanitizeFilename(String original) {
        if (original == null || original.isBlank()) {
            return "profile-image";
        }
        String clean = original.replaceAll("[^a-zA-Z0-9._-]", "_");
        if (clean.length() > 200) {
            clean = clean.substring(clean.length() - 200);
        }
        return clean;
    }
}
