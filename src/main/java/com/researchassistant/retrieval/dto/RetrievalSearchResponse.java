package com.researchassistant.retrieval.dto;

import java.util.List;

public record RetrievalSearchResponse(
        boolean lexicalUsed,
        boolean semanticUsed,
        List<EvidenceCandidate> candidates
) {
}
