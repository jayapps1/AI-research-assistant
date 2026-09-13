package com.researchassistant.document.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.document")
public record DocumentProperties(
        long maxUploadSizeBytes,
        Storage storage
) {

    public record Storage(
            String type,
            String localDirectory
    ) {
    }
}
