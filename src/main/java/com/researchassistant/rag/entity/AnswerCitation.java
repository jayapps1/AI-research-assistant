package com.researchassistant.rag.entity;

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
        name = "answer_citations",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_answer_citations_ordinal",
                columnNames = {"answer_id", "citation_ordinal"}
        ),
        indexes = {
                @Index(name = "idx_answer_citations_answer_id", columnList = "answer_id"),
                @Index(name = "idx_answer_citations_query_evidence_id", columnList = "query_evidence_id")
        }
)
@Getter
@Setter
@NoArgsConstructor
public class AnswerCitation {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "answer_id", nullable = false)
    private GroundedAnswer answer;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "query_evidence_id", nullable = false)
    private RagQueryEvidence evidence;

    @Column(name = "citation_ordinal", nullable = false)
    private int citationOrdinal;

    @Column(name = "claim_text", columnDefinition = "TEXT")
    private String claimText;

    @Column(name = "supporting_text_snapshot", columnDefinition = "TEXT")
    private String supportingTextSnapshot;

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
    }
}
