package com.researchassistant.document.embedding;

import java.util.List;

public interface DocumentEmbeddingProvider {

    String provider();

    String model();

    int dimensions();

    boolean available();

    List<EmbeddingVector> embed(List<String> texts);
}
