package com.researchassistant.document.entity;

import com.researchassistant.document.chunk.ChunkHygieneService;
import com.researchassistant.document.repository.DocumentChunkRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(properties = {
        "app.security.jwt.secret=MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=",
        "app.security.credentials.encryption-key=MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY="
})
class DocumentChunkSemanticTypeIntegrationTests {

    @Autowired
    private DocumentChunkRepository documentChunkRepository;

    @Autowired
    private EntityManager entityManager;

    private final ChunkHygieneService hygieneService = new ChunkHygieneService();

    @Test
    @DisplayName("1. Enum values match expected RAG domain model")
    void testEnumValues() {
        assertThat(ChunkSemanticType.values()).containsExactlyInAnyOrder(
                ChunkSemanticType.FRONT_MATTER,
                ChunkSemanticType.BODY,
                ChunkSemanticType.TABLE,
                ChunkSemanticType.FIGURE_CAPTION,
                ChunkSemanticType.REFERENCES,
                ChunkSemanticType.FOOTER_HEADER,
                ChunkSemanticType.METADATA
        );
    }

    @Test
    @DisplayName("2. PrePersist onCreate() ensures default BODY when semanticType is not provided")
    void testOnCreateDefaultSemanticType() {
        DocumentChunk chunk = new DocumentChunk();
        assertThat(chunk.getSemanticType()).isNull();
        chunk.onCreate();
        assertThat(chunk.getSemanticType()).isEqualTo(ChunkSemanticType.BODY);

        // If explicitly set, onCreate does not overwrite it
        DocumentChunk refChunk = new DocumentChunk();
        refChunk.setSemanticType(ChunkSemanticType.REFERENCES);
        refChunk.onCreate();
        assertThat(refChunk.getSemanticType()).isEqualTo(ChunkSemanticType.REFERENCES);
    }

    @Test
    @DisplayName("3. Existing database records have valid semantic types and none are NULL")
    void testLegacyBackfillIntegrity() {
        long totalChunks = documentChunkRepository.count();
        assertThat(totalChunks).isGreaterThanOrEqualTo(0);

        Number nullCount = (Number) entityManager.createNativeQuery(
                "SELECT COUNT(*) FROM document_chunks WHERE semantic_type IS NULL"
        ).getSingleResult();
        assertThat(nullCount.longValue()).isEqualTo(0L);

        @SuppressWarnings("unchecked")
        List<Object[]> distinctTypes = entityManager.createNativeQuery(
                "SELECT semantic_type, COUNT(*) FROM document_chunks GROUP BY semantic_type"
        ).getResultList();

        for (Object[] row : distinctTypes) {
            String typeStr = (String) row[0];
            assertThat(typeStr).isNotNull();
            assertThat(ChunkSemanticType.valueOf(typeStr)).isNotNull();
        }
    }

    @Test
    @DisplayName("4. ChunkHygieneService classifies new chunks accurately and filters non-substantive types")
    void testHygieneClassificationAndFiltering() {
        // References classification and filtering
        String refText = "References\n[1] Smith, J. (2023). Deep learning applications. AI Journal, 12(3), 45-67.";
        ChunkSemanticType refType = hygieneService.classify(refText, 15, 1);
        assertThat(refType).isEqualTo(ChunkSemanticType.REFERENCES);
        assertThat(hygieneService.isSubstantiveEvidence(refType)).isFalse();

        // Footer/Header classification and filtering
        String footerText = "Page 10 of 42\nDownloaded from https://example.com";
        ChunkSemanticType footerType = hygieneService.classify(footerText, 10, 1);
        assertThat(footerType).isEqualTo(ChunkSemanticType.FOOTER_HEADER);
        assertThat(hygieneService.isSubstantiveEvidence(footerType)).isFalse();

        // Metadata classification and filtering
        String metaText = "Keywords: deep learning, neural networks, NLP\nReceived: 10 Jan 2024";
        ChunkSemanticType metaType = hygieneService.classify(metaText, 1, 1);
        assertThat(metaType).isEqualTo(ChunkSemanticType.METADATA);
        assertThat(hygieneService.isSubstantiveEvidence(metaType)).isFalse();

        // Body classification and substantive retention
        String bodyText = "The empirical findings demonstrate that fine-tuning transformer models on domain-specific corpora yields a statistically significant 18% improvement in F1-score across all evaluation benchmarks.";
        ChunkSemanticType bodyType = hygieneService.classify(bodyText, 5, 2);
        assertThat(bodyType).isEqualTo(ChunkSemanticType.BODY);
        assertThat(hygieneService.isSubstantiveEvidence(bodyType)).isTrue();

        // Table and Figure Caption substantive retention
        assertThat(hygieneService.isSubstantiveEvidence(ChunkSemanticType.TABLE)).isTrue();
        assertThat(hygieneService.isSubstantiveEvidence(ChunkSemanticType.FIGURE_CAPTION)).isTrue();
    }

    @Test
    @Transactional
    @DisplayName("5. Database check constraint enforces valid semantic_type enum values")
    void testCheckConstraintRejectsInvalidEnum() {
        @SuppressWarnings("unchecked")
        List<Object[]> rows = entityManager.createNativeQuery(
                "SELECT id, document_version_id, page_id FROM document_chunks LIMIT 1"
        ).getResultList();

        if (!rows.isEmpty()) {
            Object[] first = rows.get(0);
            UUID versionId = (UUID) first[1];
            UUID pageId = (UUID) first[2];
            UUID testId = UUID.randomUUID();
            String sha64 = "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef";

            assertThatThrownBy(() -> {
                entityManager.createNativeQuery(
                        "INSERT INTO document_chunks (id, document_version_id, page_id, chunk_number, text_content, character_start, character_end, character_count, content_checksum_sha256, semantic_type, created_at) " +
                        "VALUES (:id, :versionId, :pageId, 999999, 'Test invalid semantic type', 0, 25, 25, :sha64, 'INVALID_TYPE', NOW())"
                )
                .setParameter("id", testId)
                .setParameter("versionId", versionId)
                .setParameter("pageId", pageId)
                .setParameter("sha64", sha64)
                .executeUpdate();
                entityManager.flush();
            }).hasMessageFindingMatch("chk_document_chunks_semantic_type|check constraint|CHECK constraint");
        }
    }

    @Test
    @Transactional
    @DisplayName("6. Database NOT NULL constraint rejects null semantic_type")
    void testNotNullConstraintRejectsNull() {
        @SuppressWarnings("unchecked")
        List<Object[]> rows = entityManager.createNativeQuery(
                "SELECT id, document_version_id, page_id FROM document_chunks LIMIT 1"
        ).getResultList();

        if (!rows.isEmpty()) {
            Object[] first = rows.get(0);
            UUID versionId = (UUID) first[1];
            UUID pageId = (UUID) first[2];
            UUID testId = UUID.randomUUID();
            String sha64 = "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef";

            assertThatThrownBy(() -> {
                entityManager.createNativeQuery(
                        "INSERT INTO document_chunks (id, document_version_id, page_id, chunk_number, text_content, character_start, character_end, character_count, content_checksum_sha256, semantic_type, created_at) " +
                        "VALUES (:id, :versionId, :pageId, 999998, 'Test null semantic type', 0, 22, 22, :sha64, NULL, NOW())"
                )
                .setParameter("id", testId)
                .setParameter("versionId", versionId)
                .setParameter("pageId", pageId)
                .setParameter("sha64", sha64)
                .executeUpdate();
                entityManager.flush();
            }).hasMessageFindingMatch("null value in column|not-null constraint|violates not-null|is null");
        }
    }
}
