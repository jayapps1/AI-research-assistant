package com.researchassistant.rag.generation;

import java.util.List;

public record GeneratedAnswerDraft(
        String answerText,
        List<GeneratedCitation> citations,
        String provider,
        String model,
        Integer inputTokens,
        Integer outputTokens,
        Long generationDurationMs,
        String finishReason
) {
    public GeneratedAnswerDraft {
        citations = citations == null ? List.of() : List.copyOf(citations);
    }
}
