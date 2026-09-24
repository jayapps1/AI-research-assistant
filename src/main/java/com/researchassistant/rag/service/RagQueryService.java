package com.researchassistant.rag.service;

import com.researchassistant.document.entity.DocumentChunk;
import com.researchassistant.document.entity.Document;
import com.researchassistant.document.entity.DocumentVersion;
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
import com.researchassistant.rag.repository.RagConversationRepository;
import com.researchassistant.rag.repository.RagQueryDocumentRepository;
import com.researchassistant.rag.repository.RagQueryEvidenceRepository;
import com.researchassistant.rag.repository.RagQueryRepository;
import com.researchassistant.rag.scope.RetrievalScope;
import com.researchassistant.rag.scope.RetrievalScopeService;
import com.researchassistant.rag.scope.RetrievalScopeType;
import com.researchassistant.rag.scope.RetrievalVersionPolicy;
import com.researchassistant.rag.entity.RagConversationStatus;
import com.researchassistant.retrieval.dto.EvidenceCandidate;
import com.researchassistant.retrieval.dto.RetrievalSearchResponse;
import com.researchassistant.retrieval.service.HybridDocumentRetrievalService;

import com.researchassistant.identity.entity.User;
import com.researchassistant.project.service.ProjectAuthorizationService;
import com.researchassistant.reference.repository.ProjectReferenceRepository;
import com.researchassistant.reference.repository.ReferenceSourceLinkRepository;
import com.researchassistant.reference.service.ProjectReferenceRegistryService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.researchassistant.ai.orchestration.AiTaskRequest;
import com.researchassistant.ai.orchestration.AiTaskResult;
import com.researchassistant.ai.orchestration.AiTaskType;
import com.researchassistant.ai.provider.AiProviderType;
import com.researchassistant.ai.usage.AiRequest;
import com.researchassistant.ai.usage.AiRequestStatus;
import com.researchassistant.ai.usage.AiUsageRecordingService;
import com.researchassistant.billing.aicredit.service.AiCreditMeter;
import com.researchassistant.billing.aicredit.service.AiCreditReservation;
import com.researchassistant.billing.aicredit.service.AiCreditService;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class RagQueryService {

    private static final Logger log = LoggerFactory.getLogger(RagQueryService.class);

    private static final String INSUFFICIENT_EVIDENCE_TEXT =
            "The selected research sources do not contain enough evidence to generate this section.";

    private final RagAuthorizationService authorizationService;
    private final ProjectAuthorizationService projectAuthorizationService;
    private final RetrievalScopeService scopeService;
    private final HybridDocumentRetrievalService retrievalService;
    private final EvidenceBundleService evidenceBundleService;
    private final GroundedAnswerGenerator answerGenerator;
    private final CitationVerificationService citationVerificationService;
    private final RagConversationRepository conversationRepository;
    private final RagQueryRepository queryRepository;
    private final RagQueryDocumentRepository queryDocumentRepository;
    private final RagQueryEvidenceRepository evidenceRepository;
    private final GroundedAnswerRepository answerRepository;
    private final AnswerCitationRepository citationRepository;
    private final DocumentChunkRepository chunkRepository;
    private final RagResponseMapper mapper;
    private final RagProperties properties;
    private final RagContextBudgetService contextBudgetService;
    private final ProjectReferenceRegistryService referenceRegistryService;
    private final ReferenceSourceLinkRepository sourceLinkRepository;
    private final ProjectReferenceRepository projectReferenceRepository;
    private final ObjectProvider<AiCreditService> creditServiceProvider;
    private final ObjectProvider<AiCreditMeter> creditMeterProvider;
    private final ObjectProvider<AiUsageRecordingService> usageRecordingServiceProvider;

    public RagQueryService(
            RagAuthorizationService authorizationService,
            ProjectAuthorizationService projectAuthorizationService,
            RetrievalScopeService scopeService,
            HybridDocumentRetrievalService retrievalService,
            EvidenceBundleService evidenceBundleService,
            ObjectProvider<GroundedAnswerGenerator> answerGenerator,
            CitationVerificationService citationVerificationService,
            RagConversationRepository conversationRepository,
            RagQueryRepository queryRepository,
            RagQueryDocumentRepository queryDocumentRepository,
            RagQueryEvidenceRepository evidenceRepository,
            GroundedAnswerRepository answerRepository,
            AnswerCitationRepository citationRepository,
            DocumentChunkRepository chunkRepository,
            RagResponseMapper mapper,
            RagProperties properties,
            RagContextBudgetService contextBudgetService,
            ProjectReferenceRegistryService referenceRegistryService,
            ReferenceSourceLinkRepository sourceLinkRepository,
            ProjectReferenceRepository projectReferenceRepository,
            ObjectProvider<AiCreditService> creditServiceProvider,
            ObjectProvider<AiCreditMeter> creditMeterProvider,
            ObjectProvider<AiUsageRecordingService> usageRecordingServiceProvider
    ) {
        this.authorizationService = authorizationService;
        this.projectAuthorizationService = projectAuthorizationService;
        this.scopeService = scopeService;
        this.retrievalService = retrievalService;
        this.evidenceBundleService = evidenceBundleService;
        this.answerGenerator = answerGenerator.getIfAvailable(DisabledGroundedAnswerGenerator::new);
        this.citationVerificationService = citationVerificationService;
        this.conversationRepository = conversationRepository;
        this.queryRepository = queryRepository;
        this.queryDocumentRepository = queryDocumentRepository;
        this.evidenceRepository = evidenceRepository;
        this.answerRepository = answerRepository;
        this.citationRepository = citationRepository;
        this.chunkRepository = chunkRepository;
        this.mapper = mapper;
        this.properties = properties;
        this.contextBudgetService = contextBudgetService;
        this.referenceRegistryService = referenceRegistryService;
        this.sourceLinkRepository = sourceLinkRepository;
        this.projectReferenceRepository = projectReferenceRepository;
        this.creditServiceProvider = creditServiceProvider;
        this.creditMeterProvider = creditMeterProvider;
        this.usageRecordingServiceProvider = usageRecordingServiceProvider;
    }

    @Transactional(noRollbackFor = {
            com.researchassistant.ai.exception.AiGenerationException.class,
            RagVerificationException.class,
            RagCapabilityUnavailableException.class
    })
    public GroundedAnswerResponse submitForProject(
            UUID projectId,
            User user,
            SubmitRagQueryRequest request,
            String conversationTitle
    ) {
        var context = projectAuthorizationService.requireProjectEditor(projectId, user);
        RagConversation conversation = new RagConversation();
        conversation.setProject(context.project());
        conversation.setCreatedBy(user);
        conversation.setTitle(conversationTitle == null || conversationTitle.isBlank() ? "Report section generation" : conversationTitle.trim());
        conversation.setStatus(RagConversationStatus.ACTIVE);
        conversation = conversationRepository.save(conversation);
        return submit(conversation.getId(), user, request);
    }

    @Transactional(noRollbackFor = {
            com.researchassistant.ai.exception.AiGenerationException.class,
            RagVerificationException.class,
            RagCapabilityUnavailableException.class
    })
    public GroundedAnswerResponse submitLiteratureReviewForProject(
            UUID projectId,
            User user,
            SubmitRagQueryRequest request,
            String conversationTitle
    ) {
        var context = projectAuthorizationService.requireProjectEditor(projectId, user);
        RagConversation conversation = new RagConversation();
        conversation.setProject(context.project());
        conversation.setCreatedBy(user);
        conversation.setTitle(conversationTitle == null || conversationTitle.isBlank() ? "Literature Review generation" : conversationTitle.trim());
        conversation.setStatus(RagConversationStatus.ACTIVE);
        conversation = conversationRepository.save(conversation);
        return submitLiteratureReview(conversation.getId(), user, request);
    }

    @Transactional(noRollbackFor = {
            com.researchassistant.ai.exception.AiGenerationException.class,
            RagVerificationException.class,
            RagCapabilityUnavailableException.class
    })
    public GroundedAnswerResponse submitLiteratureReview(
            java.util.UUID conversationId,
            User user,
            SubmitRagQueryRequest request
    ) {
        String question = request.question().trim();
        RagConversation conversation = authorizationService.requireOwnConversation(conversationId, user);
        RetrievalScopeType scopeType = request.scopeType() == null
                ? RetrievalScopeType.PROJECT_ALL_DOCUMENTS
                : request.scopeType();
        String retrievalQuestion = request.retrievalQuery() == null || request.retrievalQuery().isBlank()
                ? question
                : request.retrievalQuery().trim();
        int selectedLimit = request.evidenceLimit() == null
                ? Math.max(properties.evidence().maxItems(), 12)
                : request.evidenceLimit();
        int limit = Math.min(Math.max(selectedLimit, 1), Math.max(properties.evidence().maxItems(), 60));
        RetrievalStageResult stage = retrieveLiteratureEvidence(
                conversation,
                user,
                question,
                retrievalQuestion,
                scopeType,
                request.documentIds(),
                limit
        );
        return generateFromEvidence(stage, question, user, "MULTI_SOURCE_LITERATURE_SYNTHESIS");
    }

    @Transactional(noRollbackFor = {
            com.researchassistant.ai.exception.AiGenerationException.class,
            RagVerificationException.class,
            RagCapabilityUnavailableException.class
    })
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
        String retrievalQuestion = request.retrievalQuery() == null || request.retrievalQuery().isBlank()
                ? question
                : request.retrievalQuery().trim();
        int limit = request.evidenceLimit() == null
                ? properties.evidence().maxItems()
                : request.evidenceLimit();

        RetrievalStageResult stage = retrieveAndPersistEvidence(
                conversation,
                user,
                question,
                retrievalQuestion,
                scopeType,
                request.documentIds(),
                limit
        );

        if (evidenceBundleService.insufficient(stage.bundle())) {
            return persistInsufficientEvidence(stage.query());
        }
        return generateFromEvidence(stage, question, user, null);
    }

    private GroundedAnswerResponse generateFromEvidence(RetrievalStageResult stage, String question, User user, String forcedStrategy) {
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

        AiCreditService creditService = creditServiceProvider.getIfAvailable();
        AiCreditMeter creditMeter = creditMeterProvider.getIfAvailable();
        AiUsageRecordingService usageRecordingService = usageRecordingServiceProvider.getIfAvailable();

        UUID workspaceId = stage.bundle().scope() != null ? stage.bundle().scope().workspaceId() : null;
        AiCreditReservation reservation = null;
        int estimatedInputTokens = estimateInputTokens(question, stage.bundle());
        RagContextBudgetService.Budget budget = contextBudgetService.calculate(answerGenerator.modelName(), question, null, null);
        String generationStrategy = forcedStrategy == null ? resolveGenerationStrategy(stage.bundle()) : forcedStrategy;
        persistGenerationPreflight(stage.query(), stage.bundle(), estimatedInputTokens, budget, generationStrategy);
        log.info(
                "RAG generation preflight: correlationId={} taskType={} projectId={} selectedDocumentCount={} candidateChunkCount={} selectedEvidenceCount={} estimatedInputTokens={} configuredModel={} contextWindow={} evidenceTokenLimit={} contextBudget={} truncatedEvidenceCount={} generationStrategy={}",
                MDC.get("requestId"),
                com.researchassistant.ai.orchestration.AiTaskType.GROUNDED_QA,
                stage.bundle().scope() != null ? stage.bundle().scope().projectId() : null,
                stage.bundle().scope() != null ? stage.bundle().scope().authorizedDocumentIds().size() : 0,
                stage.bundle().rerankedCandidatesCount(),
                stage.bundle().items().size(),
                estimatedInputTokens,
                answerGenerator.modelName(),
                budget.modelContextWindow(),
                budget.maxEvidenceTokens(),
                stage.bundle().contextBudgetTokens(),
                stage.bundle().truncatedEvidenceCount(),
                generationStrategy
        );
        if (creditService != null && creditMeter != null && workspaceId != null) {
            BigDecimal estimated = creditMeter.estimateReservation(
                    "OPENAI",
                    answerGenerator.modelName(),
                    estimatedInputTokens,
                    budget.reservedOutputTokens()
            );
            reservation = creditService.reserveCredits(workspaceId, estimated);
        }

        updateStatus(stage.query(), RagQueryStatus.GENERATING);
        GeneratedAnswerDraft draft;
        List<GeneratedAnswerDraft> providerDrafts = new ArrayList<>();
        try {
            draft = answerGenerator.generate(stage.bundle());
            providerDrafts.add(draft);
        } catch (Exception e) {
            log.error(
                    "RAG generation failed: correlationId={} taskType={} projectId={} selectedDocumentCount={} retrievedChunkCount={} estimatedInputTokens={} configuredModel={} exceptionClass={} failureCode={}",
                    MDC.get("requestId"),
                    com.researchassistant.ai.orchestration.AiTaskType.GROUNDED_QA,
                    stage.bundle().scope() != null ? stage.bundle().scope().projectId() : null,
                    stage.bundle().scope() != null ? stage.bundle().scope().authorizedDocumentIds().size() : 0,
                    stage.bundle().items().size(),
                    estimatedInputTokens,
                    answerGenerator.modelName(),
                    e.getClass().getName(),
                    e instanceof com.researchassistant.ai.exception.AiGenerationException aiEx ? aiEx.getCode() : "UNEXPECTED"
            );
            if (creditService != null && reservation != null) {
                creditService.releaseReservation(reservation);
            }
            if (e instanceof com.researchassistant.ai.exception.AiGenerationException aiEx) {
                markFailed(stage.query(), aiEx.getCode(), aiEx.getMessage());
            }
            throw e;
        }

        UUID billingAiRequestId = null;
        for (GeneratedAnswerDraft providerDraft : providerDrafts) {
            UUID recordedRequestId = recordProviderUsage(usageRecordingService, user, workspaceId, question, stage.bundle(), providerDraft);
            if (billingAiRequestId == null) {
                billingAiRequestId = recordedRequestId;
            }
        }

        CitationVerificationResult initialVerification = citationVerificationService.verify(draft, evidenceRepository.findAllByQueryIdOrderByEvidenceOrdinalAsc(stage.query().getId()));
        if (!initialVerification.verified() && answerGenerator.supportsCitationRepair()) {
            log.warn(
                    "RAG citation verification failed; attempting one repair: correlationId={} requestId={} projectId={} selectedDocumentCount={} evidenceCount={} allowedEvidenceIds={} citationMarkers={} invalidCitationMarkers={} missingCitationOrdinals={} errors={}",
                    MDC.get("requestId"),
                    stage.query().getId(),
                    stage.bundle().scope() != null ? stage.bundle().scope().projectId() : null,
                    stage.bundle().scope() != null ? stage.bundle().scope().authorizedDocumentIds().size() : 0,
                    stage.bundle().items().size(),
                    allowedEvidenceIds(stage.bundle()),
                    initialVerification.citationMarkers(),
                    initialVerification.invalidCitationMarkers(),
                    initialVerification.missingCitationOrdinals(),
                    initialVerification.errors()
            );
            try {
                GeneratedAnswerDraft repaired = answerGenerator.repairCitations(stage.bundle(), draft, initialVerification);
                UUID recordedRepairRequestId = recordProviderUsage(usageRecordingService, user, workspaceId, question, stage.bundle(), repaired);
                if (billingAiRequestId == null) {
                    billingAiRequestId = recordedRepairRequestId;
                }
                draft = combineUsage(draft, repaired);
            } catch (Exception e) {
                log.warn(
                        "RAG citation repair failed: correlationId={} requestId={} projectId={} exceptionClass={}",
                        MDC.get("requestId"),
                        stage.query().getId(),
                        stage.bundle().scope() != null ? stage.bundle().scope().projectId() : null,
                        e.getClass().getName()
                );
            }
        }

        persistGenerationUsage(stage.query(), draft);
        GroundedAnswerResponse response;
        try {
            response = verifyAndPersistAnswer(stage.query().getId(), draft);
        } catch (RuntimeException e) {
            if (creditService != null && reservation != null) {
                creditService.releaseReservation(reservation);
            }
            throw e;
        }

        if (creditService != null && reservation != null) {
            if (draft.finishReason() != null && !"stop".equalsIgnoreCase(draft.finishReason()) && (draft.inputTokens() == null || draft.inputTokens() == 0)) {
                creditService.releaseReservation(reservation);
            } else {
                BigDecimal actualCredits = creditMeter != null
                        ? creditMeter.calculateCredits(draft.provider(), draft.model(), draft.inputTokens(), draft.outputTokens())
                        : BigDecimal.ZERO;
                creditService.reconcileReservation(reservation, actualCredits, billingAiRequestId, user);
            }
        }
        return response;
    }

    @Transactional
    protected RetrievalStageResult retrieveAndPersistEvidence(
            RagConversation conversation,
            User user,
            String question,
            String retrievalQuestion,
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
                retrievalService.retrieve(scope, retrievalQuestion, limit);
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
        log.info(
                "RAG retrieval complete: correlationId={} taskType={} projectId={} selectedDocumentCount={} candidateChunkCount={} retrievedChunkCount={} retrievalMode={}",
                MDC.get("requestId"),
                com.researchassistant.ai.orchestration.AiTaskType.GROUNDED_QA,
                scope.projectId(),
                scope.authorizedDocumentIds().size(),
                bundle.rerankedCandidatesCount(),
                bundle.items().size(),
                bundle.retrievalMode()
        );
        query.setRetrievalMode(bundle.retrievalMode());
        query.setRetrievalDurationMs(durationMs);
        query.setSelectedDocumentCount(scope.authorizedDocumentIds().size());
        query.setCandidateChunkCount(bundle.rerankedCandidatesCount());
        query.setSelectedEvidenceCount(bundle.items().size());
        query.setContextBudgetTokens(bundle.contextBudgetTokens());
        query.setTruncatedEvidenceCount(bundle.truncatedEvidenceCount());
        query.setStatus(RagQueryStatus.EVIDENCE_READY);
        return new RetrievalStageResult(query, bundle);
    }

    @Transactional
    protected RetrievalStageResult retrieveLiteratureEvidence(
            RagConversation conversation,
            User user,
            String question,
            String retrievalQuestion,
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
        int analyzedSources = 0;
        int sourcesWithRelevantEvidence = 0;
        boolean lexicalUsed = false;
        boolean semanticUsed = false;
        List<EvidenceCandidate> candidates = new ArrayList<>();
        int perSourceLimit = Math.max(2, Math.min(6, (int) Math.ceil((double) Math.max(1, limit) / Math.max(1, scope.authorizedDocumentIds().size()))));
        for (UUID documentId : scope.authorizedDocumentIds()) {
            UUID versionId = scope.currentVersionByDocumentId().get(documentId);
            if (versionId == null) {
                continue;
            }
            RetrievalScope singleSourceScope = new RetrievalScope(
                    scope.workspaceId(),
                    scope.projectId(),
                    RetrievalScopeType.SELECTED_DOCUMENTS,
                    RetrievalVersionPolicy.CURRENT_VERSION_ONLY,
                    List.of(documentId),
                    List.of(versionId),
                    Map.of(documentId, versionId),
                    1,
                    OffsetDateTime.now()
            );
            RetrievalSearchResponse retrieval = retrievalService.retrieve(singleSourceScope, retrievalQuestion, perSourceLimit);
            analyzedSources++;
            lexicalUsed = lexicalUsed || retrieval.lexicalUsed();
            semanticUsed = semanticUsed || retrieval.semanticUsed();
            if (!retrieval.candidates().isEmpty()) {
                sourcesWithRelevantEvidence++;
                candidates.addAll(retrieval.candidates());
            }
        }
        long durationMs = (System.nanoTime() - started) / 1_000_000L;
        EvidenceBundle bundle = evidenceBundleService.build(
                literatureSynthesisQuestion(question, scope.authorizedDocumentIds().size(), analyzedSources, sourcesWithRelevantEvidence),
                scope,
                new RetrievalSearchResponse(lexicalUsed, semanticUsed, candidates),
                limit,
                null,
                "PerSource+NoOpEvidenceReranker"
        );
        persistEvidence(query, bundle);
        log.info(
                "Literature retrieval complete: correlationId={} taskType={} projectId={} selectedSourceCount={} analyzedSourceCount={} sourcesWithRelevantEvidence={} candidateChunkCount={} finalEvidenceCount={} retrievalMode={}",
                MDC.get("requestId"),
                com.researchassistant.ai.orchestration.AiTaskType.GROUNDED_QA,
                scope.projectId(),
                scope.authorizedDocumentIds().size(),
                analyzedSources,
                sourcesWithRelevantEvidence,
                candidates.size(),
                bundle.items().size(),
                bundle.retrievalMode()
        );
        query.setRetrievalMode(bundle.retrievalMode());
        query.setRetrievalDurationMs(durationMs);
        query.setSelectedDocumentCount(scope.authorizedDocumentIds().size());
        query.setAnalyzedSourceCount(analyzedSources);
        query.setSourcesWithRelevantEvidenceCount(sourcesWithRelevantEvidence);
        query.setCandidateChunkCount(candidates.size());
        query.setSelectedEvidenceCount(bundle.items().size());
        query.setContextBudgetTokens(bundle.contextBudgetTokens());
        query.setTruncatedEvidenceCount(bundle.truncatedEvidenceCount());
        query.setStatus(RagQueryStatus.EVIDENCE_READY);
        return new RetrievalStageResult(query, bundle);
    }

    private UUID recordProviderUsage(
            AiUsageRecordingService usageRecordingService,
            User user,
            UUID workspaceId,
            String question,
            EvidenceBundle bundle,
            GeneratedAnswerDraft draft
    ) {
        if (usageRecordingService == null || draft == null) {
            return null;
        }
        UUID requestId = UUID.randomUUID();
        AiTaskRequest taskReq = new AiTaskRequest(
                AiTaskType.GROUNDED_QA,
                null,
                workspaceId,
                bundle.scope() != null ? bundle.scope().projectId() : null,
                question,
                "",
                bundle.scope(),
                bundle,
                GeneratedAnswerDraft.class,
                true
        );
        boolean isSuccess = draft.finishReason() == null || "stop".equalsIgnoreCase(draft.finishReason());
        int inTokens = draft.inputTokens() != null ? draft.inputTokens() : 0;
        int outTokens = draft.outputTokens() != null ? draft.outputTokens() : 0;
        long latencyMs = draft.generationDurationMs() != null ? draft.generationDurationMs() : 0L;
        AiTaskResult<GeneratedAnswerDraft> taskResult = isSuccess
                ? AiTaskResult.success(
                        requestId,
                        AiTaskType.GROUNDED_QA,
                        providerType(draft.provider()),
                        draft.model(),
                        draft,
                        inTokens,
                        outTokens,
                        inTokens + outTokens,
                        null,
                        latencyMs,
                        null,
                        OffsetDateTime.now(),
                        OffsetDateTime.now(),
                        java.util.Collections.emptyList()
                )
                : AiTaskResult.failure(
                        requestId,
                        AiTaskType.GROUNDED_QA,
                        providerType(draft.provider()),
                        draft.model(),
                        null,
                        draft.finishReason(),
                        latencyMs,
                        OffsetDateTime.now(),
                        OffsetDateTime.now(),
                        java.util.Collections.emptyList()
                );
        AiRequest recordedReq = usageRecordingService.recordRequest(user, taskReq, taskResult);
        return recordedReq.getId();
    }

    private AiProviderType providerType(String provider) {
        if (provider == null || provider.isBlank()) {
            return AiProviderType.OPENAI;
        }
        try {
            return AiProviderType.valueOf(provider.trim().toUpperCase(java.util.Locale.ROOT));
        } catch (IllegalArgumentException ignored) {
            return AiProviderType.OPENAI;
        }
    }

    private GeneratedAnswerDraft combineUsage(GeneratedAnswerDraft original, GeneratedAnswerDraft repaired) {
        return new GeneratedAnswerDraft(
                repaired.answerText(),
                repaired.citations(),
                repaired.provider(),
                repaired.model(),
                sum(original.inputTokens(), repaired.inputTokens()),
                sum(original.outputTokens(), repaired.outputTokens()),
                sumLong(original.generationDurationMs(), repaired.generationDurationMs()),
                repaired.finishReason()
        );
    }

    private Integer sum(Integer first, Integer second) {
        int total = (first == null ? 0 : first) + (second == null ? 0 : second);
        return total == 0 && first == null && second == null ? null : total;
    }

    private Long sumLong(Long first, Long second) {
        long total = (first == null ? 0L : first) + (second == null ? 0L : second);
        return total == 0L && first == null && second == null ? null : total;
    }

    private List<String> allowedEvidenceIds(EvidenceBundle bundle) {
        return bundle.items().stream()
                .map(item -> "E" + item.evidenceOrdinal())
                .toList();
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
            query.setFailureCode("AI_CITATION_VERIFICATION_FAILED");
            query.setFailureMessage(String.join("; ", verification.errors()));
            log.warn(
                    "RAG citation verification failed: requestId={} queryId={} projectId={} selectedDocumentCount={} analyzedSourceCount={} evidenceCount={} allowedEvidenceIds={} citationMarkersProduced={} citationOrdinalsParsed={} invalidCitationMarkers={} missingCitationOrdinals={} evidenceMappings={} referenceMetadataAvailability={} errors={}",
                    MDC.get("requestId"),
                    queryId,
                    query.getConversation() != null && query.getConversation().getProject() != null ? query.getConversation().getProject().getId() : null,
                    query.getSelectedDocumentCount(),
                    query.getAnalyzedSourceCount(),
                    evidence.size(),
                    evidence.stream().map(item -> "E" + item.getEvidenceOrdinal()).toList(),
                    verification.citationMarkers(),
                    verification.citedEvidenceOrdinals(),
                    verification.invalidCitationMarkers(),
                    verification.missingCitationOrdinals(),
                    evidenceMappings(evidence),
                    referenceMetadataAvailability(query, evidence),
                    verification.errors()
            );
            throw new RagVerificationException("Generated answer citation verification failed.");
        }
        log.debug(
                "RAG citation verification succeeded: requestId={} queryId={} evidenceCount={} allowedEvidenceIds={} citationMarkersProduced={} citationOrdinalsParsed={} evidenceMappings={} referenceMetadataAvailability={}",
                MDC.get("requestId"),
                queryId,
                evidence.size(),
                evidence.stream().map(item -> "E" + item.getEvidenceOrdinal()).toList(),
                verification.citationMarkers(),
                verification.citedEvidenceOrdinals(),
                evidenceMappings(evidence),
                referenceMetadataAvailability(query, evidence)
        );
        answer.setStatus(GroundedAnswerStatus.VERIFIED);
        answer = answerRepository.save(answer);
        Map<Integer, RagQueryEvidence> evidenceByOrdinal = evidence.stream()
                .collect(Collectors.toMap(RagQueryEvidence::getEvidenceOrdinal, Function.identity()));
        List<AnswerCitation> citations = new ArrayList<>();
        int citationOrdinal = 1;
        for (Integer evidenceOrdinal : verification.citedEvidenceOrdinals()) {
            RagQueryEvidence queryEvidence =
                    evidenceByOrdinal.get(evidenceOrdinal);
            if (queryEvidence == null) {
                continue;
            }
            AnswerCitation citation = new AnswerCitation();
            citation.setAnswer(answer);
            citation.setEvidence(queryEvidence);
            citation.setCitationOrdinal(citationOrdinal++);
            citation.setClaimText("Evidence reference E" + evidenceOrdinal);
            citation.setSupportingTextSnapshot(queryEvidence.getTextSnapshot());
            citations.add(citation);
        }
        citationRepository.saveAll(citations);
        query.setStatus(RagQueryStatus.COMPLETED);
        query.setCompletedAt(OffsetDateTime.now());
        return mapper.answer(query, answer, citations,
                distinctDocumentCount(evidence), evidence.size());
    }

    private List<Map<String, Object>> evidenceMappings(List<RagQueryEvidence> evidence) {
        return evidence.stream()
                .map(item -> {
                    Map<String, Object> mapping = new java.util.LinkedHashMap<>();
                    mapping.put("evidenceId", "E" + item.getEvidenceOrdinal());
                    mapping.put("documentId", item.getDocumentId());
                    mapping.put("documentCode", item.getDocumentCode());
                    mapping.put("documentVersionId", item.getDocumentVersionId());
                    mapping.put("pageNumber", item.getPageNumber());
                    mapping.put("chunkNumber", item.getChunkNumber());
                    return mapping;
                })
                .toList();
    }

    private List<Map<String, Object>> referenceMetadataAvailability(RagQuery query, List<RagQueryEvidence> evidence) {
        UUID projectId = query.getConversation() == null || query.getConversation().getProject() == null
                ? null
                : query.getConversation().getProject().getId();
        return evidence.stream()
                .collect(Collectors.toMap(RagQueryEvidence::getDocumentId, Function.identity(), (first, ignored) -> first))
                .values()
                .stream()
                .map(item -> {
                    Map<String, Object> mapping = new java.util.LinkedHashMap<>();
                    mapping.put("documentId", item.getDocumentId());
                    mapping.put("documentCode", item.getDocumentCode());
                    sourceLinkRepository.findFirstByDocumentId(item.getDocumentId()).ifPresentOrElse(link -> {
                        var reference = link.getReference();
                        mapping.put("referenceId", reference.getId());
                        mapping.put("projectReferenceLinked", projectId != null && projectReferenceRepository
                                .findByProjectIdAndReferenceId(projectId, reference.getId())
                                .isPresent());
                        mapping.put("hasTitle", reference.getTitle() != null && !reference.getTitle().isBlank());
                        mapping.put("hasYear", reference.getPublicationYear() != null);
                        mapping.put("metadataStatus", reference.getMetadataStatus());
                    }, () -> {
                        mapping.put("referenceId", null);
                        mapping.put("projectReferenceLinked", false);
                        mapping.put("hasTitle", false);
                        mapping.put("hasYear", false);
                        mapping.put("metadataStatus", "MISSING_REFERENCE_LINK");
                    });
                    return mapping;
                })
                .toList();
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
        Map<UUID, Boolean> registeredDocumentIds = new java.util.HashMap<>();
        for (EvidenceItem item : bundle.items()) {
            RagQueryEvidence snapshot = new RagQueryEvidence();
            snapshot.setQuery(query);
            DocumentChunk chunk = chunkRepository.getReferenceById(item.chunkId());
            DocumentVersion version = chunk.getDocumentVersion();
            Document document = version.getDocument();
            if (!registeredDocumentIds.containsKey(document.getId())) {
                referenceRegistryService.ensureForDocument(document, version, query.getCreatedBy());
                registeredDocumentIds.put(document.getId(), true);
            }
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

    private String literatureSynthesisQuestion(String question, int selectedSources, int analyzedSources, int sourcesWithRelevantEvidence) {
        return question + """

                Literature synthesis source coverage:
                selectedSources = %d
                analyzedSources = %d
                sourcesWithRelevantEvidence = %d

                Write an integrated, scholarly Literature Review narrative organized by themes, conceptual frameworks, agreements, disagreements, methodological patterns, empirical findings, and identified research gaps.
                CRITICAL INSTRUCTION: Do NOT output an evidence assessment table, source extraction table, or "Source-level evidence assessment" matrix in this report chapter (analytical matrix tables belong exclusively in the analytical literature matrix). Output pure, polished academic prose structured into logical thematic subsections. Do not write a repeated paper-by-paper summary. Cite source-grounded claims with the supplied evidence markers only.
                """.formatted(selectedSources, analyzedSources, sourcesWithRelevantEvidence);
    }

    private int estimateInputTokens(String question, EvidenceBundle bundle) {
        int questionChars = question == null ? 0 : question.length();
        int evidenceChars = bundle == null || bundle.items() == null
                ? 0
                : bundle.items().stream()
                        .map(EvidenceItem::text)
                        .filter(java.util.Objects::nonNull)
                        .mapToInt(String::length)
                        .sum();
        int promptOverheadTokens = 800;
        return Math.max(500, ((questionChars + evidenceChars) / 4) + promptOverheadTokens);
    }

    @Transactional
    protected void persistGenerationPreflight(
            RagQuery query,
            EvidenceBundle bundle,
            int estimatedInputTokens,
            RagContextBudgetService.Budget budget,
            String generationStrategy
    ) {
        RagQuery managed = queryRepository.findById(query.getId()).orElse(query);
        managed.setSelectedDocumentCount(bundle.scope() != null ? bundle.scope().authorizedDocumentIds().size() : 0);
        managed.setCandidateChunkCount(bundle.rerankedCandidatesCount());
        managed.setSelectedEvidenceCount(bundle.items().size());
        managed.setEstimatedInputTokens(estimatedInputTokens);
        managed.setContextBudgetTokens(budget.maxEvidenceTokens());
        managed.setTruncatedEvidenceCount(bundle.truncatedEvidenceCount());
        managed.setGenerationStrategy(generationStrategy);
        managed.setConfiguredModel(answerGenerator.modelName());
        queryRepository.save(managed);
    }

    @Transactional
    protected void persistGenerationUsage(RagQuery query, GeneratedAnswerDraft draft) {
        RagQuery managed = queryRepository.findById(query.getId()).orElse(query);
        managed.setActualInputTokens(draft.inputTokens());
        managed.setActualOutputTokens(draft.outputTokens());
        managed.setConfiguredModel(draft.model() == null ? answerGenerator.modelName() : draft.model());
        queryRepository.save(managed);
    }

    private String resolveGenerationStrategy(EvidenceBundle bundle) {
        return bundle != null && bundle.truncatedEvidenceCount() > 0 && bundle.items().size() >= properties.evidence().maxItems()
                ? "DIRECT_RAG_BUDGETED"
                : "DIRECT_RAG";
    }

    private record RetrievalStageResult(RagQuery query, EvidenceBundle bundle) {
    }
}
