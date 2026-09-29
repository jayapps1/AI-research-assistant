package com.researchassistant.conversation.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(
        name = "message_citations",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_message_citations_ordinal",
                columnNames = {"message_id", "citation_ordinal"}
        ),
        indexes = {
                @Index(name = "idx_message_citations_message", columnList = "message_id"),
                @Index(name = "idx_message_citations_source", columnList = "conversation_source_id")
        }
)
@Getter
@Setter
@NoArgsConstructor
public class MessageCitation {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "message_id", nullable = false)
    private ConversationMessage message;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "conversation_source_id", nullable = false)
    private ConversationSource source;

    @Column(name = "citation_ordinal", nullable = false)
    private int citationOrdinal;

    @Column(name = "marker", length = 40)
    private String marker;

    @Column(name = "claim_text", columnDefinition = "TEXT")
    private String claimText;

    @Column(name = "supporting_excerpt", columnDefinition = "TEXT")
    private String supportingExcerpt;

    @Column(name = "formatted_citation", columnDefinition = "TEXT")
    private String formattedCitation;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        if (createdAt == null) {
            createdAt = OffsetDateTime.now();
        }
        if (citationOrdinal < 1) {
            citationOrdinal = 1;
        }
    }
}
