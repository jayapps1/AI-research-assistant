package com.researchassistant.conversation.dto;

import java.util.List;

public record ConversationDetailResponse(
        ConversationSummaryResponse conversation,
        List<ConversationMessageResponse> messages,
        ConversationRunResponse latestRun
) {
}
