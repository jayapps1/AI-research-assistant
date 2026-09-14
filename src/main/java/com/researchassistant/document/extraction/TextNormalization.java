package com.researchassistant.document.extraction;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

public final class TextNormalization {

    private TextNormalization() {
    }

    public static String normalize(String value) {
        if (value == null) {
            return "";
        }
        String normalized = value
                .replace("\r\n", "\n")
                .replace('\r', '\n')
                .replace('\u0000', ' ')
                .replaceAll("[\\p{Cntrl}&&[^\n\t]]", " ")
                .replaceAll("[ \\t\\x0B\\f]+", " ")
                .replaceAll(" *\n *", "\n")
                .replaceAll("\n{4,}", "\n\n\n")
                .trim();
        return normalized;
    }

    public static String sha256(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is not available.", exception);
        }
    }
}
