package com.researchassistant.retrieval.rerank;

import com.researchassistant.retrieval.dto.EvidenceCandidate;

import java.util.List;

public interface EvidenceReranker {

    default List<EvidenceCandidate> rerank(List<EvidenceCandidate> candidates) {
        return rerank("", candidates, candidates.size());
    }

    List<EvidenceCandidate> rerank(
            String query,
            List<EvidenceCandidate> candidates,
            int limit
    );
}
