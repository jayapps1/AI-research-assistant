package com.researchassistant.retrieval.rerank;

import com.researchassistant.retrieval.dto.EvidenceCandidate;

import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;

@Component
public class NoOpEvidenceReranker implements EvidenceReranker {

    @Override
    public List<EvidenceCandidate> rerank(
            String query,
            List<EvidenceCandidate> candidates,
            int limit
    ) {
        return candidates.stream()
                .sorted(Comparator
                        .comparing(EvidenceCandidate::fusedScore,
                                Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(EvidenceCandidate::documentCode)
                        .thenComparingInt(EvidenceCandidate::chunkNumber)
                        .thenComparing(EvidenceCandidate::chunkId))
                .limit(Math.max(0, limit))
                .map(candidate -> candidate.withRerankScore(candidate.fusedScore()))
                .toList();
    }
}
