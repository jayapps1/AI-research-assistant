package com.researchassistant.document.embedding;

import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class DisabledDocumentEmbeddingProvider
        implements DocumentEmbeddingProvider {

    private final EmbeddingProperties properties;

    public DisabledDocumentEmbeddingProvider(EmbeddingProperties properties) {
        this.properties = properties;
    }

    @Override
    public String provider() {
        return normalize(properties.provider(), "none");
    }

    @Override
    public String model() {
        return normalize(properties.model(), "none");
    }

    @Override
    public int dimensions() {
        Integer configured = properties.parsedDimensions();
        return configured == null ? 1 : configured;
    }

    @Override
    public boolean available() {
        return false;
    }

    @Override
    public List<EmbeddingVector> embed(List<String> texts) {
        throw new IllegalStateException("Document embeddings are disabled.");
    }

    private String normalize(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }
}
