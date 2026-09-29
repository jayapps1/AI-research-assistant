package com.researchassistant.conversation.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.researchassistant.conversation.dto.ConversationMessageResponse;
import com.researchassistant.conversation.dto.ConversationRunResponse;
import com.researchassistant.conversation.dto.ConversationSourceResponse;
import com.researchassistant.conversation.dto.ConversationSummaryResponse;
import com.researchassistant.conversation.dto.MessageCitationResponse;
import com.researchassistant.conversation.entity.Conversation;
import com.researchassistant.conversation.entity.ConversationMessage;
import com.researchassistant.conversation.entity.ConversationRun;
import com.researchassistant.conversation.entity.ConversationSource;
import com.researchassistant.conversation.entity.MessageCitation;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
public class ConversationMapper {

    private final ObjectMapper objectMapper;

    public ConversationMapper() {
        this(new ObjectMapper());
    }

    public ConversationMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public ConversationSummaryResponse summary(Conversation conversation) {
        return new ConversationSummaryResponse(
                conversation.getId(),
                conversation.getUser().getId(),
                conversation.getWorkspace() != null ? conversation.getWorkspace().getId() : null,
                conversation.getWorkspace() != null ? conversation.getWorkspace().getName() : null,
                conversation.getProject() != null ? conversation.getProject().getId() : null,
                conversation.getProject() != null ? conversation.getProject().getTitle() : null,
                conversation.getType(),
                conversation.getStatus(),
                conversation.getTitle(),
                conversation.getCreatedAt(),
                conversation.getUpdatedAt(),
                conversation.getLastMessageAt(),
                conversation.getArchivedAt(),
                conversation.getDeletedAt()
        );
    }

    public ConversationMessageResponse message(ConversationMessage message) {
        return message(message, List.of(), List.of());
    }

    public ConversationMessageResponse message(
            ConversationMessage message,
            List<MessageCitation> citations,
            List<ConversationSource> sources
    ) {
        return new ConversationMessageResponse(
                message.getId(),
                message.getRole(),
                message.getContent(),
                message.getStructuredContent(),
                message.getSequenceNumber(),
                message.getCreatedAt(),
                message.getUpdatedAt(),
                citations == null ? List.of() : citations.stream().map(this::citation).toList(),
                sources == null ? List.of() : sources.stream().map(this::source).toList()
        );
    }

    public ConversationSourceResponse source(ConversationSource source) {
        if (source == null) {
            return null;
        }
        return new ConversationSourceResponse(
                source.getId(),
                source.getSourceType(),
                source.getDocument() != null ? source.getDocument().getId() : null,
                source.getDocument() != null ? source.getDocument().getDocumentCode() : null,
                source.getDocumentVersion() != null ? source.getDocumentVersion().getId() : null,
                source.getDocumentVersion() != null ? source.getDocumentVersion().getVersionNumber() : null,
                source.getProjectReference() != null ? source.getProjectReference().getId() : null,
                source.getTitle(),
                source.getUrl(),
                source.getProvider(),
                readAuthors(source.getAuthorsJson()),
                source.getPublishedAt(),
                source.getRetrievedAt(),
                source.getSourceOrdinal(),
                readMetadata(source.getMetadata())
        );
    }

    public MessageCitationResponse citation(MessageCitation citation) {
        ConversationSource source = citation.getSource();
        Map<String, Object> metadata = source == null ? Map.of() : readMetadata(source.getMetadata());
        return new MessageCitationResponse(
                citation.getId(),
                citation.getCitationOrdinal(),
                source != null ? source.getId() : null,
                source != null ? source.getSourceType() : null,
                source != null ? source.getTitle() : null,
                source != null ? source.getUrl() : null,
                source != null ? source.getProvider() : null,
                source != null ? readAuthors(source.getAuthorsJson()) : List.of(),
                source != null ? source.getPublishedAt() : null,
                source != null ? source.getRetrievedAt() : null,
                source != null && source.getDocument() != null ? source.getDocument().getId() : null,
                source != null && source.getDocument() != null ? source.getDocument().getDocumentCode() : null,
                source != null && source.getDocument() != null ? source.getDocument().getTitle() : null,
                source != null && source.getDocumentVersion() != null ? source.getDocumentVersion().getId() : null,
                source != null && source.getDocumentVersion() != null ? source.getDocumentVersion().getVersionNumber() : null,
                source != null && source.getProjectReference() != null ? source.getProjectReference().getId() : null,
                integerMetadata(metadata, "pageNumber"),
                integerMetadata(metadata, "chunkNumber"),
                citation.getSupportingExcerpt(),
                citation.getFormattedCitation(),
                metadata
        );
    }

    public ConversationRunResponse run(ConversationRun run) {
        if (run == null) {
            return null;
        }
        return new ConversationRunResponse(
                run.getId(),
                run.getUserMessage() != null ? run.getUserMessage().getId() : null,
                run.getAssistantMessage() != null ? run.getAssistantMessage().getId() : null,
                run.getAiRequest() != null ? run.getAiRequest().getId() : null,
                run.getRagQuery() != null ? run.getRagQuery().getId() : null,
                run.getOperationType(),
                run.getSearchScope(),
                run.getStatus(),
                run.getProvider(),
                run.getModel(),
                run.getInputTokens(),
                run.getOutputTokens(),
                run.getTotalTokens(),
                run.getCachedInputTokens(),
                run.getProviderCost(),
                run.getPlatformCredits(),
                run.getWebProvider(),
                run.getWebResultCount(),
                run.getWebProviderCost(),
                run.getFailureCode(),
                run.getFailureMessage(),
                run.getStartedAt(),
                run.getCompletedAt()
        );
    }

    private List<String> readAuthors(String authorsJson) {
        if (authorsJson == null || authorsJson.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(authorsJson, new TypeReference<List<String>>() {});
        } catch (Exception ignored) {
            return List.of();
        }
    }

    private Map<String, Object> readMetadata(String metadata) {
        if (metadata == null || metadata.isBlank()) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(metadata, new TypeReference<Map<String, Object>>() {});
        } catch (Exception ignored) {
            return Map.of();
        }
    }

    private Integer integerMetadata(Map<String, Object> metadata, String key) {
        Object value = metadata.get(key);
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value instanceof String text && !text.isBlank()) {
            try {
                return Integer.parseInt(text);
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }
}
