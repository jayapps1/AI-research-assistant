package com.researchassistant.reference.service;

import org.springframework.stereotype.Service;
import java.text.Normalizer;
import java.util.Locale;

@Service
public class ReferenceNormalizationService {
    public String normalizeDoi(String value) {
        if (value == null) return null;
        String doi = value.trim();
        doi = doi.replaceFirst("(?i)^https?://(dx\\.)?doi\\.org/", "");
        doi = doi.replaceFirst("(?i)^doi:\\s*", "");
        doi = doi.trim();
        return doi.isBlank() ? null : doi.toLowerCase(Locale.ROOT);
    }

    public String normalizeTitle(String value) {
        if (value == null) return null;
        String normalized = Normalizer.normalize(value, Normalizer.Form.NFKD)
                .replaceAll("\\p{M}", "")
                .replaceAll("[^\\p{IsAlphabetic}\\p{IsDigit}]+", " ")
                .trim()
                .toLowerCase(Locale.ROOT)
                .replaceAll("\\s+", " ");
        return normalized.isBlank() ? null : normalized;
    }

    public String normalizeAuthorName(String value) {
        return normalizeTitle(value);
    }
}
