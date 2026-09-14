package com.researchassistant.rag.dto.response;

import com.researchassistant.rag.entity.RagQueryStatus;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record GroundedAnswerResponse(
        UUID queryId,
        UUID conversationId,
        String answer,
        RagQueryStatus status,
        List<CitationResponse> citations,
        RetrievalSummaryResponse retrievalSummary,
        OffsetDateTime createdAt
) {
    public GroundedAnswerResponse {
        citations = citations == null ? List.of() : List.copyOf(citations);
    }
}
