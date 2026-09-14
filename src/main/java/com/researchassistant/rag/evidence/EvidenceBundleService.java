package com.researchassistant.rag.evidence;

import com.researchassistant.rag.config.RagProperties;
import com.researchassistant.rag.scope.RetrievalScope;
import com.researchassistant.retrieval.dto.EvidenceCandidate;
import com.researchassistant.retrieval.dto.RetrievalSearchResponse;

import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class EvidenceBundleService {

    private final RagProperties properties;

    public EvidenceBundleService(RagProperties properties) {
        this.properties = properties;
    }

    public EvidenceBundle build(
            String query,
            RetrievalScope scope,
            RetrievalSearchResponse retrieval,
            int requestedLimit,
            String embeddingModel,
            String rerankerName
    ) {
        int itemLimit = requestedLimit <= 0
                ? properties.evidence().maxItems()
                : Math.min(requestedLimit, properties.evidence().maxItems());
        int totalRemaining = properties.evidence().maxTotalCharacters();
        List<EvidenceItem> items = new ArrayList<>();
        Set<UUID> seenChunks = new LinkedHashSet<>();
        for (EvidenceCandidate candidate : retrieval.candidates()) {
            if (items.size() >= itemLimit || totalRemaining <= 0) {
                break;
            }
            if (!seenChunks.add(candidate.chunkId())) {
                continue;
            }
            String text = trim(candidate.text(), Math.min(
                    properties.evidence().maxItemCharacters(),
                    totalRemaining
            ));
            if (text.isBlank()) {
                continue;
            }
            int ordinal = items.size() + 1;
            items.add(new EvidenceItem(
                    UUID.randomUUID(),
                    ordinal,
                    candidate.chunkId(),
                    candidate.workspaceId(),
                    candidate.projectId(),
                    candidate.documentId(),
                    candidate.documentCode(),
                    candidate.documentTitle(),
                    candidate.documentVersionId(),
                    candidate.versionNumber(),
                    candidate.pageNumber(),
                    candidate.chunkNumber(),
                    text,
                    candidate.lexicalScore(),
                    candidate.semanticScore(),
                    candidate.fusedScore(),
                    candidate.rerankScore(),
                    ordinal
            ));
            totalRemaining -= text.length();
        }
        return new EvidenceBundle(
                query,
                scope,
                items,
                retrieval.lexicalUsed() ? retrieval.candidates().size() : 0,
                retrieval.semanticUsed() ? retrieval.candidates().size() : 0,
                retrieval.candidates().size(),
                items.size(),
                retrieval.semanticUsed() ? "HYBRID" : "LEXICAL",
                embeddingModel,
                rerankerName,
                OffsetDateTime.now()
        );
    }

    public boolean insufficient(EvidenceBundle bundle) {
        return bundle.items().size() < Math.max(1, properties.evidence().minimumItems());
    }

    private String trim(String value, int max) {
        if (value == null) {
            return "";
        }
        String normalized = value.trim();
        if (normalized.length() <= max) {
            return normalized;
        }
        return normalized.substring(0, max);
    }
}
