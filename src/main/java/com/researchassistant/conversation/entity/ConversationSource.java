package com.researchassistant.conversation.entity;

import com.researchassistant.document.entity.Document;
import com.researchassistant.document.entity.DocumentVersion;
import com.researchassistant.reference.entity.ProjectReference;

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
        name = "conversation_sources",
        indexes = {
                @Index(name = "idx_conversation_sources_conversation", columnList = "conversation_id"),
                @Index(name = "idx_conversation_sources_message", columnList = "message_id"),
                @Index(name = "idx_conversation_sources_url", columnList = "url")
        }
)
@Getter
@Setter
@NoArgsConstructor
public class ConversationSource {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "conversation_id", nullable = false)
    private Conversation conversation;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "message_id", nullable = false)
    private ConversationMessage message;

    @Enumerated(EnumType.STRING)
    @Column(name = "source_type", nullable = false, length = 60)
    private ConversationSourceType sourceType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "document_id")
    private Document document;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "document_version_id")
    private DocumentVersion documentVersion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_reference_id")
    private ProjectReference projectReference;

    @Column(name = "title", nullable = false, columnDefinition = "TEXT")
    private String title;

    @Column(name = "url", columnDefinition = "TEXT")
    private String url;

    @Column(name = "provider", length = 80)
    private String provider;

    @Column(name = "authors_json", columnDefinition = "TEXT")
    private String authorsJson;

    @Column(name = "published_at")
    private OffsetDateTime publishedAt;

    @Column(name = "retrieved_at", nullable = false)
    private OffsetDateTime retrievedAt;

    @Column(name = "source_ordinal")
    private Integer sourceOrdinal;

    @Column(name = "metadata", columnDefinition = "TEXT")
    private String metadata;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        OffsetDateTime now = OffsetDateTime.now();
        if (createdAt == null) {
            createdAt = now;
        }
        if (retrievedAt == null) {
            retrievedAt = now;
        }
    }
}
