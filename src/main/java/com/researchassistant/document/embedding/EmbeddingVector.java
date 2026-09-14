package com.researchassistant.document.embedding;

public record EmbeddingVector(double[] values) {

    public int dimensions() {
        return values.length;
    }
}
