package com.researchassistant.document.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.document")
public record DocumentProperties(
        long maxUploadSizeBytes,
        Storage storage,
        Chunking chunking,
        Processing processing
) {

    public record Storage(
            String type,
            String localDirectory
    ) {
    }

    public record Chunking(
            int targetCharacters,
            int overlapCharacters,
            int minimumCharacters
    ) {
    }

    public record Processing(
            boolean autoProcessAfterUpload
    ) {
    }
}
