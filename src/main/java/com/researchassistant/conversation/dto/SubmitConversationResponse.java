package com.researchassistant.conversation.dto;

public record SubmitConversationResponse(
        ConversationSummaryResponse conversation,
        ConversationMessageResponse userMessage,
        ConversationMessageResponse assistantMessage,
        ConversationRunResponse run
) {
}
