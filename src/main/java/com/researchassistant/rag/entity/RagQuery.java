package com.researchassistant.rag.entity;

import com.researchassistant.identity.entity.User;
import com.researchassistant.rag.scope.RetrievalScopeType;

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
import jakarta.persistence.Table;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(
        name = "rag_queries",
        indexes = {
                @Index(name = "idx_rag_queries_conversation_id", columnList = "conversation_id"),
                @Index(name = "idx_rag_queries_created_by", columnList = "created_by"),
                @Index(name = "idx_rag_queries_status", columnList = "status"),
                @Index(name = "idx_rag_queries_created_at", columnList = "created_at")
        }
)
@Getter
@Setter
@NoArgsConstructor
public class RagQuery {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "conversation_id", nullable = false)
    private RagConversation conversation;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "created_by", nullable = false)
    private User createdBy;

    @Column(name = "question", nullable = false, columnDefinition = "TEXT")
    private String question;

    @Enumerated(EnumType.STRING)
    @Column(name = "scope_type", nullable = false, length = 40)
    private RetrievalScopeType scopeType;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 40)
    private RagQueryStatus status = RagQueryStatus.RECEIVED;

    @Column(name = "requested_evidence_limit")
    private Integer requestedEvidenceLimit;

    @Column(name = "retrieval_mode", length = 40)
    private String retrievalMode;

    @Column(name = "retrieval_duration_ms")
    private Long retrievalDurationMs;

    @Column(name = "selected_document_count")
    private Integer selectedDocumentCount;

    @Column(name = "analyzed_source_count")
    private Integer analyzedSourceCount;

    @Column(name = "sources_with_relevant_evidence_count")
    private Integer sourcesWithRelevantEvidenceCount;

    @Column(name = "candidate_chunk_count")
    private Integer candidateChunkCount;

    @Column(name = "selected_evidence_count")
    private Integer selectedEvidenceCount;

    @Column(name = "estimated_input_tokens")
    private Integer estimatedInputTokens;

    @Column(name = "actual_input_tokens")
    private Integer actualInputTokens;

    @Column(name = "actual_output_tokens")
    private Integer actualOutputTokens;

    @Column(name = "context_budget_tokens")
    private Integer contextBudgetTokens;

    @Column(name = "truncated_evidence_count")
    private Integer truncatedEvidenceCount;

    @Column(name = "generation_strategy", length = 60)
    private String generationStrategy;

    @Column(name = "configured_model", length = 120)
    private String configuredModel;

    @Column(name = "failure_code", length = 100)
    private String failureCode;

    @Column(name = "failure_message", length = 1000)
    private String failureMessage;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "completed_at")
    private OffsetDateTime completedAt;

    @PrePersist
    protected void onCreate() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        if (createdAt == null) {
            createdAt = OffsetDateTime.now();
        }
        if (status == null) {
            status = RagQueryStatus.RECEIVED;
        }
    }
}
