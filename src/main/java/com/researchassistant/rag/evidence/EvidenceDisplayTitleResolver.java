package com.researchassistant.rag.evidence;

import com.researchassistant.document.entity.Document;
import com.researchassistant.document.util.DocumentTitleNormalizer;

public final class EvidenceDisplayTitleResolver {

    private EvidenceDisplayTitleResolver() {
    }

    public static String resolve(EvidenceItem item, Document document) {
        String itemDocumentCode = item == null ? null : item.documentCode();
        String documentCode = DocumentTitleNormalizer.normalizeDisplayTitle(itemDocumentCode);
        if (documentCode == null && document != null) {
            documentCode = DocumentTitleNormalizer.normalizeDisplayTitle(document.getDocumentCode());
        }
        String title = DocumentTitleNormalizer.firstNonBlankDisplayTitle(
                item == null ? null : item.documentTitle(),
                document == null ? null : document.getTitle(),
                document == null ? null : document.getBibliographicTitle(),
                documentCode
        );
        if (title == null) {
            throw new IllegalStateException("RAG evidence source title could not be resolved.");
        }
        return title;
    }
}
