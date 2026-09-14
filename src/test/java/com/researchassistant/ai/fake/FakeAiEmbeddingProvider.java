package com.researchassistant.ai.fake;

import com.researchassistant.document.embedding.DocumentEmbeddingProvider;
import com.researchassistant.document.embedding.EmbeddingVector;

import java.util.ArrayList;
import java.util.List;

public class FakeAiEmbeddingProvider implements DocumentEmbeddingProvider {

    private boolean available = true;
    private int dimensionCount = 4;

    public void setAvailable(boolean available) {
        this.available = available;
    }

    public void setDimensionCount(int dimensionCount) {
        this.dimensionCount = dimensionCount;
    }

    @Override
    public String provider() {
        return "FAKE_OPENAI";
    }

    @Override
    public String model() {
        return "fake-embedding-model";
    }

    @Override
    public int dimensions() {
        return dimensionCount;
    }

    @Override
    public boolean available() {
        return available;
    }

    @Override
    public List<EmbeddingVector> embed(List<String> texts) {
        if (!available) {
            throw new IllegalStateException("Fake embedding provider unavailable");
        }
        List<EmbeddingVector> result = new ArrayList<>();
        for (String text : texts) {
            double[] values = new double[dimensionCount];
            for (int i = 0; i < dimensionCount; i++) {
                values[i] = 0.1 * (i + 1);
            }
            result.add(new EmbeddingVector(values));
        }
        return result;
    }
}
