package com.researchassistant.document.util;

import java.text.Normalizer;

public final class DocumentTitleNormalizer {

    public static final int MAX_DISPLAY_TITLE_LENGTH = 500;

    private DocumentTitleNormalizer() {
    }

    public static String normalizeDisplayTitle(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.replaceAll("\\s+", " ").trim();
        if (normalized.isEmpty()) {
            return null;
        }
        return truncate(normalized);
    }

    public static String requireDisplayTitle(String value) {
        String normalized = normalizeDisplayTitle(value);
        if (normalized == null) {
            throw new IllegalArgumentException("Document title is required.");
        }
        return normalized;
    }

    public static String displayTitleFromFilename(String originalFilename) {
        String safeName = safeOriginalFilename(originalFilename);
        int dotIndex = safeName.lastIndexOf('.');
        String base = dotIndex > 0 ? safeName.substring(0, dotIndex) : safeName;
        return requireDisplayTitle(base.isBlank() ? "document" : base);
    }

    public static String safeOriginalFilename(String originalFilename) {
        String value = originalFilename == null
                ? "document"
                : originalFilename;
        value = Normalizer.normalize(value, Normalizer.Form.NFKC)
                .replace('\\', '_')
                .replace('/', '_')
                .replaceAll("[\\p{Cntrl}\\r\\n\"]", "_")
                .trim();
        return value.isEmpty() ? "document" : truncate(value);
    }

    public static String firstNonBlankDisplayTitle(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            String normalized = normalizeDisplayTitle(value);
            if (normalized != null) {
                return normalized;
            }
        }
        return null;
    }

    private static String truncate(String value) {
        return value.length() <= MAX_DISPLAY_TITLE_LENGTH
                ? value
                : value.substring(0, MAX_DISPLAY_TITLE_LENGTH).trim();
    }
}
