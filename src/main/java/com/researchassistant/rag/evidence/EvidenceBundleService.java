package com.researchassistant.rag.evidence;

import com.researchassistant.rag.config.RagProperties;
import com.researchassistant.rag.scope.RetrievalScope;
import com.researchassistant.rag.service.RagContextBudgetService;
import com.researchassistant.ai.config.AiProperties;
import com.researchassistant.retrieval.dto.EvidenceCandidate;
import com.researchassistant.retrieval.dto.RetrievalSearchResponse;

import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class EvidenceBundleService {

    private final RagProperties properties;
    private final AiProperties aiProperties;
    private final RagContextBudgetService contextBudgetService;

    public EvidenceBundleService(
            RagProperties properties,
            AiProperties aiProperties,
            RagContextBudgetService contextBudgetService
    ) {
        this.properties = properties;
        this.aiProperties = aiProperties;
        this.contextBudgetService = contextBudgetService;
    }

    public EvidenceBundle build(
            String query,
            RetrievalScope scope,
            RetrievalSearchResponse retrieval,
            int requestedLimit,
            String embeddingModel,
            String rerankerName
    ) {
        boolean perSourceLiteraturePass = rerankerName != null && rerankerName.toLowerCase(java.util.Locale.ROOT).contains("persource");
        int configuredMax = properties.evidence().maxItems();
        int effectiveMax = perSourceLiteraturePass
                ? Math.max(configuredMax, Math.min(Math.max(requestedLimit, configuredMax), 60))
                : configuredMax;
        int itemLimit = requestedLimit <= 0
                ? configuredMax
                : Math.min(requestedLimit, effectiveMax);
        RagContextBudgetService.Budget budget = contextBudgetService.calculate(
                aiProperties.generation().model(),
                query,
                null,
                null
        );
        int totalRemainingTokens = budget.maxEvidenceTokens();
        int totalRemaining = properties.evidence().maxTotalCharacters();
        List<EvidenceItem> items = new ArrayList<>();
        Set<UUID> seenChunks = new LinkedHashSet<>();
        int selectedEvidenceTokens = 0;
        List<EvidenceCandidate> diversified = diversifiedCandidates(retrieval.candidates(), itemLimit);
        for (EvidenceCandidate candidate : diversified) {
            if (items.size() >= itemLimit || totalRemaining <= 0 || totalRemainingTokens <= 0) {
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
            int evidenceTokens = contextBudgetService.estimateEvidenceTokens(text);
            if (evidenceTokens > totalRemainingTokens) {
                text = trimToTokenBudget(text, totalRemainingTokens);
                evidenceTokens = contextBudgetService.estimateEvidenceTokens(text);
            }
            if (text.isBlank() || evidenceTokens > totalRemainingTokens) {
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
            totalRemainingTokens -= evidenceTokens;
            selectedEvidenceTokens += evidenceTokens;
        }
        int truncatedEvidenceCount = Math.max(0, retrieval.candidates().size() - items.size());
        return new EvidenceBundle(
                query,
                scope,
                items,
                retrieval.lexicalUsed() ? retrieval.candidates().size() : 0,
                retrieval.semanticUsed() ? retrieval.candidates().size() : 0,
                retrieval.candidates().size(),
                items.size(),
                budget.maxEvidenceTokens(),
                selectedEvidenceTokens,
                truncatedEvidenceCount,
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

    private List<EvidenceCandidate> diversifiedCandidates(List<EvidenceCandidate> candidates, int itemLimit) {
        if (candidates == null || candidates.size() <= 1 || itemLimit <= 1) {
            return candidates == null ? List.of() : candidates;
        }
        Map<UUID, EvidenceCandidate> bestByDocument = new LinkedHashMap<>();
        for (EvidenceCandidate candidate : candidates) {
            bestByDocument.putIfAbsent(candidate.documentId(), candidate);
        }
        if (bestByDocument.size() <= 1) {
            return candidates;
        }
        Set<UUID> used = new LinkedHashSet<>();
        List<EvidenceCandidate> result = new ArrayList<>();
        int diversitySlots = Math.min(bestByDocument.size(), Math.max(2, itemLimit / 2));
        bestByDocument.values().stream()
                .sorted(Comparator.comparing(EvidenceCandidate::fusedScore, Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(diversitySlots)
                .forEach(candidate -> {
                    result.add(candidate);
                    used.add(candidate.chunkId());
                });
        for (EvidenceCandidate candidate : candidates) {
            if (used.add(candidate.chunkId())) {
                result.add(candidate);
            }
        }
        return result;
    }

    private String trimToTokenBudget(String text, int tokenBudget) {
        if (text == null || tokenBudget <= 0) {
            return "";
        }
        int maxChars = Math.max(0, tokenBudget * 4);
        return trim(text, Math.min(text.length(), maxChars));
    }
}
