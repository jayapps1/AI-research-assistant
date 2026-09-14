package com.researchassistant.document.chunk;

public record ChunkSlice(
        String text,
        int startInclusive,
        int endExclusive
) {
}
