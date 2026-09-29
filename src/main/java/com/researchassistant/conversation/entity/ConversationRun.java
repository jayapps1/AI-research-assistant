package com.researchassistant.conversation.entity;

import com.researchassistant.ai.usage.AiRequest;
import com.researchassistant.rag.entity.RagQuery;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(
        name = "conversation_runs",
        indexes = {
                @Index(name = "idx_conversation_runs_conversation_created", columnList = "conversation_id,created_at"),
                @Index(name = "idx_conversation_runs_user_message", columnList = "user_message_id"),
                @Index(name = "idx_conversation_runs_ai_request", columnList = "ai_request_id")
        }
)
@Getter
@Setter
@NoArgsConstructor
public class ConversationRun {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "conversation_id", nullable = false)
    private Conversation conversation;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_message_id", nullable = false)
    private ConversationMessage userMessage;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assistant_message_id")
    private ConversationMessage assistantMessage;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ai_request_id")
    private AiRequest aiRequest;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rag_query_id")
    private RagQuery ragQuery;

    @Column(name = "operation_type", nullable = false, length = 60)
    private String operationType = "GENERAL_CHAT";

    @Column(name = "search_scope", nullable = false, length = 40)
    private String searchScope = "GENERAL";

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 40)
    private ConversationRunStatus status = ConversationRunStatus.PENDING;

    @Column(name = "provider", length = 40)
    private String provider;

    @Column(name = "model", length = 200)
    private String model;

    @Column(name = "input_tokens")
    private Integer inputTokens;

    @Column(name = "output_tokens")
    private Integer outputTokens;

    @Column(name = "total_tokens")
    private Integer totalTokens;

    @Column(name = "cached_input_tokens")
    private Integer cachedInputTokens;

    @Column(name = "provider_cost", precision = 12, scale = 6)
    private BigDecimal providerCost;

    @Column(name = "platform_credits", precision = 14, scale = 4)
    private BigDecimal platformCredits;

    @Column(name = "web_provider", length = 80)
    private String webProvider;

    @Column(name = "web_result_count")
    private Integer webResultCount;

    @Column(name = "web_provider_cost", precision = 12, scale = 6)
    private BigDecimal webProviderCost;

    @Column(name = "failure_code", length = 100)
    private String failureCode;

    @Column(name = "failure_message", length = 1000)
    private String failureMessage;

    @Column(name = "started_at", nullable = false)
    private OffsetDateTime startedAt;

    @Column(name = "completed_at")
    private OffsetDateTime completedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        OffsetDateTime now = OffsetDateTime.now();
        if (startedAt == null) {
            startedAt = now;
        }
        if (createdAt == null) {
            createdAt = now;
        }
        if (updatedAt == null) {
            updatedAt = now;
        }
        if (status == null) {
            status = ConversationRunStatus.PENDING;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = OffsetDateTime.now();
    }
}
