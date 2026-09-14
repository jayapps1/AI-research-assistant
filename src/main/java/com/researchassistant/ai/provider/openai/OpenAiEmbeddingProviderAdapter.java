package com.researchassistant.ai.provider.openai;

import com.researchassistant.ai.config.AiProperties;
import com.researchassistant.ai.provider.AiProviderType;
import com.researchassistant.document.embedding.DocumentEmbeddingProvider;
import com.researchassistant.document.embedding.EmbeddingVector;

import org.springframework.ai.embedding.EmbeddingModel;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@ConditionalOnProperty(name = "app.ai.embedding.provider", havingValue = "openai")
public class OpenAiEmbeddingProviderAdapter implements DocumentEmbeddingProvider {

    private final AiProperties properties;
    private final EmbeddingModel embeddingModel;

    public OpenAiEmbeddingProviderAdapter(AiProperties properties, ObjectProvider<EmbeddingModel> embeddingModel) {
        this.properties = properties;
        this.embeddingModel = embeddingModel.getIfAvailable();
    }

    @Override
    public String provider() {
        return AiProviderType.OPENAI.name();
    }

    @Override
    public String model() {
        return properties.embedding().model() == null || properties.embedding().model().isBlank()
                ? "text-embedding-3-small"
                : properties.embedding().model();
    }

    @Override
    public int dimensions() {
        if (properties.embedding().dimensions() != null && properties.embedding().dimensions() > 0) {
            return properties.embedding().dimensions();
        }
        return 1536;
    }

    @Override
    public boolean available() {
        return properties.embedding().enabled() && embeddingModel != null;
    }

    @Override
    public List<EmbeddingVector> embed(List<String> texts) {
        if (!available()) {
            throw new IllegalStateException("OpenAI embedding provider is disabled or unavailable.");
        }
        if (texts == null || texts.isEmpty()) {
            return List.of();
        }
        List<float[]> responseVectors = embeddingModel.embed(texts);
        List<EmbeddingVector> result = new ArrayList<>(responseVectors.size());
        for (float[] vector : responseVectors) {
            double[] doubles = new double[vector.length];
            for (int i = 0; i < vector.length; i++) {
                doubles[i] = vector[i];
            }
            result.add(new EmbeddingVector(doubles));
        }
        return result;
    }
}
