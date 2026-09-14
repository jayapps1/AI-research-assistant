package com.researchassistant.rag.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "rag_query_documents")
@IdClass(RagQueryDocument.IdKey.class)
@Getter
@Setter
@NoArgsConstructor
public class RagQueryDocument {

    @Id
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "query_id", nullable = false)
    private RagQuery query;

    @Id
    @Column(name = "document_id", nullable = false)
    private UUID documentId;

    @Id
    @Column(name = "document_version_id", nullable = false)
    private UUID documentVersionId;

    public static class IdKey implements Serializable {
        private UUID query;
        private UUID documentId;
        private UUID documentVersionId;

        public IdKey() {
        }

        public boolean equals(Object other) {
            if (this == other) {
                return true;
            }
            if (!(other instanceof IdKey idKey)) {
                return false;
            }
            return Objects.equals(query, idKey.query)
                    && Objects.equals(documentId, idKey.documentId)
                    && Objects.equals(documentVersionId, idKey.documentVersionId);
        }

        public int hashCode() {
            return Objects.hash(query, documentId, documentVersionId);
        }
    }
}
