package com.researchassistant.rag.service;

import com.researchassistant.document.entity.DocumentChunk;
import com.researchassistant.document.repository.DocumentChunkRepository;
import com.researchassistant.rag.citation.CitationVerificationResult;
import com.researchassistant.rag.citation.CitationVerificationService;
import com.researchassistant.rag.config.RagProperties;
import com.researchassistant.rag.dto.request.SubmitRagQueryRequest;
import com.researchassistant.rag.dto.response.GroundedAnswerResponse;
import com.researchassistant.rag.dto.response.RagEvidenceResponse;
import com.researchassistant.rag.entity.AnswerCitation;
import com.researchassistant.rag.entity.GroundedAnswer;
import com.researchassistant.rag.entity.GroundedAnswerStatus;
import com.researchassistant.rag.entity.RagConversation;
import com.researchassistant.rag.entity.RagQuery;
import com.researchassistant.rag.entity.RagQueryDocument;
import com.researchassistant.rag.entity.RagQueryEvidence;
import com.researchassistant.rag.entity.RagQueryStatus;
import com.researchassistant.rag.evidence.EvidenceBundle;
import com.researchassistant.rag.evidence.EvidenceBundleService;
import com.researchassistant.rag.evidence.EvidenceItem;
import com.researchassistant.rag.exception.RagCapabilityUnavailableException;
import com.researchassistant.rag.exception.RagVerificationException;
import com.researchassistant.rag.generation.GeneratedAnswerDraft;
import com.researchassistant.rag.generation.GeneratedCitation;
import com.researchassistant.rag.generation.DisabledGroundedAnswerGenerator;
import com.researchassistant.rag.generation.GroundedAnswerGenerator;
import com.researchassistant.rag.repository.AnswerCitationRepository;
import com.researchassistant.rag.repository.GroundedAnswerRepository;
import com.researchassistant.rag.repository.RagQueryDocumentRepository;
import com.researchassistant.rag.repository.RagQueryEvidenceRepository;
import com.researchassistant.rag.repository.RagQueryRepository;
import com.researchassistant.rag.scope.RetrievalScope;
import com.researchassistant.rag.scope.RetrievalScopeService;
import com.researchassistant.rag.scope.RetrievalScopeType;
import com.researchassistant.retrieval.dto.RetrievalSearchResponse;
import com.researchassistant.retrieval.service.HybridDocumentRetrievalService;

import com.researchassistant.identity.entity.User;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class RagQueryService {

    private static final String INSUFFICIENT_EVIDENCE_TEXT =
            "I could not find enough evidence in the selected research sources to answer this question.";

    private final RagAuthorizationService authorizationService;
    private final RetrievalScopeService scopeService;
    private final HybridDocumentRetrievalService retrievalService;
    private final EvidenceBundleService evidenceBundleService;
    private final GroundedAnswerGenerator answerGenerator;
    private final CitationVerificationService citationVerificationService;
    private final RagQueryRepository queryRepository;
    private final RagQueryDocumentRepository queryDocumentRepository;
    private final RagQueryEvidenceRepository evidenceRepository;
    private final GroundedAnswerRepository answerRepository;
    private final AnswerCitationRepository citationRepository;
    private final DocumentChunkRepository chunkRepository;
    private final RagResponseMapper mapper;
    private final RagProperties properties;

    public RagQueryService(
            RagAuthorizationService authorizationService,
            RetrievalScopeService scopeService,
            HybridDocumentRetrievalService retrievalService,
            EvidenceBundleService evidenceBundleService,
            ObjectProvider<GroundedAnswerGenerator> answerGenerator,
            CitationVerificationService citationVerificationService,
            RagQueryRepository queryRepository,
            RagQueryDocumentRepository queryDocumentRepository,
            RagQueryEvidenceRepository evidenceRepository,
            GroundedAnswerRepository answerRepository,
            AnswerCitationRepository citationRepository,
            DocumentChunkRepository chunkRepository,
            RagResponseMapper mapper,
            RagProperties properties
    ) {
        this.authorizationService = authorizationService;
        this.scopeService = scopeService;
        this.retrievalService = retrievalService;
        this.evidenceBundleService = evidenceBundleService;
        this.answerGenerator = answerGenerator.getIfAvailable(DisabledGroundedAnswerGenerator::new);
        this.citationVerificationService = citationVerificationService;
        this.queryRepository = queryRepository;
        this.queryDocumentRepository = queryDocumentRepository;
        this.evidenceRepository = evidenceRepository;
        this.answerRepository = answerRepository;
        this.citationRepository = citationRepository;
        this.chunkRepository = chunkRepository;
        this.mapper = mapper;
        this.properties = properties;
    }

    @Transactional
    public GroundedAnswerResponse submit(
            java.util.UUID conversationId,
            User user,
            SubmitRagQueryRequest request
    ) {
        String question = request.question().trim();
        RagConversation conversation =
                authorizationService.requireOwnConversation(conversationId, user);
        RetrievalScopeType scopeType = request.scopeType() == null
                ? RetrievalScopeType.PROJECT_ALL_DOCUMENTS
                : request.scopeType();
        int limit = request.evidenceLimit() == null
                ? properties.evidence().maxItems()
                : request.evidenceLimit();

        RetrievalStageResult stage = retrieveAndPersistEvidence(
                conversation,
                user,
                question,
                scopeType,
                request.documentIds(),
                limit
        );

        if (evidenceBundleService.insufficient(stage.bundle())) {
            return persistInsufficientEvidence(stage.query());
        }
        if (!answerGenerator.available()) {
            markFailed(stage.query(), "GENERATION_DISABLED",
                    "Grounded answer generation is disabled.");
            throw new RagCapabilityUnavailableException(
                    "Grounded answer generation is not enabled."
            );
        }

        updateStatus(stage.query(), RagQueryStatus.GENERATING);
        GeneratedAnswerDraft draft = answerGenerator.generate(stage.bundle());
        return verifyAndPersistAnswer(stage.query().getId(), draft);
    }

    @Transactional
    protected RetrievalStageResult retrieveAndPersistEvidence(
            RagConversation conversation,
            User user,
            String question,
            RetrievalScopeType scopeType,
            java.util.Set<java.util.UUID> documentIds,
            int limit
    ) {
        RagQuery query = new RagQuery();
        query.setConversation(conversation);
        query.setCreatedBy(user);
        query.setQuestion(question);
        query.setScopeType(scopeType);
        query.setStatus(RagQueryStatus.RECEIVED);
        query.setRequestedEvidenceLimit(limit);
        query = queryRepository.save(query);

        RetrievalScope scope = scopeService.resolve(
                user,
                conversation.getProject().getId(),
                scopeType,
                documentIds
        );
        persistResolvedDocuments(query, scope);
        query.setStatus(RagQueryStatus.RETRIEVING);
        long started = System.nanoTime();
        RetrievalSearchResponse retrieval =
                retrievalService.retrieve(scope, question, limit);
        long durationMs = (System.nanoTime() - started) / 1_000_000L;
        EvidenceBundle bundle = evidenceBundleService.build(
                question,
                scope,
                retrieval,
                limit,
                null,
                "NoOpEvidenceReranker"
        );
        persistEvidence(query, bundle);
        query.setRetrievalMode(bundle.retrievalMode());
        query.setRetrievalDurationMs(durationMs);
        query.setStatus(RagQueryStatus.EVIDENCE_READY);
        return new RetrievalStageResult(query, bundle);
    }

    @Transactional
    protected GroundedAnswerResponse persistInsufficientEvidence(RagQuery query) {
        RagQuery managed = queryRepository.findWithConversationById(query.getId()).orElse(query);
        managed.setStatus(RagQueryStatus.INSUFFICIENT_EVIDENCE);
        managed.setCompletedAt(OffsetDateTime.now());
        GroundedAnswer answer = new GroundedAnswer();
        answer.setQuery(managed);
        answer.setAnswerText(INSUFFICIENT_EVIDENCE_TEXT);
        answer.setStatus(GroundedAnswerStatus.INSUFFICIENT_EVIDENCE);
        answer = answerRepository.save(answer);
        return mapper.answer(managed, answer, List.of(), 0,
                evidenceRepository.findAllByQueryIdOrderByEvidenceOrdinalAsc(managed.getId()).size());
    }

    @Transactional
    protected GroundedAnswerResponse verifyAndPersistAnswer(
            java.util.UUID queryId,
            GeneratedAnswerDraft draft
    ) {
        RagQuery query = queryRepository.findWithConversationById(queryId)
                .orElseThrow();
        List<RagQueryEvidence> evidence =
                evidenceRepository.findAllByQueryIdOrderByEvidenceOrdinalAsc(queryId);
        CitationVerificationResult verification =
                citationVerificationService.verify(draft, evidence);
        GroundedAnswer answer = new GroundedAnswer();
        answer.setQuery(query);
        answer.setAnswerText(draft.answerText());
        answer.setProvider(draft.provider());
        answer.setModel(draft.model());
        answer.setInputTokens(draft.inputTokens());
        answer.setOutputTokens(draft.outputTokens());
        answer.setGenerationDurationMs(draft.generationDurationMs());
        answer.setFinishReason(draft.finishReason());
        if (!verification.verified()) {
            answer.setStatus(GroundedAnswerStatus.REJECTED_UNGROUNDED);
            answerRepository.save(answer);
            query.setStatus(RagQueryStatus.FAILED);
            query.setCompletedAt(OffsetDateTime.now());
            query.setFailureCode("CITATION_VERIFICATION_FAILED");
            query.setFailureMessage(String.join("; ", verification.errors()));
            throw new RagVerificationException("Generated answer citation verification failed.");
        }
        answer.setStatus(GroundedAnswerStatus.VERIFIED);
        answer = answerRepository.save(answer);
        Map<Integer, RagQueryEvidence> evidenceByOrdinal = evidence.stream()
                .collect(Collectors.toMap(RagQueryEvidence::getEvidenceOrdinal, Function.identity()));
        List<AnswerCitation> citations = new ArrayList<>();
        int citationOrdinal = 1;
        for (GeneratedCitation generatedCitation : draft.citations()) {
            RagQueryEvidence queryEvidence =
                    evidenceByOrdinal.get(generatedCitation.evidenceOrdinal());
            AnswerCitation citation = new AnswerCitation();
            citation.setAnswer(answer);
            citation.setEvidence(queryEvidence);
            citation.setCitationOrdinal(citationOrdinal++);
            citation.setClaimText(generatedCitation.claimText());
            citation.setSupportingTextSnapshot(queryEvidence.getTextSnapshot());
            citations.add(citation);
        }
        citationRepository.saveAll(citations);
        query.setStatus(RagQueryStatus.COMPLETED);
        query.setCompletedAt(OffsetDateTime.now());
        return mapper.answer(query, answer, citations,
                distinctDocumentCount(evidence), evidence.size());
    }

    @Transactional
    protected void updateStatus(RagQuery query, RagQueryStatus status) {
        RagQuery managed = queryRepository.findById(query.getId()).orElse(query);
        managed.setStatus(status);
        queryRepository.save(managed);
    }

    @Transactional
    protected void markFailed(RagQuery query, String code, String message) {
        RagQuery managed = queryRepository.findById(query.getId()).orElse(query);
        managed.setStatus(RagQueryStatus.FAILED);
        managed.setCompletedAt(OffsetDateTime.now());
        managed.setFailureCode(code);
        managed.setFailureMessage(message);
        queryRepository.save(managed);
    }

    @Transactional(readOnly = true)
    public GroundedAnswerResponse get(java.util.UUID queryId, User user) {
        RagQuery query = authorizationService.requireOwnQuery(queryId, user);
        GroundedAnswer answer = answerRepository.findByQueryId(queryId).orElse(null);
        List<AnswerCitation> citations = answer == null
                ? List.of()
                : citationRepository.findAllByAnswerIdOrderByCitationOrdinalAsc(answer.getId());
        List<RagQueryEvidence> evidence =
                evidenceRepository.findAllByQueryIdOrderByEvidenceOrdinalAsc(queryId);
        return mapper.answer(query, answer, citations,
                distinctDocumentCount(evidence), evidence.size());
    }

    @Transactional(readOnly = true)
    public List<RagEvidenceResponse> evidence(java.util.UUID queryId, User user) {
        authorizationService.requireOwnQuery(queryId, user);
        return evidenceRepository.findWithTraceByQueryIdOrderByEvidenceOrdinalAsc(queryId)
                .stream()
                .map(mapper::evidence)
                .toList();
    }

    private void persistResolvedDocuments(RagQuery query, RetrievalScope scope) {
        List<RagQueryDocument> documents = new ArrayList<>();
        scope.currentVersionByDocumentId().forEach((documentId, versionId) -> {
            RagQueryDocument document = new RagQueryDocument();
            document.setQuery(query);
            document.setDocumentId(documentId);
            document.setDocumentVersionId(versionId);
            documents.add(document);
        });
        queryDocumentRepository.saveAll(documents);
    }

    private void persistEvidence(RagQuery query, EvidenceBundle bundle) {
        List<RagQueryEvidence> evidence = new ArrayList<>();
        for (EvidenceItem item : bundle.items()) {
            RagQueryEvidence snapshot = new RagQueryEvidence();
            snapshot.setQuery(query);
            DocumentChunk chunk = chunkRepository.getReferenceById(item.chunkId());
            snapshot.setChunk(chunk);
            snapshot.setDocumentId(item.documentId());
            snapshot.setDocumentCode(item.documentCode());
            snapshot.setDocumentTitle(item.documentTitle());
            snapshot.setDocumentVersionId(item.documentVersionId());
            snapshot.setVersionNumber(item.versionNumber());
            snapshot.setPageNumber(item.pageNumber());
            snapshot.setChunkNumber(item.chunkNumber());
            snapshot.setEvidenceOrdinal(item.evidenceOrdinal());
            snapshot.setTextSnapshot(item.text());
            snapshot.setLexicalScore(item.lexicalScore());
            snapshot.setSemanticScore(item.semanticScore());
            snapshot.setFusedScore(item.fusedScore());
            snapshot.setRerankScore(item.rerankScore());
            evidence.add(snapshot);
        }
        evidenceRepository.saveAll(evidence);
    }

    private int distinctDocumentCount(List<RagQueryEvidence> evidence) {
        return (int) evidence.stream()
                .map(RagQueryEvidence::getDocumentId)
                .distinct()
                .count();
    }

    private record RetrievalStageResult(RagQuery query, EvidenceBundle bundle) {
    }
}
