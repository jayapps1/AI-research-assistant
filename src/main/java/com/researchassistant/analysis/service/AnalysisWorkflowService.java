package com.researchassistant.analysis.service;

import com.researchassistant.analysis.dto.AnalysisDtos.*;
import com.researchassistant.analysis.entity.*;
import com.researchassistant.analysis.exception.ReportValidationException;
import com.researchassistant.analysis.repository.*;
import com.researchassistant.cache.CacheInvalidationService;
import com.researchassistant.common.enums.ContentOrigin;
import com.researchassistant.common.exception.ResourceNotFoundException;
import com.researchassistant.dataset.model.ResearchDataset;
import com.researchassistant.dataset.repository.ResearchDatasetRepository;
import com.researchassistant.document.entity.DocumentStatus;
import com.researchassistant.document.repository.DocumentRepository;
import com.researchassistant.identity.entity.User;
import com.researchassistant.methodology.repository.MethodologyRepository;
import com.researchassistant.project.entity.ResearchProject;
import com.researchassistant.project.service.ProjectAuthorizationService;
import com.researchassistant.rag.dto.request.SubmitRagQueryRequest;
import com.researchassistant.rag.dto.response.GroundedAnswerResponse;
import com.researchassistant.rag.exception.RagCapabilityUnavailableException;
import com.researchassistant.rag.entity.RagQueryEvidence;
import com.researchassistant.rag.repository.RagQueryEvidenceRepository;
import com.researchassistant.rag.scope.RetrievalScopeType;
import com.researchassistant.rag.service.RagQueryService;
import com.researchassistant.reference.entity.ProjectReference;
import com.researchassistant.reference.entity.ProjectReferenceStatus;
import com.researchassistant.reference.entity.ReferenceEntry;
import com.researchassistant.reference.entity.ReferenceMetadataStatus;
import com.researchassistant.reference.repository.ProjectReferenceRepository;
import com.researchassistant.reference.repository.ReferenceSourceLinkRepository;
import com.researchassistant.reference.service.CitationFormattingService;
import com.researchassistant.reference.service.ProjectReferenceRegistryService;
import com.researchassistant.reference.dto.ReferenceDtos.CitationContext;
import com.researchassistant.literature.entity.LiteratureMatrix;
import com.researchassistant.literature.repository.LiteratureMatrixRepository;
import com.researchassistant.researchdesign.entity.*;
import com.researchassistant.researchdesign.repository.*;
import com.researchassistant.security.audit.SecurityAuditEventType;
import com.researchassistant.security.audit.SecurityAuditService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.OffsetDateTime;
import java.util.*;

@Service
public class AnalysisWorkflowService {
    private final AnalysisRunRepository runRepository;
    private final AnalysisResultRepository resultRepository;
    private final ResearchFindingRepository findingRepository;
    private final FindingDiscussionRepository discussionRepository;
    private final DiscussionEvidenceRepository discussionEvidenceRepository;
    private final ResearchConclusionRepository conclusionRepository;
    private final ResearchRecommendationRepository recommendationRepository;
    private final ResearchReportRepository reportRepository;
    private final ResearchReportTemplateRepository templateRepository;
    private final ResearchReportChapterRepository chapterRepository;
    private final ResearchReportSectionRepository sectionRepository;
    private final ResearchReportCitationRepository citationRepository;
    private final ReportDocumentVersionRepository documentVersionRepository;
    private final ResearchObjectiveRepository objectiveRepository;
    private final ResearchQuestionRepository questionRepository;
    private final ResearchHypothesisRepository hypothesisRepository;
    private final ResearchProblemRepository problemRepository;
    private final MethodologyRepository methodologyRepository;
    private final ResearchDatasetRepository datasetRepository;
    private final DocumentRepository documentRepository;
    private final ProjectAuthorizationService authorizationService;
    private final CacheInvalidationService cacheInvalidationService;
    private final SecurityAuditService auditService;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final RagQueryService ragQueryService;
    private final RagQueryEvidenceRepository ragEvidenceRepository;
    private final ProjectReferenceRegistryService referenceRegistryService;
    private final ReportMarkdownRenderer markdownRenderer;
    private final ReportRichTextService richTextService;
    private final CitationFormattingService citationFormattingService;
    private final ProjectReferenceRepository projectReferenceRepository;
    private final ReferenceSourceLinkRepository referenceSourceLinkRepository;
    private final LiteratureMatrixRepository literatureMatrixRepository;
    private final TransactionTemplate transactionTemplate;

    public AnalysisWorkflowService(AnalysisRunRepository runRepository, AnalysisResultRepository resultRepository,
            ResearchFindingRepository findingRepository, FindingDiscussionRepository discussionRepository,
            DiscussionEvidenceRepository discussionEvidenceRepository, ResearchConclusionRepository conclusionRepository,
            ResearchRecommendationRepository recommendationRepository, ResearchReportRepository reportRepository,
            ResearchReportTemplateRepository templateRepository, ResearchReportChapterRepository chapterRepository,
            ResearchReportSectionRepository sectionRepository, ResearchReportCitationRepository citationRepository,
            ReportDocumentVersionRepository documentVersionRepository,
            ResearchObjectiveRepository objectiveRepository, ResearchQuestionRepository questionRepository,
            ResearchHypothesisRepository hypothesisRepository, ResearchProblemRepository problemRepository,
            MethodologyRepository methodologyRepository, ResearchDatasetRepository datasetRepository,
            DocumentRepository documentRepository,
            ProjectAuthorizationService authorizationService, CacheInvalidationService cacheInvalidationService,
            SecurityAuditService auditService, RagQueryService ragQueryService,
            RagQueryEvidenceRepository ragEvidenceRepository, ProjectReferenceRegistryService referenceRegistryService,
            ReportMarkdownRenderer markdownRenderer, ReportRichTextService richTextService,
            CitationFormattingService citationFormattingService, ProjectReferenceRepository projectReferenceRepository,
            ReferenceSourceLinkRepository referenceSourceLinkRepository, LiteratureMatrixRepository literatureMatrixRepository,
            PlatformTransactionManager transactionManager) {
        this.runRepository = runRepository;
        this.resultRepository = resultRepository;
        this.findingRepository = findingRepository;
        this.discussionRepository = discussionRepository;
        this.discussionEvidenceRepository = discussionEvidenceRepository;
        this.conclusionRepository = conclusionRepository;
        this.recommendationRepository = recommendationRepository;
        this.reportRepository = reportRepository;
        this.templateRepository = templateRepository;
        this.chapterRepository = chapterRepository;
        this.sectionRepository = sectionRepository;
        this.citationRepository = citationRepository;
        this.documentVersionRepository = documentVersionRepository;
        this.objectiveRepository = objectiveRepository;
        this.questionRepository = questionRepository;
        this.hypothesisRepository = hypothesisRepository;
        this.problemRepository = problemRepository;
        this.methodologyRepository = methodologyRepository;
        this.datasetRepository = datasetRepository;
        this.documentRepository = documentRepository;
        this.authorizationService = authorizationService;
        this.cacheInvalidationService = cacheInvalidationService;
        this.auditService = auditService;
        this.ragQueryService = ragQueryService;
        this.ragEvidenceRepository = ragEvidenceRepository;
        this.referenceRegistryService = referenceRegistryService;
        this.markdownRenderer = markdownRenderer;
        this.richTextService = richTextService;
        this.citationFormattingService = citationFormattingService;
        this.projectReferenceRepository = projectReferenceRepository;
        this.referenceSourceLinkRepository = referenceSourceLinkRepository;
        this.literatureMatrixRepository = literatureMatrixRepository;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    @Transactional
    public AnalysisRunResponse createRun(UUID projectId, User user, CreateAnalysisRunRequest request) {
        ResearchProject project = authorizationService.requireProjectEditor(projectId, user).project();
        AnalysisRun run = new AnalysisRun();
        run.setProject(project);
        run.setObjective(loadObjective(request.objectiveId(), projectId));
        run.setQuestion(loadQuestion(request.questionId(), projectId));
        run.setHypothesis(loadHypothesis(request.hypothesisId(), projectId));
        run.setDataset(loadDataset(request.datasetId(), projectId));
        run.setQualitativeSourceType(request.qualitativeSourceType());
        run.setQualitativeSourceReference(request.qualitativeSourceReference());
        if (run.getDataset() == null && run.getQualitativeSourceReference() == null) throw new IllegalArgumentException("Analysis requires a dataset or qualitative source.");
        run.setTitle(required(request.title(), "Analysis title is required."));
        run.setAnalysisType(request.analysisType());
        run.setMethodDescription(optional(request.methodDescription()));
        run.setParametersJson(optional(request.parametersJson()));
        run.setInitiatedBy(user);
        return AnalysisRunResponse.from(runRepository.save(run));
    }

    @Transactional(readOnly = true)
    public Page<AnalysisRunResponse> listRuns(UUID projectId, User user, Pageable pageable) {
        authorizationService.requireProjectViewer(projectId, user);
        return runRepository.findAllByProjectId(projectId, pageable).map(AnalysisRunResponse::from);
    }

    @Transactional
    public AnalysisResultResponse completeRun(UUID runId, User user, CompleteAnalysisRunRequest request) {
        AnalysisRun run = runRepository.findById(runId).orElseThrow(() -> new ResourceNotFoundException("Analysis run not found."));
        authorizationService.requireProjectEditor(run.getProject().getId(), user);
        run.setStatus(AnalysisRun.Status.COMPLETED);
        run.setCompletedAt(OffsetDateTime.now());
        AnalysisResult result = new AnalysisResult();
        result.setAnalysisRun(run);
        result.setProject(run.getProject());
        result.setSummary(required(request.summary(), "Analysis summary is required."));
        result.setResultPayloadJson(optional(request.resultPayloadJson()));
        result.setLimitations(optional(request.limitations()));
        result.setGeneratedBy(defaultOrigin(request.generatedBy()));
        result.setCreatedBy(user);
        return AnalysisResultResponse.from(resultRepository.save(result));
    }

    @Transactional
    public FindingResponse createFinding(UUID projectId, User user, CreateFindingRequest request) {
        ResearchProject project = authorizationService.requireProjectEditor(projectId, user).project();
        ResearchFinding finding = new ResearchFinding();
        finding.setProject(project);
        finding.setObjective(loadObjective(request.objectiveId(), projectId));
        finding.setQuestion(loadQuestion(request.questionId(), projectId));
        finding.setHypothesis(loadHypothesis(request.hypothesisId(), projectId));
        List<AnalysisResult> results = loadAnalysisResults(request.analysisResultIds(), projectId);
        finding.getAnalysisResults().addAll(results);
        finding.setAnalysisResult(results.getFirst());
        finding.setType(request.type() == null ? ResearchFindingType.OTHER : request.type());
        finding.setTitle(required(request.title(), "Finding title is required."));
        setFindingText(finding, request.findingText());
        finding.setEvidenceSummary(optional(request.evidenceSummary()));
        finding.setResultValueSnapshot(optional(request.resultValueSnapshot()));
        finding.setDisplayOrder(request.displayOrder() == null ? 1 : request.displayOrder());
        finding.setOrigin(defaultOrigin(request.origin()));
        finding.setCreatedBy(user);
        ResearchFinding saved = findingRepository.save(finding);
        afterResearchOutputChanged(projectId, user.getId(), SecurityAuditEventType.RESEARCH_FINDING_CREATED);
        return FindingResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public Page<FindingResponse> listFindings(UUID projectId, User user, UUID objectiveId, UUID questionId,
            UUID hypothesisId, ResearchFindingStatus status, ResearchFindingType type, Pageable pageable) {
        authorizationService.requireProjectViewer(projectId, user);
        List<FindingResponse> filtered = findingRepository.findAllByProjectIdOrderByDisplayOrderAsc(projectId, Pageable.unpaged()).getContent().stream()
                .filter(f -> objectiveId == null || (f.getObjective() != null && objectiveId.equals(f.getObjective().getId())))
                .filter(f -> questionId == null || (f.getQuestion() != null && questionId.equals(f.getQuestion().getId())))
                .filter(f -> hypothesisId == null || (f.getHypothesis() != null && hypothesisId.equals(f.getHypothesis().getId())))
                .filter(f -> status == null || status == f.getStatus())
                .filter(f -> type == null || type == f.getType())
                .map(FindingResponse::from)
                .toList();
        int start = (int) Math.min(pageable.getOffset(), filtered.size());
        int end = Math.min(start + pageable.getPageSize(), filtered.size());
        return new PageImpl<>(filtered.subList(start, end), pageable, filtered.size());
    }

    @Transactional(readOnly = true)
    public FindingResponse getFinding(UUID findingId, User user) {
        ResearchFinding finding = loadFinding(findingId);
        authorizationService.requireProjectViewer(finding.getProject().getId(), user);
        return FindingResponse.from(finding);
    }

    @Transactional
    public FindingResponse updateFinding(UUID findingId, User user, UpdateFindingRequest request) {
        ResearchFinding finding = loadFindingForEdit(findingId, user);
        requireNotArchived(finding.getStatus());
        if (request.objectiveId() != null) finding.setObjective(loadObjective(request.objectiveId(), finding.getProject().getId()));
        if (request.questionId() != null) finding.setQuestion(loadQuestion(request.questionId(), finding.getProject().getId()));
        if (request.hypothesisId() != null) finding.setHypothesis(loadHypothesis(request.hypothesisId(), finding.getProject().getId()));
        if (request.type() != null) finding.setType(request.type());
        if (optional(request.title()) != null) finding.setTitle(optional(request.title()));
        if (optional(request.findingText()) != null) setFindingText(finding, request.findingText());
        if (request.evidenceSummary() != null) finding.setEvidenceSummary(optional(request.evidenceSummary()));
        if (request.resultValueSnapshot() != null) finding.setResultValueSnapshot(optional(request.resultValueSnapshot()));
        if (request.displayOrder() != null) finding.setDisplayOrder(request.displayOrder());
        if (request.origin() != null) finding.setOrigin(request.origin());
        finding.setUpdatedBy(user);
        finding.setRevisionNumber(finding.getRevisionNumber() + 1);
        ResearchFinding saved = findingRepository.save(finding);
        markSectionsStale("ResearchFinding", finding.getId());
        afterResearchOutputChanged(finding.getProject().getId(), user.getId(), SecurityAuditEventType.RESEARCH_FINDING_REVISED);
        return FindingResponse.from(saved);
    }

    @Transactional
    public FindingResponse approveFinding(UUID findingId, User user) {
        ResearchFinding finding = loadFinding(findingId);
        authorizationService.requireProjectAdminAccess(finding.getProject().getId(), user);
        finding.setStatus(ResearchFindingStatus.APPROVED);
        finding.setUpdatedBy(user);
        finding.setRevisionNumber(finding.getRevisionNumber() + 1);
        afterResearchOutputChanged(finding.getProject().getId(), user.getId(), SecurityAuditEventType.RESEARCH_FINDING_APPROVED);
        return FindingResponse.from(finding);
    }

    @Transactional
    public FindingResponse archiveFinding(UUID findingId, User user) {
        ResearchFinding finding = loadFindingForEdit(findingId, user);
        finding.setStatus(ResearchFindingStatus.ARCHIVED);
        finding.setUpdatedBy(user);
        finding.setRevisionNumber(finding.getRevisionNumber() + 1);
        afterResearchOutputChanged(finding.getProject().getId(), user.getId(), SecurityAuditEventType.RESEARCH_FINDING_REVISED);
        return FindingResponse.from(finding);
    }

    public GeneratedDraftResponse generateFinding(UUID projectId, User user, GenerateFindingRequest request) {
        authorizationService.requireProjectEditor(projectId, user);
        loadObjective(request.objectiveId(), projectId);
        loadAnalysisResults(request.analysisResultIds(), projectId);
        throw new RagCapabilityUnavailableException("AI finding drafting is disabled. Deterministic analysis results remain authoritative.");
    }

    @Transactional
    public DiscussionResponse createDiscussion(UUID findingId, User user, CreateDiscussionRequest request) {
        ResearchFinding finding = loadFinding(findingId);
        authorizationService.requireProjectEditor(finding.getProject().getId(), user);
        FindingDiscussion discussion = new FindingDiscussion();
        discussion.setProject(finding.getProject());
        discussion.setFinding(finding);
        discussion.setTitle(optional(request.title()));
        setDiscussionText(discussion, request.discussionText());
        discussion.setRelationToLiterature(optional(request.relationToLiterature()));
        discussion.setImplications(optional(request.implications()));
        discussion.setLimitations(optional(request.limitations()));
        discussion.setDisplayOrder(request.displayOrder() == null ? 1 : request.displayOrder());
        discussion.setOrigin(defaultOrigin(request.origin()));
        discussion.setCreatedBy(user);
        FindingDiscussion saved = discussionRepository.save(discussion);
        afterResearchOutputChanged(finding.getProject().getId(), user.getId(), SecurityAuditEventType.DISCUSSION_CREATED);
        return DiscussionResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public List<DiscussionResponse> listDiscussions(UUID findingId, User user) {
        ResearchFinding finding = loadFinding(findingId);
        authorizationService.requireProjectViewer(finding.getProject().getId(), user);
        return discussionRepository.findAllByFindingIdOrderByDisplayOrderAsc(findingId).stream().map(DiscussionResponse::from).toList();
    }

    @Transactional
    public DiscussionResponse updateDiscussion(UUID discussionId, User user, UpdateDiscussionRequest request) {
        FindingDiscussion discussion = loadDiscussion(discussionId);
        authorizationService.requireProjectEditor(discussion.getProject().getId(), user);
        if (discussion.getStatus() == DiscussionStatus.ARCHIVED) throw new IllegalStateException("Archived discussions cannot be updated.");
        if (request.title() != null) discussion.setTitle(optional(request.title()));
        if (request.discussionText() != null) setDiscussionText(discussion, request.discussionText());
        if (request.relationToLiterature() != null) discussion.setRelationToLiterature(optional(request.relationToLiterature()));
        if (request.implications() != null) discussion.setImplications(optional(request.implications()));
        if (request.limitations() != null) discussion.setLimitations(optional(request.limitations()));
        if (request.status() != null) discussion.setStatus(request.status());
        if (request.displayOrder() != null) discussion.setDisplayOrder(request.displayOrder());
        if (request.origin() != null) discussion.setOrigin(request.origin());
        discussion.setUpdatedBy(user);
        discussion.setRevisionNumber(discussion.getRevisionNumber() + 1);
        markSectionsStale("DiscussionOfFinding", discussion.getId());
        afterResearchOutputChanged(discussion.getProject().getId(), user.getId(), SecurityAuditEventType.REPORT_SECTION_UPDATED);
        return DiscussionResponse.from(discussion);
    }

    public GeneratedDraftResponse generateDiscussion(UUID findingId, User user) {
        ResearchFinding finding = loadFinding(findingId);
        authorizationService.requireProjectEditor(finding.getProject().getId(), user);
        throw new RagCapabilityUnavailableException("AI discussion drafting is disabled. Use grounded evidence and citation verification before persisting discussion prose.");
    }

    @Transactional(readOnly = true)
    public List<DiscussionEvidenceResponse> discussionEvidence(UUID discussionId, User user) {
        FindingDiscussion discussion = loadDiscussion(discussionId);
        authorizationService.requireProjectViewer(discussion.getProject().getId(), user);
        return discussionEvidenceRepository.findAllByDiscussionIdOrderByCitationOrdinalAsc(discussionId).stream().map(DiscussionEvidenceResponse::from).toList();
    }

    @Transactional
    public DiscussionResponse approveDiscussion(UUID discussionId, User user) {
        FindingDiscussion discussion = loadDiscussion(discussionId);
        authorizationService.requireProjectAdminAccess(discussion.getProject().getId(), user);
        discussion.setStatus(DiscussionStatus.APPROVED);
        discussion.setUpdatedBy(user);
        discussion.setRevisionNumber(discussion.getRevisionNumber() + 1);
        afterResearchOutputChanged(discussion.getProject().getId(), user.getId(), SecurityAuditEventType.DISCUSSION_APPROVED);
        return DiscussionResponse.from(discussion);
    }

    @Transactional
    public ConclusionResponse createConclusion(UUID projectId, User user, CreateConclusionRequest request) {
        ResearchProject project = authorizationService.requireProjectEditor(projectId, user).project();
        ResearchConclusion conclusion = new ResearchConclusion();
        conclusion.setProject(project);
        conclusion.setObjective(loadObjective(request.objectiveId(), projectId));
        conclusion.setQuestion(loadQuestion(request.questionId(), projectId));
        conclusion.setType(request.type() == null ? ResearchConclusionType.OBJECTIVE_SPECIFIC : request.type());
        conclusion.getFindings().addAll(loadFindings(request.findingIds(), projectId, true));
        conclusion.setTitle(optional(request.title()) == null ? "Conclusion" : optional(request.title()));
        setConclusionText(conclusion, request.conclusionText());
        conclusion.setScopeNote(optional(request.scopeNote()));
        conclusion.setDisplayOrder(request.displayOrder() == null ? 1 : request.displayOrder());
        conclusion.setOrigin(defaultOrigin(request.origin()));
        conclusion.setCreatedBy(user);
        ResearchConclusion saved = conclusionRepository.save(conclusion);
        afterResearchOutputChanged(projectId, user.getId(), SecurityAuditEventType.CONCLUSION_CREATED);
        return ConclusionResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public Page<ConclusionResponse> listConclusions(UUID projectId, User user, Pageable pageable) {
        authorizationService.requireProjectViewer(projectId, user);
        return conclusionRepository.findAllByProjectIdOrderByDisplayOrderAsc(projectId, pageable).map(ConclusionResponse::from);
    }

    @Transactional(readOnly = true)
    public ConclusionResponse getConclusion(UUID conclusionId, User user) {
        ResearchConclusion conclusion = loadConclusion(conclusionId);
        authorizationService.requireProjectViewer(conclusion.getProject().getId(), user);
        return ConclusionResponse.from(conclusion);
    }

    @Transactional
    public ConclusionResponse updateConclusion(UUID conclusionId, User user, UpdateConclusionRequest request) {
        ResearchConclusion conclusion = loadConclusion(conclusionId);
        authorizationService.requireProjectEditor(conclusion.getProject().getId(), user);
        if (conclusion.getStatus() == ResearchConclusionStatus.ARCHIVED) throw new IllegalStateException("Archived conclusions cannot be updated.");
        UUID projectId = conclusion.getProject().getId();
        if (request.objectiveId() != null) conclusion.setObjective(loadObjective(request.objectiveId(), projectId));
        if (request.questionId() != null) conclusion.setQuestion(loadQuestion(request.questionId(), projectId));
        if (request.type() != null) conclusion.setType(request.type());
        if (request.findingIds() != null) { conclusion.getFindings().clear(); conclusion.getFindings().addAll(loadFindings(request.findingIds(), projectId, true)); }
        if (request.title() != null) conclusion.setTitle(optional(request.title()));
        if (request.conclusionText() != null) setConclusionText(conclusion, request.conclusionText());
        if (request.scopeNote() != null) conclusion.setScopeNote(optional(request.scopeNote()));
        if (request.displayOrder() != null) conclusion.setDisplayOrder(request.displayOrder());
        if (request.origin() != null) conclusion.setOrigin(request.origin());
        conclusion.setUpdatedBy(user);
        conclusion.setRevisionNumber(conclusion.getRevisionNumber() + 1);
        markSectionsStale("ResearchConclusion", conclusion.getId());
        afterResearchOutputChanged(projectId, user.getId(), SecurityAuditEventType.REPORT_SECTION_UPDATED);
        return ConclusionResponse.from(conclusion);
    }

    @Transactional
    public ConclusionResponse approveConclusion(UUID conclusionId, User user) {
        ResearchConclusion conclusion = loadConclusion(conclusionId);
        authorizationService.requireProjectAdminAccess(conclusion.getProject().getId(), user);
        if (conclusion.getFindings().isEmpty()) throw new IllegalStateException("A conclusion must be supported by at least one finding.");
        conclusion.setStatus(ResearchConclusionStatus.APPROVED);
        conclusion.setUpdatedBy(user);
        conclusion.setRevisionNumber(conclusion.getRevisionNumber() + 1);
        afterResearchOutputChanged(conclusion.getProject().getId(), user.getId(), SecurityAuditEventType.CONCLUSION_APPROVED);
        return ConclusionResponse.from(conclusion);
    }

    public GeneratedDraftResponse generateConclusion(UUID projectId, User user) {
        authorizationService.requireProjectEditor(projectId, user);
        throw new RagCapabilityUnavailableException("AI conclusion drafting is disabled. Conclusions must stay within approved findings.");
    }

    @Transactional
    public RecommendationResponse createRecommendation(UUID projectId, User user, CreateRecommendationRequest request) {
        ResearchProject project = authorizationService.requireProjectEditor(projectId, user).project();
        ResearchRecommendation recommendation = new ResearchRecommendation();
        recommendation.setProject(project);
        recommendation.setType(request.type() == null ? ResearchRecommendationType.OTHER : request.type());
        recommendation.setTitle(optional(request.title()) == null ? "Recommendation" : optional(request.title()));
        setRecommendationText(recommendation, request.recommendationText());
        recommendation.setTargetAudience(optional(request.targetAudience()));
        recommendation.setAudience(optional(request.targetAudience()));
        recommendation.setPriority(request.priority() == null ? RecommendationPriority.MEDIUM : request.priority());
        recommendation.getFindings().addAll(loadFindings(nullToEmpty(request.findingIds()), projectId, false));
        recommendation.getConclusions().addAll(loadConclusions(nullToEmpty(request.conclusionIds()), projectId, false));
        if (recommendation.getFindings().isEmpty() && recommendation.getConclusions().isEmpty()) throw new IllegalArgumentException("Recommendation requires at least one supporting finding or conclusion.");
        recommendation.setRationale(optional(request.rationale()));
        recommendation.setDisplayOrder(request.displayOrder() == null ? 1 : request.displayOrder());
        recommendation.setOrigin(defaultOrigin(request.origin()));
        recommendation.setCreatedBy(user);
        ResearchRecommendation saved = recommendationRepository.save(recommendation);
        afterResearchOutputChanged(projectId, user.getId(), SecurityAuditEventType.RECOMMENDATION_CREATED);
        return RecommendationResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public Page<RecommendationResponse> listRecommendations(UUID projectId, User user, Pageable pageable) {
        authorizationService.requireProjectViewer(projectId, user);
        return recommendationRepository.findAllByProjectIdOrderByDisplayOrderAsc(projectId, pageable).map(RecommendationResponse::from);
    }

    @Transactional(readOnly = true)
    public RecommendationResponse getRecommendation(UUID recommendationId, User user) {
        ResearchRecommendation recommendation = loadRecommendation(recommendationId);
        authorizationService.requireProjectViewer(recommendation.getProject().getId(), user);
        return RecommendationResponse.from(recommendation);
    }

    @Transactional
    public RecommendationResponse updateRecommendation(UUID recommendationId, User user, UpdateRecommendationRequest request) {
        ResearchRecommendation recommendation = loadRecommendation(recommendationId);
        authorizationService.requireProjectEditor(recommendation.getProject().getId(), user);
        if (recommendation.getStatus() == ResearchRecommendationStatus.ARCHIVED) throw new IllegalStateException("Archived recommendations cannot be updated.");
        UUID projectId = recommendation.getProject().getId();
        if (request.type() != null) recommendation.setType(request.type());
        if (request.title() != null) recommendation.setTitle(optional(request.title()));
        if (request.recommendationText() != null) setRecommendationText(recommendation, request.recommendationText());
        if (request.targetAudience() != null) { recommendation.setTargetAudience(optional(request.targetAudience())); recommendation.setAudience(optional(request.targetAudience())); }
        if (request.priority() != null) recommendation.setPriority(request.priority());
        if (request.findingIds() != null) { recommendation.getFindings().clear(); recommendation.getFindings().addAll(loadFindings(request.findingIds(), projectId, false)); }
        if (request.conclusionIds() != null) { recommendation.getConclusions().clear(); recommendation.getConclusions().addAll(loadConclusions(request.conclusionIds(), projectId, false)); }
        if (request.rationale() != null) recommendation.setRationale(optional(request.rationale()));
        if (request.displayOrder() != null) recommendation.setDisplayOrder(request.displayOrder());
        if (request.origin() != null) recommendation.setOrigin(request.origin());
        recommendation.setUpdatedBy(user);
        recommendation.setRevisionNumber(recommendation.getRevisionNumber() + 1);
        markSectionsStale("ResearchRecommendation", recommendation.getId());
        afterResearchOutputChanged(projectId, user.getId(), SecurityAuditEventType.REPORT_SECTION_UPDATED);
        return RecommendationResponse.from(recommendation);
    }

    @Transactional
    public RecommendationResponse approveRecommendation(UUID recommendationId, User user) {
        ResearchRecommendation recommendation = loadRecommendation(recommendationId);
        authorizationService.requireProjectAdminAccess(recommendation.getProject().getId(), user);
        if (recommendation.getFindings().isEmpty() && recommendation.getConclusions().isEmpty()) throw new IllegalStateException("A recommendation requires supporting findings or conclusions.");
        recommendation.setStatus(ResearchRecommendationStatus.APPROVED);
        recommendation.setUpdatedBy(user);
        recommendation.setRevisionNumber(recommendation.getRevisionNumber() + 1);
        afterResearchOutputChanged(recommendation.getProject().getId(), user.getId(), SecurityAuditEventType.RECOMMENDATION_APPROVED);
        return RecommendationResponse.from(recommendation);
    }

    public GeneratedDraftResponse generateRecommendation(UUID projectId, User user) {
        authorizationService.requireProjectEditor(projectId, user);
        throw new RagCapabilityUnavailableException("AI recommendation drafting is disabled. Recommendations must be traceable to findings or conclusions.");
    }

    @Transactional
    public ReportResponse createReport(UUID projectId, User user, CreateReportRequest request) {
        ResearchProject project = authorizationService.requireProjectEditor(projectId, user).project();
        ResearchReport report = new ResearchReport();
        report.setProject(project);
        report.setTitle(required(request.title(), "Report title is required."));
        report.setType(request.type() == null ? ResearchReportType.FINAL_YEAR_PROJECT : request.type());
        ResearchReportTemplate template = request.templateId() != null
                ? loadTemplate(request.templateId(), report.getType())
                : project.getReportTemplate() != null
                        ? project.getReportTemplate()
                        : loadTemplate(null, report.getType());
        report.setTemplate(template);
        report.setInstitutionName(optional(request.institutionName()));
        report.setDepartmentName(optional(request.departmentName()));
        report.setAuthorName(optional(request.authorName()));
        report.setSupervisorName(optional(request.supervisorName()));
        report.setDegreeProgram(optional(request.degreeProgram()));
        report.setSubmissionYear(request.submissionYear());
        CitationStyle citationStyle = request.citationStyle() != null
                ? request.citationStyle()
                : project.getCitationStyle() != null
                        ? project.getCitationStyle()
                        : template != null && template.getDefaultCitationStyle() != null
                                ? template.getDefaultCitationStyle()
                                : CitationStyle.APA_7;
        report.setCitationStyle(citationStyle);
        report.setOrigin(defaultOrigin(request.origin()));
        report.setCreatedBy(user);
        ResearchReport saved = reportRepository.save(report);
        ensureReportStructure(saved, user);
        auditService.record(user.getId(), SecurityAuditEventType.REPORT_CREATED);
        return ReportResponse.from(saved);
    }

    @Transactional
    public ReportResponse ensureReportInitialized(UUID projectId, User user) {
        ResearchProject project = authorizationService.requireProjectEditor(projectId, user).project();
        ResearchReport report = reportRepository.findFirstByProjectIdOrderByUpdatedAtDesc(projectId)
                .orElseGet(() -> createInitializedReport(project, user));
        if (report.getTemplate() == null) {
            ResearchReportTemplate template = project.getReportTemplate() != null
                    ? project.getReportTemplate()
                    : loadTemplate(null, report.getType());
            report.setTemplate(template);
        }
        if (report.getCitationStyle() == null) {
            report.setCitationStyle(project.getCitationStyle() != null ? project.getCitationStyle() : CitationStyle.APA_7);
        }
        ensureReportStructure(report, user);
        backfillProjectReferences(projectId, user);
        return ReportResponse.from(report);
    }

    @Transactional
    public Page<ReportResponse> listReports(UUID projectId, User user, Pageable pageable) {
        authorizationService.requireProjectViewer(projectId, user);
        Page<ReportResponse> page = reportRepository.findAllByProjectIdOrderByUpdatedAtDesc(projectId, pageable).map(ReportResponse::from);
        if (page.isEmpty()) {
            try {
                ReportResponse initialized = ensureReportInitialized(projectId, user);
                return new PageImpl<>(List.of(initialized), pageable, 1);
            } catch (Exception ignored) {
                // User may not have editor permissions to initialize, return empty
            }
        }
        return page;
    }

    @Transactional(readOnly = true)
    public ReportResponse getReport(UUID reportId, User user) {
        ResearchReport report = loadReport(reportId);
        authorizationService.requireProjectViewer(report.getProject().getId(), user);
        return ReportResponse.from(report);
    }

    @Transactional
    public ReportResponse updateReport(UUID reportId, User user, UpdateReportRequest request) {
        ResearchReport report = loadReportForEdit(reportId, user);
        if (request.title() != null) report.setTitle(optional(request.title()));
        if (request.institutionName() != null) report.setInstitutionName(optional(request.institutionName()));
        if (request.departmentName() != null) report.setDepartmentName(optional(request.departmentName()));
        if (request.authorName() != null) report.setAuthorName(optional(request.authorName()));
        if (request.supervisorName() != null) report.setSupervisorName(optional(request.supervisorName()));
        if (request.degreeProgram() != null) report.setDegreeProgram(optional(request.degreeProgram()));
        if (request.submissionYear() != null) report.setSubmissionYear(request.submissionYear());
        if (request.citationStyle() != null) report.setCitationStyle(request.citationStyle());
        report.setRevisionNumber(report.getRevisionNumber() + 1);
        cacheInvalidationService.evictProjectMetadata(report.getProject().getId());
        return ReportResponse.from(report);
    }

    @Transactional
    public ReportResponse assembleReport(UUID reportId, User user) {
        ResearchReport report = loadReportForEdit(reportId, user);
        ensureReportStructure(report, user);
        auditService.record(user.getId(), SecurityAuditEventType.REPORT_ASSEMBLED);
        return ReportResponse.from(report);
    }

    private ResearchReport createInitializedReport(ResearchProject project, User user) {
        ResearchReportTemplate template = project.getReportTemplate() != null
                ? project.getReportTemplate()
                : loadTemplate(null, ResearchReportType.FINAL_YEAR_PROJECT);
        ResearchReport report = new ResearchReport();
        report.setProject(project);
        report.setTemplate(template);
        report.setTitle(project.getTitle() == null || project.getTitle().isBlank() ? "Research Report" : project.getTitle() + " Report");
        report.setType(template == null ? ResearchReportType.FINAL_YEAR_PROJECT : template.getType());
        report.setCitationStyle(project.getCitationStyle() != null
                ? project.getCitationStyle()
                : template != null && template.getDefaultCitationStyle() != null ? template.getDefaultCitationStyle() : CitationStyle.APA_7);
        report.setInstitutionName(template == null ? null : template.getInstitution());
        report.setDepartmentName(template == null ? null : template.getDepartment());
        report.setOrigin(ContentOrigin.USER);
        report.setCreatedBy(user);
        return reportRepository.save(report);
    }

    private void ensureReportStructure(ResearchReport report, User user) {
        boolean assembled = false;
        if (report.getTemplate() != null && report.getTemplate().getConfigurationJson() != null && !report.getTemplate().getConfigurationJson().isBlank()) {
            try {
                ensureFromTemplate(report, user, report.getTemplate().getConfigurationJson());
                assembled = true;
            } catch (Exception ignored) {
                assembled = false;
            }
        }
        if (!assembled) {
            ensureDefaultChapters(report, user);
        }
        linkPersistedResearchEntities(report, user);
        cleanLiteratureReviewAndExtractMatrix(report, user);
        ensureReferencesSection(report, user);
        recalculateSectionNumbers(report);
        for (ResearchReportSection section : sectionRepository.findAllByChapterReportId(report.getId())) {
            ensureRichSection(section);
        }
    }

    private void ensureFromTemplate(ResearchReport report, User user, String configurationJson) throws Exception {
        JsonNode root = objectMapper.readTree(configurationJson);
        JsonNode chaptersNode = root.get("chapters");
        if (chaptersNode == null || !chaptersNode.isArray()) {
            ensureDefaultChapters(report, user);
            return;
        }

        List<ResearchReportChapter> existingChapters = new ArrayList<>(chapterRepository.findAllByReportIdOrderByDisplayOrderAsc(report.getId()));
        int chOrder = 1;
        for (JsonNode chNode : chaptersNode) {
            ReportChapterType type = parseChapterType(chNode.has("type") ? chNode.get("type").asText() : "CUSTOM");
            String title = chNode.has("title") ? chNode.get("title").asText() : "Chapter " + chOrder;
            Integer chapterNumber = chNode.has("chapterNumber") && !chNode.get("chapterNumber").isNull() ? chNode.get("chapterNumber").asInt() : null;
            ResearchReportChapter chapter = findChapter(existingChapters, type, title)
                    .orElseGet(() -> {
                        ResearchReportChapter created = new ResearchReportChapter();
                        created.setReport(report);
                        created.setType(type);
                        created.setTitle(title);
                        created.setChapterNumber(chapterNumber);
                        created.setDisplayOrder(chOrderValue(existingChapters));
                        ResearchReportChapter saved = chapterRepository.save(created);
                        existingChapters.add(saved);
                        return saved;
                    });
            if (chapter.getChapterNumber() == null && chapterNumber != null) chapter.setChapterNumber(chapterNumber);
            JsonNode sectionsNode = chNode.get("sections");
            if (sectionsNode != null && sectionsNode.isArray()) {
                ensureTemplateSections(chapter, sectionsNode, user);
            } else if (sectionRepository.findAllByChapterIdOrderByDisplayOrderAsc(chapter.getId()).isEmpty()) {
                createSeedSection(chapter, user);
            }
            chOrder++;
        }
    }

    private int chOrderValue(List<ResearchReportChapter> chapters) {
        return chapters.stream().mapToInt(ResearchReportChapter::getDisplayOrder).max().orElse(0) + 1;
    }

    private void ensureTemplateSections(ResearchReportChapter chapter, JsonNode sectionsNode, User user) {
        List<ResearchReportSection> existing = new ArrayList<>(sectionRepository.findAllByChapterIdOrderByDisplayOrderAsc(chapter.getId()));
        int order = 1;
        for (JsonNode secNode : sectionsNode) {
            ReportSectionType type = parseSectionType(secNode.has("type") ? secNode.get("type").asText() : "CUSTOM");
            String heading = secNode.has("heading") ? secNode.get("heading").asText() : "Section " + order;
            int displayOrder = order++;
            ResearchReportSection section = findSection(existing, type, heading)
                    .orElseGet(() -> {
                        ResearchReportSection created = new ResearchReportSection();
                        created.setChapter(chapter);
                        created.setType(type);
                        created.setHeading(heading);
                        created.setDisplayOrder(displayOrder);
                        created.setOrigin(ContentOrigin.USER);
                        created.setCreatedBy(user);
                        ResearchReportSection saved = sectionRepository.save(created);
                        existing.add(saved);
                        return saved;
                    });
            ensureRichSection(section);
        }
    }

    private void ensureDefaultChapters(ResearchReport report, User user) {
        if (chapterRepository.findAllByReportIdOrderByDisplayOrderAsc(report.getId()).isEmpty()) {
            createDefaultChapters(report, user);
        }
    }

    private Optional<ResearchReportChapter> findChapter(List<ResearchReportChapter> chapters, ReportChapterType type, String title) {
        return chapters.stream()
                .filter(chapter -> chapter.getType() == type || normalizeTitle(chapter.getTitle()).equals(normalizeTitle(title)))
                .findFirst();
    }

    private Optional<ResearchReportSection> findSection(List<ResearchReportSection> sections, ReportSectionType type, String heading) {
        return sections.stream()
                .filter(section -> (section.getType() == type && normalizeTitle(section.getHeading()).equals(normalizeTitle(heading)))
                        || normalizeTitle(section.getHeading()).equals(normalizeTitle(heading)))
                .findFirst();
    }

    private ReportChapterType parseChapterType(String value) {
        try {
            return ReportChapterType.valueOf(value);
        } catch (Exception ignored) {
            return ReportChapterType.CUSTOM;
        }
    }

    private ReportSectionType parseSectionType(String value) {
        try {
            return ReportSectionType.valueOf(value);
        } catch (Exception ignored) {
            return ReportSectionType.CUSTOM;
        }
    }

    private void ensureRichSection(ResearchReportSection section) {
        boolean changed = false;
        if (section.getContentJson() == null || section.getContentJson().isBlank() || !richTextService.isValidDocumentJson(section.getContentJson())) {
            section.setContentJson(richTextService.markdownToDocumentJson(section.getContent()));
            changed = true;
        }
        if (section.getPlainText() == null || section.getPlainText().isBlank()) {
            section.setPlainText(richTextService.plainTextFromDocumentJson(section.getContentJson()));
            changed = true;
        }
        if ((section.getContent() == null || section.getContent().isBlank()) && section.getContentJson() != null) {
            String markdown = richTextService.documentJsonToMarkdown(section.getContentJson());
            section.setContent(markdown.isBlank() ? null : markdown);
            changed = true;
        }
        ReportSectionStatus desired = section.getPlainText() == null || section.getPlainText().isBlank()
                ? ReportSectionStatus.NOT_STARTED
                : section.getStatus() == ReportSectionStatus.NOT_STARTED ? ReportSectionStatus.DRAFT : section.getStatus();
        if (section.getStatus() != desired) {
            section.setStatus(desired);
            changed = true;
        }
        if (changed) {
            sectionRepository.save(section);
        }
    }

    private boolean isBlankDoc(String contentJson) {
        if (contentJson == null || contentJson.isBlank()) return true;
        try {
            String plain = richTextService.plainTextFromDocumentJson(contentJson);
            return plain == null || plain.isBlank();
        } catch (Exception ignored) {
            return true;
        }
    }

    private void linkPersistedResearchEntities(ResearchReport report, User user) {
        UUID projectId = report.getProject().getId();
        for (ResearchReportSection section : sectionRepository.findAllByChapterReportId(report.getId())) {
            boolean isEmpty = (section.getContent() == null || section.getContent().isBlank())
                    && (section.getContentJson() == null || section.getContentJson().isBlank() || isBlankDoc(section.getContentJson()));
            if (!isEmpty) {
                continue;
            }
            switch (section.getType()) {
                case PROBLEM_STATEMENT -> {
                    problemRepository.findAllByProjectId(projectId).stream().findFirst().ifPresent(problem -> {
                        String text = problem.getStatement();
                        if (text != null && !text.isBlank()) {
                            section.setContent(text);
                            section.setContentJson(richTextService.markdownToDocumentJson(text));
                            section.setPlainText(richTextService.plainTextFromDocumentJson(section.getContentJson()));
                            section.setStatus(ReportSectionStatus.DRAFT);
                            sectionRepository.save(section);
                        }
                    });
                }
                case OBJECTIVES -> {
                    List<ResearchObjective> objectives = objectiveRepository.findAllByProjectId(projectId);
                    if (!objectives.isEmpty()) {
                        StringBuilder sb = new StringBuilder();
                        for (int i = 0; i < objectives.size(); i++) {
                            sb.append(i + 1).append(". ").append(objectives.get(i).getText()).append("\n");
                        }
                        String text = sb.toString().trim();
                        section.setContent(text);
                        section.setContentJson(richTextService.markdownToDocumentJson(text));
                        section.setPlainText(richTextService.plainTextFromDocumentJson(section.getContentJson()));
                        section.setStatus(ReportSectionStatus.DRAFT);
                        sectionRepository.save(section);
                    }
                }
                case METHODOLOGY -> {
                    methodologyRepository.findAllByProjectIdOrderByRevisionNumberDesc(projectId).stream().findFirst().ifPresent(m -> {
                        String desc = m.getDesignDescription() != null && !m.getDesignDescription().isBlank() ? m.getDesignDescription() : m.getRationale();
                        if (desc != null && !desc.isBlank()) {
                            section.setContent(desc);
                            section.setContentJson(richTextService.markdownToDocumentJson(desc));
                            section.setPlainText(richTextService.plainTextFromDocumentJson(section.getContentJson()));
                            section.setStatus(ReportSectionStatus.DRAFT);
                            sectionRepository.save(section);
                        }
                    });
                }
                case FINDINGS -> {
                    List<ResearchFinding> findings = findingRepository.findAllByProjectIdOrderByDisplayOrderAsc(projectId, Pageable.unpaged()).getContent();
                    if (!findings.isEmpty()) {
                        StringBuilder sb = new StringBuilder();
                        for (ResearchFinding f : findings) {
                            String detail = f.getFindingText() != null && !f.getFindingText().isBlank() ? f.getFindingText() : f.getStatement();
                            sb.append("### ").append(f.getTitle()).append("\n\n").append(detail != null ? detail : "").append("\n\n");
                        }
                        String text = sb.toString().trim();
                        section.setContent(text);
                        section.setContentJson(richTextService.markdownToDocumentJson(text));
                        section.setPlainText(richTextService.plainTextFromDocumentJson(section.getContentJson()));
                        section.setStatus(ReportSectionStatus.DRAFT);
                        sectionRepository.save(section);
                    }
                }
                case CONCLUSIONS -> {
                    List<ResearchConclusion> conclusions = conclusionRepository.findAllByProjectIdOrderByDisplayOrderAsc(projectId, Pageable.unpaged()).getContent();
                    if (!conclusions.isEmpty()) {
                        StringBuilder sb = new StringBuilder();
                        for (ResearchConclusion c : conclusions) {
                            String detail = c.getConclusionText() != null && !c.getConclusionText().isBlank() ? c.getConclusionText() : c.getStatement();
                            sb.append("### ").append(c.getTitle()).append("\n\n").append(detail != null ? detail : "").append("\n\n");
                        }
                        String text = sb.toString().trim();
                        section.setContent(text);
                        section.setContentJson(richTextService.markdownToDocumentJson(text));
                        section.setPlainText(richTextService.plainTextFromDocumentJson(section.getContentJson()));
                        section.setStatus(ReportSectionStatus.DRAFT);
                        sectionRepository.save(section);
                    }
                }
                case RECOMMENDATIONS -> {
                    List<ResearchRecommendation> recommendations = recommendationRepository.findAllByProjectIdOrderByDisplayOrderAsc(projectId, Pageable.unpaged()).getContent();
                    if (!recommendations.isEmpty()) {
                        StringBuilder sb = new StringBuilder();
                        for (ResearchRecommendation r : recommendations) {
                            String detail = r.getRecommendationText() != null && !r.getRecommendationText().isBlank() ? r.getRecommendationText() : r.getRecommendation();
                            sb.append("### ").append(r.getTitle()).append("\n\n").append(detail != null ? detail : "").append("\n\n");
                        }
                        String text = sb.toString().trim();
                        section.setContent(text);
                        section.setContentJson(richTextService.markdownToDocumentJson(text));
                        section.setPlainText(richTextService.plainTextFromDocumentJson(section.getContentJson()));
                        section.setStatus(ReportSectionStatus.DRAFT);
                        sectionRepository.save(section);
                    }
                }
                default -> {}
            }
        }
    }

    private void applySectionContent(ResearchReportSection section, String content, String contentJson, String plainText) {
        if (contentJson != null) {
            String normalizedJson = contentJson.isBlank() ? richTextService.markdownToDocumentJson("") : contentJson;
            if (!richTextService.isValidDocumentJson(normalizedJson)) {
                throw new IllegalArgumentException("Section rich-text JSON is invalid.");
            }
            section.setContentJson(normalizedJson);
            String resolvedPlainText = plainText != null ? optional(plainText) : richTextService.plainTextFromDocumentJson(normalizedJson);
            section.setPlainText(resolvedPlainText);
            String markdown = richTextService.documentJsonToMarkdown(normalizedJson);
            section.setContent(markdown.isBlank() ? resolvedPlainText : markdown);
            section.setManuallyEdited(true);
            section.setStatus(resolvedPlainText == null || resolvedPlainText.isBlank() ? ReportSectionStatus.NOT_STARTED : ReportSectionStatus.DRAFT);
            return;
        }
        if (content != null) {
            String normalized = optional(content);
            section.setContent(normalized);
            section.setContentJson(richTextService.markdownToDocumentJson(normalized));
            section.setPlainText(richTextService.plainTextFromDocumentJson(section.getContentJson()));
            section.setManuallyEdited(true);
            section.setStatus(section.getPlainText() == null || section.getPlainText().isBlank() ? ReportSectionStatus.NOT_STARTED : ReportSectionStatus.DRAFT);
        }
    }

    private void backfillProjectReferences(UUID projectId, User user) {
        for (com.researchassistant.document.entity.Document document : documentRepository.findAllByProjectIdAndStatusNot(projectId, DocumentStatus.ARCHIVED, Pageable.unpaged()).getContent()) {
            if (document.getCurrentVersion() != null && !document.getCurrentVersion().isQuarantined()) {
                referenceRegistryService.ensureForDocument(document, document.getCurrentVersion(), user);
            }
        }
    }

    private java.util.Set<UUID> citationEnabledDocumentIds(UUID projectId) {
        java.util.Set<UUID> ids = new java.util.LinkedHashSet<>();
        for (ProjectReference projectReference : projectReferenceRepository.findAllByProjectIdAndStatusOrderByCitationKeyAsc(projectId, ProjectReferenceStatus.ACTIVE)) {
            if (!projectReference.isAvailableForCitation()) {
                continue;
            }
            for (var link : referenceSourceLinkRepository.findAllByReferenceId(projectReference.getReference().getId())) {
                if (link.getDocument() != null) {
                    ids.add(link.getDocument().getId());
                }
            }
        }
        return ids;
    }

    private String normalizeTitle(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", " ").trim();
    }

    private void assembleFromTemplate(ResearchReport report, User user, String configurationJson) throws Exception {
        JsonNode root = objectMapper.readTree(configurationJson);
        JsonNode chaptersNode = root.get("chapters");
        if (chaptersNode == null || !chaptersNode.isArray()) {
            createDefaultChapters(report, user);
            return;
        }

        int chOrder = 1;
        for (JsonNode chNode : chaptersNode) {
            ResearchReportChapter chapter = new ResearchReportChapter();
            chapter.setReport(report);
            String typeStr = chNode.has("type") ? chNode.get("type").asText() : "CUSTOM";
            try {
                chapter.setType(ReportChapterType.valueOf(typeStr));
            } catch (Exception ignored) {
                chapter.setType(ReportChapterType.CUSTOM);
            }
            chapter.setTitle(chNode.has("title") ? chNode.get("title").asText() : "Chapter " + chOrder);
            if (chNode.has("chapterNumber") && !chNode.get("chapterNumber").isNull()) {
                chapter.setChapterNumber(chNode.get("chapterNumber").asInt());
            }
            chapter.setDisplayOrder(chOrder++);
            ResearchReportChapter savedChapter = chapterRepository.save(chapter);

            JsonNode sectionsNode = chNode.get("sections");
            if (sectionsNode != null && sectionsNode.isArray()) {
                int secOrder = 1;
                for (JsonNode secNode : sectionsNode) {
                    ResearchReportSection sec = new ResearchReportSection();
                    sec.setChapter(savedChapter);
                    String secTypeStr = secNode.has("type") ? secNode.get("type").asText() : "CUSTOM";
                    try {
                        sec.setType(ReportSectionType.valueOf(secTypeStr));
                    } catch (Exception ignored) {
                        sec.setType(ReportSectionType.CUSTOM);
                    }
                    sec.setHeading(secNode.has("heading") ? secNode.get("heading").asText() : "Section " + secOrder);
                    sec.setDisplayOrder(secOrder++);
                    sec.setOrigin(ContentOrigin.USER);
                    sec.setCreatedBy(user);
                    sectionRepository.save(sec);
                }
            } else {
                createSeedSection(savedChapter, user);
            }
        }
    }

    @Transactional(readOnly = true)
    public List<TemplateResponse> listTemplates(User user) {
        return templateRepository.findAll().stream().map(TemplateResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public SectionCapabilitiesResponse getSectionCapabilities(UUID projectId, User user) {
        authorizationService.requireProjectViewer(projectId, user);
        long datasetCount = datasetRepository.countByProjectId(projectId);
        long resultCount = resultRepository.countByProjectId(projectId);
        long findingCount = findingRepository.countByProjectId(projectId);

        boolean hasData = datasetCount > 0 || resultCount > 0;
        boolean hasFindings = findingCount > 0;

        List<SectionCapability> capabilities = new ArrayList<>();
        capabilities.add(new SectionCapability("LITERATURE_REVIEW", "READY", "Thematic synthesis, comparative analysis, and research gaps across sources", false, false));
        capabilities.add(new SectionCapability("PROBLEM_STATEMENT", "READY", "Formulate empirical gap, context, and problem magnitude", false, false));
        capabilities.add(new SectionCapability("BACKGROUND", "READY", "Broader scholarly and contextual foundation", false, false));
        capabilities.add(new SectionCapability("RESEARCH_GAP", "READY", "Synthesized omissions and methodological limitations", false, false));
        capabilities.add(new SectionCapability("OBJECTIVES", "READY", "Hierarchical research aims derived from topic and problem", false, false));
        capabilities.add(new SectionCapability("RESEARCH_QUESTIONS", "READY", "Empirically answerable research questions aligned with aims", false, false));
        capabilities.add(new SectionCapability("HYPOTHESES", "READY", "Directional or null hypotheses grounded in theoretical literature", false, false));
        capabilities.add(new SectionCapability("CONCEPTUAL_FRAMEWORK", "READY", "Key constructs, variables, and hypothesized interrelationships", false, false));
        capabilities.add(new SectionCapability("THEORETICAL_FRAMEWORK", "READY", "Grounding theoretical paradigms and explanatory models", false, false));
        capabilities.add(new SectionCapability("METHODOLOGY", "READY", "Research design, population, and sampling strategy proposal", false, false));
        capabilities.add(new SectionCapability("POPULATION_SAMPLING", "READY", "Sampling frame, sample size determination, and selection criteria", false, false));
        capabilities.add(new SectionCapability("DATA_COLLECTION_METHOD", "READY", "Procedures for empirical data collection", false, false));
        capabilities.add(new SectionCapability("RESEARCH_INSTRUMENT", "READY", "Questionnaire or interview guide draft based on literature", false, false));
        capabilities.add(new SectionCapability("FINDINGS", hasData ? "READY" : "DATA_REQUIRED", "Empirical results synthesized from actual research data", true, false));
        capabilities.add(new SectionCapability("DISCUSSION", hasFindings ? "READY" : "FINDINGS_REQUIRED", "Interpretation of empirical findings contextualized against literature", true, true));
        capabilities.add(new SectionCapability("CONCLUSION", hasFindings ? "READY" : "FINDINGS_REQUIRED", "Synthesized conclusions directly addressing research objectives", true, true));
        capabilities.add(new SectionCapability("RECOMMENDATIONS", hasFindings ? "READY" : "FINDINGS_REQUIRED", "Actionable practical, policy, and future research recommendations", true, true));
        capabilities.add(new SectionCapability("ABSTRACT", "READY", "Synthesis of background, problem, aim, methodology, and conclusions", false, false));
        capabilities.add(new SectionCapability("CUSTOM", "READY", "Custom researcher-defined section", false, false));

        return new SectionCapabilitiesResponse(projectId, capabilities);
    }

    @Transactional(readOnly = true)
    public TableOfContentsResponse generateTableOfContents(UUID reportId, User user) {
        ResearchReport report = loadReport(reportId);
        authorizationService.requireProjectViewer(report.getProject().getId(), user);
        List<ResearchReportChapter> chapters = chapterRepository.findAllByReportIdOrderByDisplayOrderAsc(reportId);

        StringBuilder md = new StringBuilder();
        md.append("# TABLE OF CONTENTS\n\n");

        List<TableOfContentsItem> chapterItems = new ArrayList<>();
        for (ResearchReportChapter ch : chapters) {
            List<ResearchReportSection> sections = sectionRepository.findAllByChapterIdOrderByDisplayOrderAsc(ch.getId());
            List<TableOfContentsSectionItem> sectionItems = new ArrayList<>();

            md.append("### ").append(ch.getTitle()).append("\n");
            int secIndex = 1;
            for (ResearchReportSection sec : sections) {
                sectionItems.add(new TableOfContentsSectionItem(sec.getHeading(), sec.getDisplayOrder(), sec.getType()));
                String prefix = ch.getChapterNumber() != null ? ch.getChapterNumber() + "." + secIndex : "-";
                md.append(prefix).append(" ").append(sec.getHeading()).append("\n");
                secIndex++;
            }
            md.append("\n");
            chapterItems.add(new TableOfContentsItem(ch.getTitle(), ch.getChapterNumber(), ch.getDisplayOrder(), sectionItems));
        }

        return new TableOfContentsResponse(reportId, report.getTitle(), chapterItems, md.toString());
    }

    @Transactional(readOnly = true)
    public ReportValidationResponse validateReport(UUID reportId, User user) {
        ResearchReport report = loadReport(reportId);
        authorizationService.requireProjectViewer(report.getProject().getId(), user);
        List<ValidationIssue> errors = new ArrayList<>();
        List<ValidationIssue> warnings = new ArrayList<>();
        List<ValidationIssue> info = new ArrayList<>();
        UUID projectId = report.getProject().getId();
        if (problemRepository.findAllByProjectId(projectId).isEmpty()) errors.add(new ValidationIssue("ERROR", "MISSING_RESEARCH_PROBLEM", "Research problem is missing.", projectId));
        if (objectiveRepository.findAllByProjectId(projectId).isEmpty()) errors.add(new ValidationIssue("ERROR", "MISSING_OBJECTIVES", "Research objectives are missing.", projectId));
        if (methodologyRepository.findAllByProjectIdOrderByRevisionNumberDesc(projectId).isEmpty()) warnings.add(new ValidationIssue("WARNING", "MISSING_METHODOLOGY", "Methodology has not been created.", projectId));
        if (resultRepository.countByProjectId(projectId) == 0) warnings.add(new ValidationIssue("WARNING", "NO_ANALYSIS_RESULTS", "No analysis results are available.", projectId));
        if (findingRepository.countByProjectId(projectId) == 0) warnings.add(new ValidationIssue("WARNING", "NO_FINDINGS", "No findings have been created from analysis results.", projectId));
        if (conclusionRepository.countByProjectId(projectId) == 0) warnings.add(new ValidationIssue("WARNING", "NO_CONCLUSIONS", "No conclusions are available.", projectId));
        if (recommendationRepository.countByProjectId(projectId) == 0) info.add(new ValidationIssue("INFO", "NO_RECOMMENDATIONS", "No recommendations are available yet.", projectId));
        for (ResearchReportSection section : sectionRepository.findAllByChapterReportId(reportId)) {
            if (section.isSourceOutOfDate()) warnings.add(new ValidationIssue("WARNING", "STALE_SECTION", "A report section is stale because its source artifact changed.", section.getId()));
            if (section.getContent() != null && section.getContent().contains("[citation metadata incomplete]")) {
                errors.add(new ValidationIssue("ERROR", "CITATION_PLACEHOLDER_IN_SECTION", "A report section contains an internal citation metadata placeholder.", section.getId()));
            }
            if (section.getContent() != null && markdownRenderer.containsRawMarkdownHeading(section.getContent())) {
                info.add(new ValidationIssue("INFO", "MARKDOWN_WILL_RENDER_AS_WORD_STRUCTURE", "A report section contains Markdown headings that will be rendered as Word heading styles during export.", section.getId()));
            }
            if ((section.getContent() == null || section.getContent().isBlank())
                    && (section.getContentJson() == null || isBlankDoc(section.getContentJson()))) {
                if (section.getType() == ReportSectionType.CONCLUSIONS) {
                    errors.add(new ValidationIssue("ERROR", "MISSING_REQUIRED_SECTION", "Conclusion is required before finalization.", section.getId()));
                } else if (section.getType() == ReportSectionType.LITERATURE_REVIEW) {
                    errors.add(new ValidationIssue("ERROR", "MISSING_REQUIRED_SECTION", "Literature Review is required before finalization.", section.getId()));
                } else {
                    warnings.add(new ValidationIssue("WARNING", "SECTION_INCOMPLETE", "Section " + section.getHeading() + " has not been completed.", section.getId()));
                }
            }
        }
        Map<UUID, ReferenceEntry> references = citedReferencesForValidation(reportId);
        for (ReferenceEntry reference : references.values()) {
            if (reference == null
                    || reference.getMetadataStatus() == ReferenceMetadataStatus.INCOMPLETE
                    || reference.getTitle() == null
                    || reference.getTitle().isBlank()
                    || "REFERENCE_METADATA_INCOMPLETE".equalsIgnoreCase(reference.getTitle())) {
                errors.add(new ValidationIssue("ERROR", "INCOMPLETE_CITED_REFERENCE_METADATA", "A cited reference has incomplete bibliographic metadata and must be reviewed before final export.", reference == null ? reportId : reference.getId()));
            }
        }
        auditService.record(user.getId(), SecurityAuditEventType.REPORT_VALIDATED);
        return new ReportValidationResponse(errors, warnings, info);
    }

    @Transactional
    public ReportResponse finalizeReport(UUID reportId, User user) {
        ResearchReport report = loadReport(reportId);
        authorizationService.requireProjectAdminAccess(report.getProject().getId(), user);
        ReportValidationResponse validation = validateReport(reportId, user);
        if (!validation.errors().isEmpty()) throw new ReportValidationException(validation);
        report.setStatus(ResearchReportStatus.FINAL);
        report.setRevisionNumber(report.getRevisionNumber() + 1);
        auditService.record(user.getId(), SecurityAuditEventType.REPORT_FINALIZED);
        return ReportResponse.from(report);
    }

    @Transactional
    public FinalDocumentResponse prepareFinalDocument(UUID reportId, User user) {
        ResearchReport report = loadReportForEdit(reportId, user);
        ensureReportStructure(report, user);

        List<ResearchReportChapter> chapters = chapterRepository.findAllByReportIdOrderByDisplayOrderAsc(reportId);
        List<ReportRichTextService.DocumentPart> parts = new ArrayList<>();
        Map<String, Object> sectionRevisions = new LinkedHashMap<>();

        // 1. Preliminary (e.g. Abstract)
        for (ResearchReportChapter chapter : chapters) {
            if (chapter.getType() == ReportChapterType.PRELIMINARY) {
                List<ResearchReportSection> sections = sectionRepository.findAllByChapterIdOrderByDisplayOrderAsc(chapter.getId());
                for (ResearchReportSection section : sections) {
                    ensureRichSection(section);
                    sectionRevisions.put(section.getId().toString(), section.getRevisionNumber());
                    parts.add(new ReportRichTextService.DocumentPart(section.getHeading(), 1, section.getContentJson(), section.getPlainText()));
                }
            }
        }

        // 2. Table of Contents
        TableOfContentsResponse toc = generateTableOfContents(reportId, user);
        parts.add(new ReportRichTextService.DocumentPart("Table of Contents", 1, richTextService.markdownToDocumentJson(toc.formattedMarkdown()), toc.formattedMarkdown()));

        // 3. Body Chapters & Hierarchical Sections
        for (ResearchReportChapter chapter : chapters) {
            if (chapter.getType() == ReportChapterType.PRELIMINARY || chapter.getType() == ReportChapterType.REFERENCES || chapter.getType() == ReportChapterType.APPENDICES) {
                continue;
            }
            parts.add(new ReportRichTextService.DocumentPart(chapter.getTitle(), 1, null, null));
            List<ResearchReportSection> rootSections = sectionRepository.findAllByChapterIdAndParentSectionIsNullOrderByDisplayOrderAsc(chapter.getId());
            for (ResearchReportSection section : rootSections) {
                ensureRichSection(section);
                sectionRevisions.put(section.getId().toString(), section.getRevisionNumber());
                String heading = section.getSectionNumber() != null ? section.getSectionNumber() + " " + section.getHeading() : section.getHeading();
                parts.add(new ReportRichTextService.DocumentPart(heading, 2, section.getContentJson(), section.getPlainText()));

                List<ResearchReportSection> subsections = sectionRepository.findAllByParentSectionIdOrderByDisplayOrderAsc(section.getId());
                for (ResearchReportSection sub : subsections) {
                    ensureRichSection(sub);
                    sectionRevisions.put(sub.getId().toString(), sub.getRevisionNumber());
                    String subHeading = sub.getSectionNumber() != null ? sub.getSectionNumber() + " " + sub.getHeading() : sub.getHeading();
                    parts.add(new ReportRichTextService.DocumentPart(subHeading, 3, sub.getContentJson(), sub.getPlainText()));

                    List<ResearchReportSection> subSubs = sectionRepository.findAllByParentSectionIdOrderByDisplayOrderAsc(sub.getId());
                    for (ResearchReportSection subSub : subSubs) {
                        ensureRichSection(subSub);
                        sectionRevisions.put(subSub.getId().toString(), subSub.getRevisionNumber());
                        String subSubHeading = subSub.getSectionNumber() != null ? subSub.getSectionNumber() + " " + subSub.getHeading() : subSub.getHeading();
                        parts.add(new ReportRichTextService.DocumentPart(subSubHeading, 4, subSub.getContentJson(), subSub.getPlainText()));
                    }
                }
            }

            if (chapter.getType() == ReportChapterType.LITERATURE_REVIEW && "CHAPTER_TWO".equalsIgnoreCase(report.getLiteratureMatrixInclusion())) {
                literatureMatrixRepository.findFirstByProjectIdOrderByCreatedAtDesc(report.getProject().getId())
                        .ifPresent(matrix -> {
                            if (matrix.getMarkdownTable() != null && !matrix.getMarkdownTable().isBlank()) {
                                parts.add(new ReportRichTextService.DocumentPart("Literature Evidence Assessment Matrix", 2, richTextService.markdownToDocumentJson(matrix.getMarkdownTable()), matrix.getMarkdownTable()));
                            }
                        });
            }
        }

        // 4. References Chapter
        refreshReportReferencesInternal(report, user);
        for (ResearchReportChapter chapter : chapters) {
            if (chapter.getType() == ReportChapterType.REFERENCES) {
                parts.add(new ReportRichTextService.DocumentPart(chapter.getTitle(), 1, null, null));
                List<ResearchReportSection> sections = sectionRepository.findAllByChapterIdOrderByDisplayOrderAsc(chapter.getId());
                for (ResearchReportSection section : sections) {
                    ensureRichSection(section);
                    sectionRevisions.put(section.getId().toString(), section.getRevisionNumber());
                    parts.add(new ReportRichTextService.DocumentPart(section.getHeading(), 2, section.getContentJson(), section.getPlainText()));
                }
            }
        }

        // 5. Appendices (including Literature Matrix if configured)
        for (ResearchReportChapter chapter : chapters) {
            if (chapter.getType() == ReportChapterType.APPENDICES) {
                parts.add(new ReportRichTextService.DocumentPart(chapter.getTitle(), 1, null, null));
                if ("APPENDIX".equalsIgnoreCase(report.getLiteratureMatrixInclusion())) {
                    literatureMatrixRepository.findFirstByProjectIdOrderByCreatedAtDesc(report.getProject().getId())
                            .ifPresent(matrix -> {
                                if (matrix.getMarkdownTable() != null && !matrix.getMarkdownTable().isBlank()) {
                                    parts.add(new ReportRichTextService.DocumentPart("Appendix: Literature Evidence Assessment Matrix", 2, richTextService.markdownToDocumentJson(matrix.getMarkdownTable()), matrix.getMarkdownTable()));
                                }
                            });
                }
                List<ResearchReportSection> sections = sectionRepository.findAllByChapterIdOrderByDisplayOrderAsc(chapter.getId());
                for (ResearchReportSection section : sections) {
                    ensureRichSection(section);
                    sectionRevisions.put(section.getId().toString(), section.getRevisionNumber());
                    parts.add(new ReportRichTextService.DocumentPart(section.getHeading(), 2, section.getContentJson(), section.getPlainText()));
                }
            }
        }

        String assembledJson = richTextService.assembleDocumentJson(parts);
        String plainText = richTextService.plainTextFromDocumentJson(assembledJson);

        int maxVersion = documentVersionRepository.maxVersionNumber(reportId);
        int nextVersion = maxVersion + 1;

        ReportDocumentVersion docVersion = new ReportDocumentVersion();
        docVersion.setReport(report);
        docVersion.setProject(report.getProject());
        docVersion.setVersionNumber(nextVersion);
        docVersion.setTitle(report.getTitle() + " - Final Draft v" + nextVersion);
        docVersion.setStatus(ReportDocumentVersionStatus.FINAL_REVIEW);
        docVersion.setCitationStyle(report.getCitationStyle());
        docVersion.setContentJson(assembledJson);
        docVersion.setPlainText(plainText);
        docVersion.setSourceReportRevisionNumber(report.getRevisionNumber());
        try {
            docVersion.setSectionRevisionSnapshotJson(objectMapper.writeValueAsString(sectionRevisions));
            docVersion.setTemplateSnapshotJson(report.getTemplate() != null ? report.getTemplate().getConfigurationJson() : "{}");
        } catch (Exception ignored) {}
        docVersion.setCreatedBy(user);
        docVersion.setUpdatedBy(user);

        ReportDocumentVersion saved = documentVersionRepository.save(docVersion);
        auditService.record(user.getId(), SecurityAuditEventType.REPORT_FINALIZED);
        return FinalDocumentResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public FinalDocumentResponse getFinalDocument(UUID reportId, User user) {
        ResearchReport report = loadReport(reportId);
        authorizationService.requireProjectViewer(report.getProject().getId(), user);
        return documentVersionRepository.findFirstByReportIdOrderByVersionNumberDesc(reportId)
                .map(FinalDocumentResponse::from)
                .orElse(null);
    }

    @Transactional
    public FinalDocumentResponse updateFinalDocument(UUID reportId, User user, UpdateFinalDocumentRequest request) {
        ResearchReport report = loadReportForEdit(reportId, user);
        ReportDocumentVersion version = documentVersionRepository.findFirstByReportIdOrderByVersionNumberDesc(reportId)
                .orElseThrow(() -> new ResourceNotFoundException("No final document version found for this report. Please prepare one first."));
        if (request.contentJson() != null && !request.contentJson().isBlank()) {
            version.setContentJson(request.contentJson());
            version.setPlainText(request.plainText() != null ? request.plainText() : richTextService.plainTextFromDocumentJson(request.contentJson()));
        }
        if (request.status() != null) {
            version.setStatus(request.status());
        }
        version.setUpdatedBy(user);
        return FinalDocumentResponse.from(version);
    }

    @Transactional
    public List<ChapterResponse> listChapters(UUID reportId, User user) {
        ResearchReport report = loadReport(reportId);
        authorizationService.requireProjectViewer(report.getProject().getId(), user);
        ensureReportStructure(report, user);
        return chapterRepository.findAllByReportIdOrderByDisplayOrderAsc(reportId).stream().map(ChapterResponse::from).toList();
    }

    @Transactional
    public ChapterResponse createChapter(UUID reportId, User user, CreateChapterRequest request) {
        ResearchReport report = loadReportForEdit(reportId, user);
        ResearchReportChapter chapter = new ResearchReportChapter();
        chapter.setReport(report);
        chapter.setType(request.type());
        chapter.setTitle(required(request.title(), "Chapter title is required."));
        chapter.setChapterNumber(request.chapterNumber());
        chapter.setDisplayOrder(request.displayOrder() == null ? 1 : request.displayOrder());
        chapter.setRequired(request.required() != null && request.required());
        chapter.setSystemDefined(request.systemDefined() != null && request.systemDefined());
        ResearchReportChapter saved = chapterRepository.save(chapter);
        recalculateSectionNumbers(report);
        return ChapterResponse.from(saved);
    }

    @Transactional
    public ChapterResponse updateChapter(UUID chapterId, User user, UpdateChapterRequest request) {
        ResearchReportChapter chapter = chapterRepository.findById(chapterId).orElseThrow(() -> new ResourceNotFoundException("Report chapter not found."));
        ResearchReport report = loadReportForEdit(chapter.getReport().getId(), user);
        if (request.type() != null) chapter.setType(request.type());
        if (request.title() != null) chapter.setTitle(optional(request.title()));
        if (request.chapterNumber() != null) chapter.setChapterNumber(request.chapterNumber());
        if (request.displayOrder() != null) chapter.setDisplayOrder(request.displayOrder());
        if (request.required() != null) chapter.setRequired(request.required());
        if (request.systemDefined() != null) chapter.setSystemDefined(request.systemDefined());
        chapterRepository.save(chapter);
        recalculateSectionNumbers(report);
        return ChapterResponse.from(chapter);
    }

    @Transactional
    public void deleteChapter(UUID chapterId, User user) {
        ResearchReportChapter chapter = chapterRepository.findById(chapterId).orElseThrow(() -> new ResourceNotFoundException("Report chapter not found."));
        ResearchReport report = loadReportForEdit(chapter.getReport().getId(), user);
        if (chapter.isRequired()) {
            throw new IllegalArgumentException("Cannot delete required template chapter: " + chapter.getTitle());
        }
        List<ResearchReportSection> sections = sectionRepository.findAllByChapterIdOrderByDisplayOrderAsc(chapterId);
        for (ResearchReportSection sec : sections) {
            deleteSectionHierarchy(sec);
        }
        chapterRepository.delete(chapter);
        recalculateSectionNumbers(report);
    }

    @Transactional
    public List<SectionResponse> listSections(UUID chapterId, User user) {
        ResearchReportChapter chapter = chapterRepository.findById(chapterId).orElseThrow(() -> new ResourceNotFoundException("Report chapter not found."));
        authorizationService.requireProjectViewer(chapter.getReport().getProject().getId(), user);
        return sectionRepository.findAllByChapterIdOrderByDisplayOrderAsc(chapterId).stream()
                .peek(this::ensureRichSection)
                .map(SectionResponse::from)
                .toList();
    }

    @Transactional
    public SectionResponse createSection(UUID chapterId, User user, CreateSectionRequest request) {
        ResearchReportChapter chapter = chapterRepository.findById(chapterId).orElseThrow(() -> new ResourceNotFoundException("Report chapter not found."));
        ResearchReport report = loadReportForEdit(chapter.getReport().getId(), user);
        ResearchReportSection section = new ResearchReportSection();
        section.setChapter(chapter);
        if (request.parentSectionId() != null) {
            ResearchReportSection parent = sectionRepository.findById(request.parentSectionId())
                    .orElseThrow(() -> new ResourceNotFoundException("Parent section not found."));
            section.setParentSection(parent);
        }
        section.setType(request.type());
        section.setHeading(required(request.heading(), "Section heading is required."));
        applySectionContent(section, request.content(), request.contentJson(), request.plainText());
        if (request.status() != null) section.setStatus(request.status());
        section.setDisplayOrder(request.displayOrder() == null ? 1 : request.displayOrder());
        section.setRequired(request.required() != null && request.required());
        section.setSystemDefined(request.systemDefined() != null && request.systemDefined());
        section.setAiEnabled(request.aiEnabled() == null || request.aiEnabled());
        section.setOrigin(defaultOrigin(request.origin()));
        section.setSourceArtifactType(optional(request.sourceArtifactType()));
        section.setSourceArtifactId(request.sourceArtifactId());
        section.setSourceRevisionNumber(resolveSourceRevision(section.getSourceArtifactType(), section.getSourceArtifactId()));
        section.setCreatedBy(user);
        if (section.getContentJson() == null) {
            section.setContentJson(richTextService.markdownToDocumentJson(section.getContent()));
            section.setPlainText(richTextService.plainTextFromDocumentJson(section.getContentJson()));
        }
        ResearchReportSection saved = sectionRepository.save(section);
        recalculateSectionNumbers(report);
        return SectionResponse.from(saved);
    }

    @Transactional
    public SectionResponse updateSection(UUID sectionId, User user, UpdateSectionRequest request) {
        ResearchReportSection section = sectionRepository.findById(sectionId).orElseThrow(() -> new ResourceNotFoundException("Report section not found."));
        ResearchReport report = loadReportForEdit(section.getChapter().getReport().getId(), user);
        if (request.parentSectionId() != null) {
            ResearchReportSection parent = sectionRepository.findById(request.parentSectionId())
                    .orElseThrow(() -> new ResourceNotFoundException("Parent section not found."));
            section.setParentSection(parent);
        }
        if (request.type() != null) section.setType(request.type());
        if (request.heading() != null) section.setHeading(optional(request.heading()));
        applySectionContent(section, request.content(), request.contentJson(), request.plainText());
        if (request.status() != null) section.setStatus(request.status());
        if (request.displayOrder() != null) section.setDisplayOrder(request.displayOrder());
        if (request.required() != null) section.setRequired(request.required());
        if (request.systemDefined() != null) section.setSystemDefined(request.systemDefined());
        if (request.aiEnabled() != null) section.setAiEnabled(request.aiEnabled());
        if (request.origin() != null) section.setOrigin(request.origin());
        if (request.sourceOutOfDate() != null) section.setSourceOutOfDate(request.sourceOutOfDate());
        section.setUpdatedBy(user);
        section.setRevisionNumber(section.getRevisionNumber() + 1);
        auditService.record(user.getId(), SecurityAuditEventType.REPORT_SECTION_UPDATED);
        recalculateSectionNumbers(report);
        return SectionResponse.from(section);
    }

    @Transactional
    public void deleteSection(UUID sectionId, User user) {
        ResearchReportSection section = sectionRepository.findById(sectionId).orElseThrow(() -> new ResourceNotFoundException("Report section not found."));
        ResearchReport report = loadReportForEdit(section.getChapter().getReport().getId(), user);
        if (section.isRequired()) {
            throw new IllegalArgumentException("Cannot delete required template section: " + section.getHeading());
        }
        deleteSectionHierarchy(section);
        recalculateSectionNumbers(report);
    }

    private void deleteSectionHierarchy(ResearchReportSection section) {
        List<ResearchReportSection> children = sectionRepository.findAllByParentSectionIdOrderByDisplayOrderAsc(section.getId());
        for (ResearchReportSection child : children) {
            deleteSectionHierarchy(child);
        }
        citationRepository.deleteAllBySectionId(section.getId());
        sectionRepository.delete(section);
    }

    @Transactional
    public ReportStructureResponse getReportStructure(UUID reportId, User user) {
        ResearchReport report = loadReport(reportId);
        authorizationService.requireProjectViewer(report.getProject().getId(), user);
        ensureReportStructure(report, user);

        List<ResearchReportChapter> chapters = chapterRepository.findAllByReportIdOrderByDisplayOrderAsc(reportId);
        List<ChapterStructureResponse> chapterResponses = new ArrayList<>();

        for (ResearchReportChapter chapter : chapters) {
            List<ResearchReportSection> allSections = sectionRepository.findAllByChapterIdOrderByDisplayOrderAsc(chapter.getId());
            Map<UUID, List<ResearchReportSection>> subsectionsByParent = allSections.stream()
                    .filter(s -> s.getParentSection() != null)
                    .collect(java.util.stream.Collectors.groupingBy(s -> s.getParentSection().getId()));

            List<SectionStructureResponse> rootResponses = allSections.stream()
                    .filter(s -> s.getParentSection() == null)
                    .map(sec -> mapSectionStructure(sec, subsectionsByParent))
                    .toList();

            chapterResponses.add(new ChapterStructureResponse(
                    chapter.getId(), reportId, chapter.getType(), chapter.getTitle(),
                    chapter.getChapterNumber(), chapter.getDisplayOrder(),
                    chapter.isRequired(), chapter.isSystemDefined(),
                    rootResponses
            ));
        }

        return new ReportStructureResponse(
                report.getId(), report.getProject().getId(), report.getTitle(), report.getType(),
                report.getCitationStyle(), report.isIncludeUncitedReferences(),
                report.getLiteratureMatrixInclusion(),
                chapterResponses
        );
    }

    private SectionStructureResponse mapSectionStructure(ResearchReportSection sec, Map<UUID, List<ResearchReportSection>> subsectionsByParent) {
        List<ResearchReportSection> children = subsectionsByParent.getOrDefault(sec.getId(), List.of());
        List<SectionStructureResponse> childResponses = children.stream()
                .map(c -> mapSectionStructure(c, subsectionsByParent))
                .toList();

        return new SectionStructureResponse(
                sec.getId(), sec.getChapter().getId(),
                sec.getParentSection() != null ? sec.getParentSection().getId() : null,
                sec.getSectionNumber(), sec.getType(), sec.getHeading(), sec.getStatus(),
                sec.getDisplayOrder(), sec.isRequired(), sec.isSystemDefined(), sec.isAiEnabled(),
                sec.isManuallyEdited(), sec.getRevisionNumber(), childResponses
        );
    }

    @Transactional
    public ReportStructureResponse reorderStructure(UUID reportId, ReorderStructureRequest request, User user) {
        ResearchReport report = loadReportForEdit(reportId, user);
        if (request.chapters() != null) {
            for (ReorderItem item : request.chapters()) {
                chapterRepository.findById(item.id()).ifPresent(ch -> {
                    if (ch.getReport().getId().equals(reportId)) {
                        ch.setDisplayOrder(item.displayOrder());
                    }
                });
            }
        }
        if (request.sections() != null) {
            for (ReorderItem item : request.sections()) {
                sectionRepository.findById(item.id()).ifPresent(sec -> {
                    if (sec.getChapter().getReport().getId().equals(reportId)) {
                        sec.setDisplayOrder(item.displayOrder());
                        if (item.parentId() != null) {
                            sectionRepository.findById(item.parentId()).ifPresent(sec::setParentSection);
                        } else {
                            sec.setParentSection(null);
                        }
                    }
                });
            }
        }
        chapterRepository.flush();
        sectionRepository.flush();
        recalculateSectionNumbers(report);
        return getReportStructure(reportId, user);
    }

    @Transactional
    public ReportResponse updateReportSettings(UUID reportId, User user, UpdateReportSettingsRequest request) {
        ResearchReport report = loadReportForEdit(reportId, user);
        boolean refreshReferences = false;
        if (request.includeUncitedReferences() != null && request.includeUncitedReferences() != report.isIncludeUncitedReferences()) {
            report.setIncludeUncitedReferences(request.includeUncitedReferences());
            refreshReferences = true;
        }
        if (request.literatureMatrixInclusion() != null) {
            report.setLiteratureMatrixInclusion(request.literatureMatrixInclusion());
        }
        if (request.citationStyle() != null && request.citationStyle() != report.getCitationStyle()) {
            report.setCitationStyle(request.citationStyle());
            refreshReferences = true;
        }
        reportRepository.save(report);
        if (refreshReferences) {
            refreshReportReferencesInternal(report, user);
        }
        return ReportResponse.from(report);
    }

    @Transactional(readOnly = true)
    public LiteratureMatrixResponse getLiteratureMatrix(UUID projectId, User user) {
        authorizationService.requireProjectViewer(projectId, user);
        LiteratureMatrix matrix = literatureMatrixRepository.findFirstByProjectIdOrderByCreatedAtDesc(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Literature matrix not found for this project."));
        return LiteratureMatrixResponse.from(matrix);
    }

    @Transactional
    public LiteratureMatrixResponse saveLiteratureMatrix(UUID projectId, User user, SaveLiteratureMatrixRequest request) {
        ResearchProject project = authorizationService.requireProjectEditor(projectId, user).project();
        LiteratureMatrix matrix = literatureMatrixRepository.findFirstByProjectIdOrderByCreatedAtDesc(projectId)
                .orElseGet(() -> {
                    LiteratureMatrix created = new LiteratureMatrix();
                    created.setProject(project);
                    created.setCreatedBy(user);
                    return created;
                });
        if (request.title() != null) matrix.setTitle(request.title());
        if (request.markdownTable() != null) matrix.setMarkdownTable(request.markdownTable());
        if (request.matrixDataJson() != null) matrix.setMatrixDataJson(request.matrixDataJson());
        matrix.setOrigin(ContentOrigin.USER);
        return LiteratureMatrixResponse.from(literatureMatrixRepository.save(matrix));
    }

    @Transactional
    public SectionResponse refreshReportReferences(UUID reportId, User user) {
        ResearchReport report = loadReportForEdit(reportId, user);
        return SectionResponse.from(refreshReportReferencesInternal(report, user));
    }

    public GeneratedDraftResponse generateSection(UUID sectionId, User user, GenerateSectionRequest request) {
        ResearchReportSection section = sectionRepository.findWithChapterReportProjectById(sectionId).orElseThrow(() -> new ResourceNotFoundException("Report section not found."));
        UUID projectId = section.getChapter().getReport().getProject().getId();
        authorizationService.requireProjectEditor(projectId, user);

        if (section.getType() == ReportSectionType.REFERENCES) {
            ResearchReportSection refreshed = refreshReportReferencesInternal(section.getChapter().getReport(), user);
            return new GeneratedDraftResponse(
                    refreshed.getContent(),
                    List.of(),
                    "DETERMINISTIC_BIBLIOGRAPHY",
                    List.of(),
                    null,
                    refreshed.getUpdatedAt()
            );
        }

        String prompt = buildSectionGenerationPrompt(section, request);
        String retrievalQuery = buildSectionRetrievalQuery(section, request);
        java.util.Set<UUID> documentIds = request == null || request.documentIds() == null
                ? java.util.Set.of()
                : new java.util.LinkedHashSet<>(request.documentIds());
        if (documentIds.isEmpty()) {
            documentIds = citationEnabledDocumentIds(projectId);
        }
        SubmitRagQueryRequest ragRequest = new SubmitRagQueryRequest(
                prompt,
                documentIds.isEmpty() ? RetrievalScopeType.PROJECT_ALL_DOCUMENTS : RetrievalScopeType.SELECTED_DOCUMENTS,
                documentIds.isEmpty() ? null : documentIds,
                request == null || request.evidenceLimit() == null ? 12 : request.evidenceLimit(),
                retrievalQuery
        );
        GroundedAnswerResponse answer = section.getType() == ReportSectionType.LITERATURE_REVIEW
                ? ragQueryService.submitLiteratureReviewForProject(
                        projectId,
                        user,
                        ragRequest,
                        "Generate " + section.getHeading()
                )
                : ragQueryService.submitForProject(
                        projectId,
                        user,
                        ragRequest,
                        "Generate " + section.getHeading()
                );
        return persistGeneratedSection(sectionId, answer, user);
    }

    @Transactional(readOnly = true)
    public List<CitationResponse> listCitations(UUID sectionId, User user) {
        ResearchReportSection section = sectionRepository.findById(sectionId).orElseThrow(() -> new ResourceNotFoundException("Report section not found."));
        authorizationService.requireProjectViewer(section.getChapter().getReport().getProject().getId(), user);
        return citationRepository.findAllBySectionIdOrderByCitationOrdinalAsc(sectionId).stream().map(CitationResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public TraceabilityMatrixResponse traceabilityMatrix(UUID projectId, User user) {
        authorizationService.requireProjectViewer(projectId, user);
        List<AnalysisRun> runs = runRepository.findAllByProjectId(projectId);
        List<ResearchFinding> findings = findingRepository.findAllByProjectIdOrderByDisplayOrderAsc(projectId, Pageable.unpaged()).getContent();
        List<ResearchConclusion> conclusions = conclusionRepository.findAllByProjectIdOrderByDisplayOrderAsc(projectId, Pageable.unpaged()).getContent();
        List<ResearchRecommendation> recommendations = recommendationRepository.findAllByProjectIdOrderByDisplayOrderAsc(projectId, Pageable.unpaged()).getContent();
        List<TraceabilityRow> rows = new ArrayList<>();
        for (ResearchObjective objective : objectiveRepository.findAllByProjectId(projectId)) {
            List<ResearchQuestion> questions = questionRepository.findAllByProjectId(projectId).stream().filter(q -> q.getObjective() != null && q.getObjective().getId().equals(objective.getId())).toList();
            List<String> warnings = new ArrayList<>();
            if (questions.isEmpty()) warnings.add("Objective exists with no research question.");
            long analysisCount = runs.stream().filter(r -> r.getObjective() != null && r.getObjective().getId().equals(objective.getId())).count();
            long findingCount = findings.stream().filter(f -> f.getObjective() != null && f.getObjective().getId().equals(objective.getId())).count();
            long conclusionCount = conclusions.stream().filter(c -> c.getObjective() != null && c.getObjective().getId().equals(objective.getId())).count();
            long recommendationCount = recommendations.stream().filter(r -> r.getFindings().stream().anyMatch(f -> f.getObjective() != null && f.getObjective().getId().equals(objective.getId())) || r.getConclusions().stream().anyMatch(c -> c.getObjective() != null && c.getObjective().getId().equals(objective.getId()))).count();
            if (analysisCount > 0 && findingCount == 0) warnings.add("Analysis exists but no finding has been created.");
            if (findingCount > 0 && conclusionCount == 0) warnings.add("Finding exists but no conclusion has been created.");
            if (conclusionCount > 0 && recommendationCount == 0) warnings.add("Conclusion exists but no recommendation has been created.");
            rows.add(new TraceabilityRow(objective.getId(), objective.getText(), questions.isEmpty() ? null : questions.getFirst().getId(), null, analysisCount, findingCount, conclusionCount, recommendationCount, warnings));
        }
        List<String> projectWarnings = new ArrayList<>();
        if (resultRepository.countByProjectId(projectId) > 0 && findingRepository.countByProjectId(projectId) == 0) projectWarnings.add("Project has analysis results but no findings.");
        if (recommendations.stream().anyMatch(r -> r.getFindings().isEmpty() && r.getConclusions().isEmpty())) projectWarnings.add("Recommendation exists with no supporting finding or conclusion.");
        return new TraceabilityMatrixResponse(projectId, rows, projectWarnings);
    }

    private void createDefaultChapters(ResearchReport report, User user) {
        Object[][] chapters = {
                {ReportChapterType.PRELIMINARY, "Abstract", null},
                {ReportChapterType.INTRODUCTION, "Chapter One: Introduction", 1},
                {ReportChapterType.LITERATURE_REVIEW, "Chapter Two: Literature Review", 2},
                {ReportChapterType.METHODOLOGY, "Chapter Three: Methodology", 3},
                {ReportChapterType.RESULTS, "Chapter Four: Results and Discussion", 4},
                {ReportChapterType.CONCLUSION_RECOMMENDATIONS, "Chapter Five: Conclusion and Recommendations", 5},
                {ReportChapterType.REFERENCES, "References", null},
                {ReportChapterType.APPENDICES, "Appendices", null}
        };
        int order = 1;
        for (Object[] spec : chapters) {
            ResearchReportChapter chapter = new ResearchReportChapter();
            chapter.setReport(report);
            chapter.setType((ReportChapterType) spec[0]);
            chapter.setTitle((String) spec[1]);
            chapter.setChapterNumber((Integer) spec[2]);
            chapter.setDisplayOrder(order++);
            chapter.setRequired(true);
            chapter.setSystemDefined(true);
            ResearchReportChapter saved = chapterRepository.save(chapter);
            createSeedSection(saved, user);
        }
    }

    private void createSeedSection(ResearchReportChapter chapter, User user) {
        ResearchReportSection section = new ResearchReportSection();
        section.setChapter(chapter);
        section.setDisplayOrder(1);
        section.setCreatedBy(user);
        section.setOrigin(ContentOrigin.USER);
        section.setRequired(true);
        section.setSystemDefined(true);
        section.setAiEnabled(chapter.getType() != ReportChapterType.REFERENCES);
        switch (chapter.getType()) {
            case PRELIMINARY -> { section.setType(ReportSectionType.ABSTRACT); section.setHeading("Abstract"); section.setSourceArtifactType("Abstract"); }
            case INTRODUCTION -> { section.setType(ReportSectionType.PROBLEM_STATEMENT); section.setHeading("Problem Statement"); section.setSourceArtifactType("ResearchProblem"); }
            case LITERATURE_REVIEW -> { section.setType(ReportSectionType.LITERATURE_REVIEW); section.setHeading("Literature Review"); section.setSourceArtifactType("LiteratureReview"); }
            case METHODOLOGY -> { section.setType(ReportSectionType.METHODOLOGY); section.setHeading("Methodology"); section.setSourceArtifactType("Methodology"); }
            case RESULTS -> { section.setType(ReportSectionType.FINDINGS); section.setHeading("Findings"); section.setSourceArtifactType("ResearchFinding"); }
            case DISCUSSION, CONCLUSION_RECOMMENDATIONS -> { section.setType(ReportSectionType.CONCLUSIONS); section.setHeading("Conclusion and Recommendations"); section.setSourceArtifactType("ResearchConclusion"); }
            case REFERENCES -> { section.setType(ReportSectionType.REFERENCES); section.setHeading("References"); section.setAiEnabled(false); }
            default -> { section.setType(ReportSectionType.CUSTOM); section.setHeading(chapter.getTitle()); }
        }
        sectionRepository.save(section);
    }

    private String buildSectionGenerationPrompt(ResearchReportSection section, GenerateSectionRequest request) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("Write only the body content for the report section titled '")
                .append(section.getHeading())
                .append("' for ")
                .append(section.getChapter().getTitle())
                .append(". ");
        prompt.append("The report template owns the section heading, chapter heading, title page, table of contents, references, and appendices. ");
        prompt.append("Do not repeat the outer section heading. Do not include preliminary pages, title page labels, table of contents, references, or bibliography content. ");
        if (section.getType() == ReportSectionType.LITERATURE_REVIEW) {
            prompt.append("Produce a professional academic literature review with thematic synthesis, methodological comparison, agreements, disagreements, limitations, and research gaps. ");
        } else {
            prompt.append("Use a professional academic report style and cite every source-grounded claim. ");
        }
        prompt.append("Use Markdown only when structure is needed for subsection headings, lists, or tables; do not use Markdown for the known outer section heading. ");
        prompt.append("Use only retrieved evidence and cite supplied evidence markers. Do not fabricate authors, years, journals, DOI, page numbers, or findings.");
        if (request != null && request.instructions() != null && !request.instructions().isBlank()) {
            prompt.append("\n\nCustom instructions:\n").append(request.instructions().trim());
        }
        return prompt.toString();
    }

    private String buildSectionRetrievalQuery(ResearchReportSection section, GenerateSectionRequest request) {
        StringBuilder query = new StringBuilder();
        query.append(section.getHeading()).append(" ");
        if (section.getType() == ReportSectionType.LITERATURE_REVIEW) {
            query.append("research objective methodology findings limitations theory research gaps thematic literature review");
        } else {
            query.append(section.getChapter().getTitle()).append(" academic report evidence");
        }
        if (request != null && request.instructions() != null && !request.instructions().isBlank()) {
            query.append(" ").append(request.instructions().trim());
        }
        return query.toString();
    }

    private GeneratedDraftResponse persistGeneratedSection(UUID sectionId, GroundedAnswerResponse answer, User user) {
        return transactionTemplate.execute(status -> {
            ResearchReportSection managedSection = sectionRepository.findByIdForUpdate(sectionId)
                    .orElseThrow(() -> new ResourceNotFoundException("Report section not found."));
            String normalizedMarkdown = markdownRenderer.normalizeSectionMarkdown(managedSection.getHeading(), answer.answer());
            managedSection.setContent(normalizedMarkdown);
            managedSection.setContentJson(richTextService.markdownToDocumentJson(normalizedMarkdown));
            managedSection.setPlainText(richTextService.plainTextFromDocumentJson(managedSection.getContentJson()));
            managedSection.setStatus(managedSection.getPlainText() == null || managedSection.getPlainText().isBlank() ? ReportSectionStatus.NOT_STARTED : ReportSectionStatus.DRAFT);
            managedSection.setOrigin(ContentOrigin.AI_GENERATED);
            managedSection.setUpdatedBy(user);
            managedSection.setSourceOutOfDate(false);
            managedSection.setRevisionNumber(managedSection.getRevisionNumber() + 1);
            sectionRepository.save(managedSection);
            persistSectionCitations(managedSection, answer, user);
            auditService.record(user.getId(), SecurityAuditEventType.REPORT_SECTION_UPDATED);
            return new GeneratedDraftResponse(
                    managedSection.getContent(),
                    answer.citations().stream().map(com.researchassistant.rag.dto.response.CitationResponse::documentId).distinct().toList(),
                    "SOURCE_GROUNDED_RAG",
                    answer.citations(),
                    answer.retrievalSummary(),
                    managedSection.getUpdatedAt()
            );
        });
    }

    private void persistSectionCitations(ResearchReportSection section, GroundedAnswerResponse answer, User user) {
        citationRepository.deleteAllBySectionId(section.getId());
        citationRepository.flush();
        List<RagQueryEvidence> evidence = ragEvidenceRepository.findWithTraceByQueryIdOrderByEvidenceOrdinalAsc(answer.queryId());
        List<ResearchReportCitation> citations = new ArrayList<>();
        int ordinal = 1;
        for (com.researchassistant.rag.dto.response.CitationResponse renderedCitation : answer.citations()) {
            RagQueryEvidence matched = evidence.stream()
                    .filter(item -> item.getDocumentId().equals(renderedCitation.documentId()))
                    .filter(item -> item.getDocumentVersionId().equals(renderedCitation.documentVersionId()))
                    .filter(item -> item.getPageNumber() == renderedCitation.pageNumber())
                    .filter(item -> item.getChunkNumber() == renderedCitation.chunkNumber())
                    .findFirst()
                    .orElse(null);
            if (matched == null) {
                continue;
            }
            var chunk = matched.getChunk();
            var version = chunk.getDocumentVersion();
            var document = version.getDocument();
            ProjectReference projectReference = referenceRegistryService.ensureForDocument(document, version, user);
            ResearchReportCitation citation = new ResearchReportCitation();
            citation.setSection(section);
            citation.setDocument(document);
            citation.setDocumentVersion(version);
            citation.setPage(chunk.getPage());
            citation.setChunk(chunk);
            citation.setDocumentCode(matched.getDocumentCode());
            citation.setCitationOrdinal(ordinal++);
            citation.setSupportingTextSnapshot(matched.getTextSnapshot());
            citation.setReference(projectReference.getReference());
            citation.setProjectReference(projectReference);
            citations.add(citation);
        }
        citationRepository.saveAll(citations);
        refreshReportReferencesInternal(section.getChapter().getReport(), user);
    }

    private void cleanLiteratureReviewAndExtractMatrix(ResearchReport report, User user) {
        List<ResearchReportSection> sections = sectionRepository.findAllByChapterReportId(report.getId());
        for (ResearchReportSection section : sections) {
            if (section.getType() == ReportSectionType.LITERATURE_REVIEW && section.getContent() != null) {
                String content = section.getContent();
                if (content.contains("Source-level evidence assessment") || (content.contains("| Source") && content.contains("| Relevance"))) {
                    int tableStart = content.indexOf("### Source-level evidence assessment");
                    if (tableStart == -1) {
                        tableStart = content.indexOf("| Source");
                    }
                    if (tableStart == -1) {
                        continue;
                    }

                    int proseStart = -1;
                    int nextHeading = content.indexOf("\n### ", tableStart + 35);
                    if (nextHeading != -1) {
                        proseStart = nextHeading + 1;
                    } else {
                        nextHeading = content.indexOf("\n## ", tableStart + 35);
                        if (nextHeading != -1) {
                            proseStart = nextHeading + 1;
                        }
                    }

                    String tablePart = proseStart != -1 ? content.substring(tableStart, proseStart).trim() : content.substring(tableStart).trim();
                    String prosePart = proseStart != -1 ? (content.substring(0, tableStart) + "\n\n" + content.substring(proseStart)).trim() : "";

                    ResearchProject project = report.getProject();
                    Optional<LiteratureMatrix> existingOpt = literatureMatrixRepository.findFirstByProjectIdOrderByCreatedAtDesc(project.getId());
                    if (existingOpt.isEmpty() || existingOpt.get().getMarkdownTable() == null || existingOpt.get().getMarkdownTable().isBlank()) {
                        LiteratureMatrix matrix = existingOpt.orElseGet(LiteratureMatrix::new);
                        matrix.setProject(project);
                        matrix.setTitle("Source-Level Evidence Matrix");
                        matrix.setOrigin(ContentOrigin.USER);
                        matrix.setCreatedBy(user != null ? user : report.getCreatedBy());
                        matrix.setMarkdownTable(tablePart);
                        literatureMatrixRepository.save(matrix);
                    }

                    if (!prosePart.isBlank()) {
                        section.setContent(prosePart);
                        section.setContentJson(richTextService.markdownToDocumentJson(prosePart));
                        section.setPlainText(richTextService.plainTextFromDocumentJson(section.getContentJson()));
                        sectionRepository.save(section);
                    }
                }
            }
        }
    }

    private void ensureReferencesSection(ResearchReport report, User user) {
        List<ResearchReportChapter> chapters = chapterRepository.findAllByReportIdOrderByDisplayOrderAsc(report.getId());
        ResearchReportChapter refChapter = chapters.stream()
                .filter(ch -> ch.getType() == ReportChapterType.REFERENCES)
                .findFirst()
                .orElseGet(() -> {
                    ResearchReportChapter ch = new ResearchReportChapter();
                    ch.setReport(report);
                    ch.setType(ReportChapterType.REFERENCES);
                    ch.setTitle("References");
                    ch.setRequired(true);
                    ch.setSystemDefined(true);
                    ch.setDisplayOrder(chOrderValue(chapters));
                    return chapterRepository.save(ch);
                });

        List<ResearchReportSection> sections = sectionRepository.findAllByChapterIdOrderByDisplayOrderAsc(refChapter.getId());
        ResearchReportSection refSection = sections.stream()
                .filter(s -> s.getType() == ReportSectionType.REFERENCES)
                .findFirst()
                .orElseGet(() -> {
                    ResearchReportSection s = new ResearchReportSection();
                    s.setChapter(refChapter);
                    s.setType(ReportSectionType.REFERENCES);
                    s.setHeading("References");
                    s.setRequired(true);
                    s.setSystemDefined(true);
                    s.setAiEnabled(false);
                    s.setDisplayOrder(1);
                    s.setOrigin(ContentOrigin.USER);
                    s.setCreatedBy(user);
                    return sectionRepository.save(s);
                });

        if (refSection.getType() != ReportSectionType.REFERENCES) {
            refSection.setType(ReportSectionType.REFERENCES);
            refSection.setAiEnabled(false);
            sectionRepository.save(refSection);
        }

        if (refSection.getContent() == null || refSection.getContent().isBlank() || refSection.getContent().contains("The supplied evidence presents") || refSection.getContent().contains("### Cross-study synthesis")) {
            refreshReportReferencesInternal(report, user);
        }
    }

    private void recalculateSectionNumbers(ResearchReport report) {
        List<ResearchReportChapter> chapters = chapterRepository.findAllByReportIdOrderByDisplayOrderAsc(report.getId());
        for (ResearchReportChapter chapter : chapters) {
            if (chapter.getChapterNumber() == null) {
                List<ResearchReportSection> allSections = sectionRepository.findAllByChapterIdOrderByDisplayOrderAsc(chapter.getId());
                for (ResearchReportSection s : allSections) {
                    if (s.getSectionNumber() != null) {
                        s.setSectionNumber(null);
                        sectionRepository.save(s);
                    }
                }
                continue;
            }
            int chNum = chapter.getChapterNumber();
            List<ResearchReportSection> rootSections = sectionRepository.findAllByChapterIdAndParentSectionIsNullOrderByDisplayOrderAsc(chapter.getId());
            for (int i = 0; i < rootSections.size(); i++) {
                ResearchReportSection root = rootSections.get(i);
                String rootNum = chNum + "." + (i + 1);
                root.setSectionNumber(rootNum);
                sectionRepository.save(root);

                List<ResearchReportSection> subSections = sectionRepository.findAllByParentSectionIdOrderByDisplayOrderAsc(root.getId());
                for (int j = 0; j < subSections.size(); j++) {
                    ResearchReportSection sub = subSections.get(j);
                    String subNum = rootNum + "." + (j + 1);
                    sub.setSectionNumber(subNum);
                    sectionRepository.save(sub);

                    List<ResearchReportSection> subSubs = sectionRepository.findAllByParentSectionIdOrderByDisplayOrderAsc(sub.getId());
                    for (int k = 0; k < subSubs.size(); k++) {
                        ResearchReportSection subSub = subSubs.get(k);
                        String subSubNum = subNum + "." + (k + 1);
                        subSub.setSectionNumber(subSubNum);
                        sectionRepository.save(subSub);
                    }
                }
            }
        }
    }

    private ResearchReportSection refreshReportReferencesInternal(ResearchReport report, User user) {
        List<ResearchReportChapter> chapters = chapterRepository.findAllByReportIdOrderByDisplayOrderAsc(report.getId());
        ResearchReportChapter refChapter = chapters.stream()
                .filter(ch -> ch.getType() == ReportChapterType.REFERENCES)
                .findFirst()
                .orElseGet(() -> {
                    ResearchReportChapter created = new ResearchReportChapter();
                    created.setReport(report);
                    created.setType(ReportChapterType.REFERENCES);
                    created.setTitle("References");
                    created.setRequired(true);
                    created.setSystemDefined(true);
                    created.setDisplayOrder(chOrderValue(chapters));
                    return chapterRepository.save(created);
                });

        ResearchReportSection refSection = sectionRepository.findAllByChapterIdOrderByDisplayOrderAsc(refChapter.getId()).stream()
                .filter(sec -> sec.getType() == ReportSectionType.REFERENCES)
                .findFirst()
                .orElseGet(() -> {
                    ResearchReportSection created = new ResearchReportSection();
                    created.setChapter(refChapter);
                    created.setType(ReportSectionType.REFERENCES);
                    created.setHeading("References");
                    created.setRequired(true);
                    created.setSystemDefined(true);
                    created.setAiEnabled(false);
                    created.setDisplayOrder(1);
                    created.setCreatedBy(user);
                    created.setOrigin(ContentOrigin.USER);
                    return sectionRepository.save(created);
                });

        Map<UUID, ReferenceEntry> referencesMap = new LinkedHashMap<>();
        List<ResearchReportCitation> citations = citationRepository.findAllBySectionChapterReportIdOrderByCitationOrdinalAsc(report.getId());
        for (ResearchReportCitation citation : citations) {
            ReferenceEntry reference = citation.getReference();
            if (reference == null && citation.getProjectReference() != null) {
                reference = citation.getProjectReference().getReference();
            }
            if (reference != null) {
                referencesMap.putIfAbsent(reference.getId(), reference);
            }
        }

        if (report.isIncludeUncitedReferences()) {
            for (ProjectReference pr : projectReferenceRepository.findAllByProjectId(report.getProject().getId())) {
                if (pr.getReference() != null) {
                    referencesMap.putIfAbsent(pr.getReference().getId(), pr.getReference());
                }
            }
        }

        List<ReferenceEntry> references = new ArrayList<>(referencesMap.values());
        CitationStyle style = report.getCitationStyle() != null ? report.getCitationStyle() : CitationStyle.APA_7;

        if (style != CitationStyle.IEEE && style != CitationStyle.VANCOUVER && style != CitationStyle.NUMERIC_APA) {
            references.sort((r1, r2) -> {
                String k1 = (r1.getTitle() == null ? "" : r1.getTitle()).toLowerCase();
                String k2 = (r2.getTitle() == null ? "" : r2.getTitle()).toLowerCase();
                return k1.compareTo(k2);
            });
        }

        StringBuilder markdownBuilder = new StringBuilder();
        int num = 1;
        for (ReferenceEntry ref : references) {
            var formatted = citationFormattingService.format(ref, style, CitationContext.REFERENCE_LIST, num++);
            String entryText = formatted.text();
            if (entryText != null && !entryText.isBlank()) {
                markdownBuilder.append(entryText.trim()).append("\n\n");
            }
        }

        String formattedMarkdown = markdownBuilder.toString().trim();
        refSection.setType(ReportSectionType.REFERENCES);
        refSection.setContent(formattedMarkdown);
        refSection.setContentJson(richTextService.markdownToDocumentJson(formattedMarkdown));
        refSection.setPlainText(richTextService.plainTextFromDocumentJson(refSection.getContentJson()));
        refSection.setStatus(references.isEmpty() ? ReportSectionStatus.NOT_STARTED : ReportSectionStatus.ACCEPTED);
        refSection.setOrigin(ContentOrigin.USER);
        refSection.setAiEnabled(false);
        refSection.setUpdatedBy(user);
        refSection.setRevisionNumber(refSection.getRevisionNumber() + 1);
        return sectionRepository.save(refSection);
    }

    private ResearchFinding loadFinding(UUID id) { return findingRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Finding not found.")); }
    private ResearchFinding loadFindingForEdit(UUID id, User user) { ResearchFinding f = loadFinding(id); authorizationService.requireProjectEditor(f.getProject().getId(), user); return f; }
    private FindingDiscussion loadDiscussion(UUID id) { return discussionRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Discussion not found.")); }
    private ResearchConclusion loadConclusion(UUID id) { return conclusionRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Conclusion not found.")); }
    private ResearchRecommendation loadRecommendation(UUID id) { return recommendationRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Recommendation not found.")); }
    private ResearchReport loadReport(UUID id) { return reportRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Research report not found.")); }
    private ResearchReport loadReportForEdit(UUID id, User user) { ResearchReport r = loadReport(id); authorizationService.requireProjectEditor(r.getProject().getId(), user); if (r.getStatus() == ResearchReportStatus.FINAL) throw new IllegalStateException("Final reports cannot be silently edited. Create a new revision first."); return r; }

    private List<AnalysisResult> loadAnalysisResults(List<UUID> ids, UUID projectId) {
        if (ids == null || ids.isEmpty()) throw new IllegalArgumentException("At least one analysis result is required.");
        List<AnalysisResult> results = resultRepository.findAllById(ids);
        if (results.size() != new LinkedHashSet<>(ids).size()) throw new ResourceNotFoundException("One or more analysis results were not found.");
        results.forEach(r -> requireProject(r.getProject().getId(), projectId));
        return results;
    }

    private List<ResearchFinding> loadFindings(List<UUID> ids, UUID projectId, boolean requireReviewedOrApproved) {
        if (ids == null || ids.isEmpty()) throw new IllegalArgumentException("At least one finding is required.");
        List<ResearchFinding> findings = findingRepository.findAllById(ids);
        if (findings.size() != new LinkedHashSet<>(ids).size()) throw new ResourceNotFoundException("One or more findings were not found.");
        for (ResearchFinding finding : findings) {
            requireProject(finding.getProject().getId(), projectId);
            if (requireReviewedOrApproved && finding.getStatus() != ResearchFindingStatus.REVIEWED && finding.getStatus() != ResearchFindingStatus.APPROVED) throw new IllegalStateException("Conclusions require reviewed or approved findings.");
        }
        return findings;
    }

    private List<ResearchConclusion> loadConclusions(List<UUID> ids, UUID projectId, boolean requireApproved) {
        if (ids == null || ids.isEmpty()) return List.of();
        List<ResearchConclusion> conclusions = conclusionRepository.findAllById(ids);
        if (conclusions.size() != new LinkedHashSet<>(ids).size()) throw new ResourceNotFoundException("One or more conclusions were not found.");
        for (ResearchConclusion conclusion : conclusions) {
            requireProject(conclusion.getProject().getId(), projectId);
            if (requireApproved && conclusion.getStatus() != ResearchConclusionStatus.APPROVED) throw new IllegalStateException("Only approved conclusions may be used here.");
        }
        return conclusions;
    }

    private ResearchObjective loadObjective(UUID id, UUID projectId) { if (id == null) return null; ResearchObjective o = objectiveRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Research objective not found.")); requireProject(o.getProject().getId(), projectId); return o; }
    private ResearchQuestion loadQuestion(UUID id, UUID projectId) { if (id == null) return null; ResearchQuestion q = questionRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Research question not found.")); requireProject(q.getProject().getId(), projectId); return q; }
    private ResearchHypothesis loadHypothesis(UUID id, UUID projectId) { if (id == null) return null; ResearchHypothesis h = hypothesisRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Research hypothesis not found.")); requireProject(h.getProject().getId(), projectId); return h; }
    private ResearchDataset loadDataset(UUID id, UUID projectId) { if (id == null) return null; ResearchDataset d = datasetRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Dataset not found.")); requireProject(d.getProject().getId(), projectId); return d; }
    private ResearchReportTemplate loadTemplate(UUID id, ResearchReportType type) { if (id != null) return templateRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Report template not found.")); return templateRepository.findFirstByTypeAndSystemTemplateTrueOrderByCreatedAtAsc(type).orElse(null); }
    private void requireProject(UUID actual, UUID expected) { if (!expected.equals(actual)) throw new IllegalArgumentException("Referenced research artifact belongs to another project."); }
    private void requireNotArchived(ResearchFindingStatus status) { if (status == ResearchFindingStatus.ARCHIVED) throw new IllegalStateException("Archived findings cannot be updated."); }
    private ContentOrigin defaultOrigin(ContentOrigin origin) { return origin == null ? ContentOrigin.USER : origin; }
    private List<UUID> nullToEmpty(List<UUID> ids) { return ids == null ? List.of() : ids; }
    private String required(String value, String message) { String normalized = optional(value); if (normalized == null) throw new IllegalArgumentException(message); return normalized; }
    private String optional(String value) { if (value == null) return null; String v = value.trim(); return v.isEmpty() ? null : v; }
    private void setFindingText(ResearchFinding finding, String text) { String v = required(text, "Finding text is required."); finding.setFindingText(v); finding.setStatement(v); }
    private void setDiscussionText(FindingDiscussion discussion, String text) { String v = required(text, "Discussion text is required."); discussion.setDiscussionText(v); discussion.setInterpretation(v); }
    private void setConclusionText(ResearchConclusion conclusion, String text) { String v = required(text, "Conclusion text is required."); conclusion.setConclusionText(v); conclusion.setStatement(v); }
    private void setRecommendationText(ResearchRecommendation recommendation, String text) { String v = required(text, "Recommendation text is required."); recommendation.setRecommendationText(v); recommendation.setRecommendation(v); }
    private void afterResearchOutputChanged(UUID projectId, UUID userId, SecurityAuditEventType eventType) { cacheInvalidationService.evictProjectMetadata(projectId); auditService.record(userId, eventType); }
    private Integer resolveSourceRevision(String type, UUID id) { if (type == null || id == null) return null; return switch (type) { case "ResearchFinding" -> findingRepository.findById(id).map(ResearchFinding::getRevisionNumber).orElse(null); case "DiscussionOfFinding" -> discussionRepository.findById(id).map(FindingDiscussion::getRevisionNumber).orElse(null); case "ResearchConclusion" -> conclusionRepository.findById(id).map(ResearchConclusion::getRevisionNumber).orElse(null); case "ResearchRecommendation" -> recommendationRepository.findById(id).map(ResearchRecommendation::getRevisionNumber).orElse(null); default -> null; }; }
    private void markSectionsStale(String artifactType, UUID artifactId) { sectionRepository.findAll().stream().filter(s -> artifactType.equals(s.getSourceArtifactType()) && artifactId.equals(s.getSourceArtifactId()) && s.isManuallyEdited()).forEach(s -> s.setSourceOutOfDate(true)); }
    private Map<UUID, ReferenceEntry> citedReferencesForValidation(UUID reportId) {
        Map<UUID, ReferenceEntry> references = new LinkedHashMap<>();
        for (ResearchReportCitation citation : citationRepository.findAllBySectionChapterReportIdOrderByCitationOrdinalAsc(reportId)) {
            ReferenceEntry reference = citation.getReference();
            if (reference == null && citation.getProjectReference() != null) {
                reference = citation.getProjectReference().getReference();
            }
            if (reference != null) {
                references.putIfAbsent(reference.getId(), reference);
            }
        }
        return references;
    }
}
