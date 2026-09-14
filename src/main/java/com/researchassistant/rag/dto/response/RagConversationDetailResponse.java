package com.researchassistant.rag.dto.response;

import java.util.List;

public record RagConversationDetailResponse(
        RagConversationResponse conversation,
        List<RagQuerySummaryResponse> queries
) {
}
