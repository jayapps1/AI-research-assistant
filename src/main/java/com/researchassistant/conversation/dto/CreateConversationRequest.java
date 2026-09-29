package com.researchassistant.conversation.dto;

import com.researchassistant.conversation.entity.ConversationSearchScope;
import com.researchassistant.rag.scope.RetrievalScopeType;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.Set;
import java.util.UUID;

public record CreateConversationRequest(
        @NotBlank
        @Size(max = 20_000)
        String content,
        ConversationSearchScope searchScope,
        RetrievalScopeType scopeType,
        Set<UUID> documentIds,
        Integer evidenceLimit
) {
}
