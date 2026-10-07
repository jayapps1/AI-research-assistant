package com.researchassistant.rag.evidence;

import com.researchassistant.document.entity.Document;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class EvidenceDisplayTitleResolverTest {

    @Test
    void usesEvidenceItemDisplayTitleFirst() {
        Document document = document("Actual Document Title", "Bibliographic Title", "DOC-001");

        assertThat(EvidenceDisplayTitleResolver.resolve(item("Evidence Display Title", "DOC-001"), document))
                .isEqualTo("Evidence Display Title");
    }

    @Test
    void fallsBackToActualDocumentTitleWhenLegacyRetrievalTitleIsMissing() {
        Document document = document("Actual Document Title", "Bibliographic Title", "DOC-001");

        assertThat(EvidenceDisplayTitleResolver.resolve(item(null, "DOC-001"), document))
                .isEqualTo("Actual Document Title");
    }

    @Test
    void fallsBackToBibliographicTitleWhenDisplayTitleIsMissing() {
        Document document = document(null, "Bibliographic Title", "DOC-001");

        assertThat(EvidenceDisplayTitleResolver.resolve(item(null, "DOC-001"), document))
                .isEqualTo("Bibliographic Title");
    }

    @Test
    void fallsBackToDocumentCodeWhenBothTitlesAreMissing() {
        Document document = document(null, null, "DOC-001");

        assertThat(EvidenceDisplayTitleResolver.resolve(item(null, "DOC-001"), document))
                .isEqualTo("DOC-001");
    }

    @Test
    void neverReturnsNullWhenDocumentCodeExists() {
        assertThat(EvidenceDisplayTitleResolver.resolve(item(null, "DOC-001"), null))
                .isEqualTo("DOC-001");
    }

    private EvidenceItem item(String documentTitle, String documentCode) {
        return new EvidenceItem(
                UUID.randomUUID(),
                1,
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                documentCode,
                documentTitle,
                UUID.randomUUID(),
                1,
                1,
                1,
                "Evidence text",
                1.0d,
                null,
                1.0d,
                null,
                1
        );
    }

    private Document document(String title, String bibliographicTitle, String documentCode) {
        Document document = new Document();
        document.setTitle(title);
        document.setBibliographicTitle(bibliographicTitle);
        document.setDocumentCode(documentCode);
        return document;
    }
}
