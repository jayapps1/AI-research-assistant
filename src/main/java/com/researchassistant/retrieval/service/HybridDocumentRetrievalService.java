package com.researchassistant.retrieval.service;

import com.researchassistant.document.embedding.DocumentEmbeddingProvider;
import com.researchassistant.document.embedding.DisabledDocumentEmbeddingProvider;
import com.researchassistant.document.embedding.EmbeddingProperties;
import com.researchassistant.document.embedding.EmbeddingVector;
import com.researchassistant.document.entity.Document;
import com.researchassistant.document.entity.DocumentStatus;
import com.researchassistant.document.exception.InvalidDocumentOperationException;
import com.researchassistant.document.repository.DocumentRepository;
import com.researchassistant.identity.entity.User;
import com.researchassistant.project.service.ProjectAuthorizationContext;
import com.researchassistant.project.service.ProjectAuthorizationService;
import com.researchassistant.rag.scope.RetrievalScope;
import com.researchassistant.retrieval.dto.DocumentRetrievalMode;
import com.researchassistant.retrieval.dto.DocumentRetrievalRequest;
import com.researchassistant.retrieval.dto.EvidenceCandidate;
import com.researchassistant.retrieval.dto.RetrievalSearchResponse;
import com.researchassistant.retrieval.repository.DocumentRetrievalRepository;
import com.researchassistant.retrieval.repository.RetrievalCandidateRow;
import com.researchassistant.retrieval.rerank.EvidenceReranker;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
public class HybridDocumentRetrievalService {

    private final ProjectAuthorizationService projectAuthorizationService;
    private final DocumentRepository documentRepository;
    private final DocumentRetrievalRepository retrievalRepository;
    private final DocumentEmbeddingProvider embeddingProvider;
    private final EmbeddingProperties embeddingProperties;
    private final RetrievalProperties retrievalProperties;
    private final EvidenceReranker reranker;

    @Autowired
    public HybridDocumentRetrievalService(
            ProjectAuthorizationService projectAuthorizationService,
            DocumentRepository documentRepository,
            DocumentRetrievalRepository retrievalRepository,
            ObjectProvider<DocumentEmbeddingProvider> embeddingProvider,
            ObjectProvider<EmbeddingProperties> embeddingProperties,
            RetrievalProperties retrievalProperties,
            EvidenceReranker reranker
    ) {
        this.projectAuthorizationService = projectAuthorizationService;
        this.documentRepository = documentRepository;
        this.retrievalRepository = retrievalRepository;
        this.embeddingProperties = embeddingProperties.getIfAvailable(() -> new EmbeddingProperties(false, "none", "", null, 64));
        this.embeddingProvider = embeddingProvider.getIfAvailable(() -> new DisabledDocumentEmbeddingProvider(this.embeddingProperties));
        this.retrievalProperties = retrievalProperties;
        this.reranker = reranker;
    }

    public HybridDocumentRetrievalService(
            ProjectAuthorizationService projectAuthorizationService,
            DocumentRepository documentRepository,
            DocumentRetrievalRepository retrievalRepository,
            DocumentEmbeddingProvider embeddingProvider,
            EmbeddingProperties embeddingProperties,
            RetrievalProperties retrievalProperties,
            EvidenceReranker reranker
    ) {
        this.projectAuthorizationService = projectAuthorizationService;
        this.documentRepository = documentRepository;
        this.retrievalRepository = retrievalRepository;
        this.embeddingProperties = embeddingProperties == null ? new EmbeddingProperties(false, "none", "", null, 64) : embeddingProperties;
        this.embeddingProvider = embeddingProvider == null ? new DisabledDocumentEmbeddingProvider(this.embeddingProperties) : embeddingProvider;
        this.retrievalProperties = retrievalProperties;
        this.reranker = reranker;
    }

    @Transactional(readOnly = true)
    public RetrievalSearchResponse retrieve(
            User user,
            DocumentRetrievalRequest request
    ) {
        if (request.query() == null || request.query().isBlank()) {
            throw new InvalidDocumentOperationException("Retrieval query is required.");
        }

        ProjectAuthorizationContext context =
                projectAuthorizationService.requireProjectViewer(
                        request.projectId(),
                        user
                );
        UUID workspaceId = context.project().getWorkspace().getId();
        if (request.workspaceId() != null
                && !request.workspaceId().equals(workspaceId)) {
            throw new InvalidDocumentOperationException(
                    "Project does not belong to the requested workspace."
            );
        }

        List<UUID> scopedDocumentIds = resolveDocumentScope(request);
        int limit = effectiveLimit(request.limit());
        List<RetrievalCandidateRow> lexical = List.of();
        List<RetrievalCandidateRow> semantic = List.of();

        if (request.lexicalEnabled()) {
            lexical = retrievalRepository.lexical(
                    request.projectId(),
                    scopedDocumentIds,
                    request.query(),
                    Math.max(limit, retrievalProperties.lexicalCandidates())
            );
        }

        boolean semanticEnabled = request.semanticEnabled()
                && embeddingProperties.enabled()
                && embeddingProvider.available();
        if (semanticEnabled) {
            EmbeddingVector queryVector =
                    embeddingProvider.embed(List.of(request.query())).getFirst();
            semantic = retrievalRepository.semantic(
                    request.projectId(),
                    scopedDocumentIds,
                    embeddingProvider.provider(),
                    embeddingProvider.model(),
                    queryVector.dimensions(),
                    boxed(queryVector.values()),
                    Math.max(limit, retrievalProperties.semanticCandidates())
            );
        }

        List<EvidenceCandidate> fused = fuse(lexical, semantic, limit);
        return new RetrievalSearchResponse(
                !lexical.isEmpty(),
                !semantic.isEmpty(),
                reranker.rerank(fused)
        );
    }

    @Transactional(readOnly = true)
    public RetrievalSearchResponse retrieve(
            RetrievalScope scope,
            String query,
            int requestedLimit
    ) {
        if (query == null || query.isBlank()) {
            throw new InvalidDocumentOperationException("Retrieval query is required.");
        }
        if (scope.authorizedDocumentIds().isEmpty()
                || scope.authorizedDocumentVersionIds().isEmpty()) {
            return new RetrievalSearchResponse(false, false, List.of());
        }
        int limit = effectiveLimit(requestedLimit);
        List<RetrievalCandidateRow> lexical = retrievalRepository.lexicalScoped(
                scope.projectId(),
                scope.authorizedDocumentIds(),
                scope.authorizedDocumentVersionIds(),
                query,
                retrievalProperties.lexicalCandidates()
        );
        List<RetrievalCandidateRow> semantic = List.of();
        boolean semanticEnabled = embeddingProperties.enabled()
                && embeddingProvider.available();
        if (semanticEnabled) {
            EmbeddingVector queryVector = embeddingProvider.embed(List.of(query)).getFirst();
            semantic = retrievalRepository.semanticScoped(
                    scope.projectId(),
                    scope.authorizedDocumentIds(),
                    scope.authorizedDocumentVersionIds(),
                    embeddingProvider.provider(),
                    embeddingProvider.model(),
                    queryVector.dimensions(),
                    boxed(queryVector.values()),
                    retrievalProperties.semanticCandidates()
            );
        }
        int fusionLimit = Math.min(
                retrievalProperties.fusionCandidates(),
                retrievalProperties.rerankCandidates()
        );
        List<EvidenceCandidate> fused = fuse(lexical, semantic, fusionLimit);
        List<EvidenceCandidate> reranked =
                reranker.rerank(query, fused, Math.min(limit, retrievalProperties.rerankCandidates()));
        return new RetrievalSearchResponse(!lexical.isEmpty(), !semantic.isEmpty(), reranked);
    }

    private List<UUID> resolveDocumentScope(DocumentRetrievalRequest request) {
        if (request.mode() != DocumentRetrievalMode.SELECTED_DOCUMENTS) {
            return List.of();
        }
        Set<UUID> selected = request.documentIds();
        if (selected == null || selected.isEmpty()) {
            throw new InvalidDocumentOperationException(
                    "Selected document retrieval requires documentIds."
            );
        }
        List<Document> documents = documentRepository.findAllById(selected);
        if (documents.size() != selected.size()) {
            throw new InvalidDocumentOperationException(
                    "One or more selected documents are not available."
            );
        }
        for (Document document : documents) {
            if (!document.getProject().getId().equals(request.projectId())
                    || document.getStatus() != DocumentStatus.READY) {
                throw new InvalidDocumentOperationException(
                        "Selected documents must belong to the authorized project and be ready for retrieval."
                );
            }
        }
        return documents.stream()
                .map(Document::getId)
                .sorted()
                .toList();
    }

    private int effectiveLimit(int requested) {
        int defaultLimit = Math.max(1, retrievalProperties.finalLimit());
        int max = Math.max(defaultLimit, retrievalProperties.maxLimit());
        if (requested <= 0) {
            return defaultLimit;
        }
        if (requested > max) {
            throw new InvalidDocumentOperationException(
                    "Retrieval limit exceeds configured maximum."
            );
        }
        return requested;
    }

    private List<EvidenceCandidate> fuse(
            List<RetrievalCandidateRow> lexical,
            List<RetrievalCandidateRow> semantic,
            int limit
    ) {
        Map<UUID, MutableEvidence> byChunk = new LinkedHashMap<>();
        addChannel(byChunk, lexical, true);
        addChannel(byChunk, semantic, false);

        return byChunk.values().stream()
                .map(MutableEvidence::toCandidate)
                .sorted(Comparator
                        .comparing(EvidenceCandidate::fusedScore).reversed()
                        .thenComparing(EvidenceCandidate::documentCode)
                        .thenComparingInt(EvidenceCandidate::chunkNumber))
                .limit(limit)
                .toList();
    }

    private void addChannel(
            Map<UUID, MutableEvidence> byChunk,
            List<RetrievalCandidateRow> rows,
            boolean lexical
    ) {
        int k = Math.max(1, retrievalProperties.rrfK());
        for (int index = 0; index < rows.size(); index++) {
            RetrievalCandidateRow row = rows.get(index);
            MutableEvidence evidence = byChunk.computeIfAbsent(
                    row.chunkId(),
                    ignored -> new MutableEvidence(row)
            );
            double rrf = 1.0 / (k + index + 1);
            evidence.fusedScore += rrf;
            if (lexical) {
                evidence.lexicalScore = row.score();
            } else {
                evidence.semanticScore = row.score();
            }
        }
    }

    private Double[] boxed(double[] values) {
        List<Double> boxed = new ArrayList<>(values.length);
        for (double value : values) {
            boxed.add(value);
        }
        return boxed.toArray(Double[]::new);
    }

    private static final class MutableEvidence {
        private final RetrievalCandidateRow row;
        private Double lexicalScore;
        private Double semanticScore;
        private double fusedScore;

        private MutableEvidence(RetrievalCandidateRow row) {
            this.row = row;
        }

        private EvidenceCandidate toCandidate() {
            return new EvidenceCandidate(
                    row.chunkId(),
                    row.workspaceId(),
                    row.projectId(),
                    row.documentId(),
                    row.documentCode(),
                    row.documentVersionId(),
                    row.versionNumber(),
                    row.pageNumber(),
                    row.chunkNumber(),
                    row.text(),
                    lexicalScore,
                    semanticScore,
                    fusedScore,
                    row.documentTitle()
            );
        }
    }
}
