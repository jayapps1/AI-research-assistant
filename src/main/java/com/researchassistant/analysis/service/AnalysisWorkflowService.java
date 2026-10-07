package com.researchassistant.analysis.service;

import com.researchassistant.analysis.dto.AnalysisDtos.*;
import com.researchassistant.analysis.entity.*;
import com.researchassistant.analysis.exception.ReportValidationException;
import com.researchassistant.analysis.exception.InsufficientProjectEvidenceException;
import com.researchassistant.analysis.repository.*;
import com.researchassistant.cache.CacheInvalidationService;
import com.researchassistant.common.enums.ContentOrigin;
import com.researchassistant.common.exception.ResourceNotFoundException;
import com.researchassistant.project.entity.AcademicProjectType;
import com.researchassistant.dataset.model.ResearchDataset;
import com.researchassistant.dataset.repository.ResearchDatasetRepository;
import com.researchassistant.document.entity.DocumentStatus;
import com.researchassistant.document.repository.DocumentRepository;
import com.researchassistant.identity.entity.User;
import com.researchassistant.methodology.repository.MethodologyRepository;
import com.researchassistant.project.entity.AcademicWorkspaceType;
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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.OffsetDateTime;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class AnalysisWorkflowService {
    private static final Logger log = LoggerFactory.getLogger(AnalysisWorkflowService.class);
    private static final String MANUAL_CITATION_SNAPSHOT = "Manual citation inserted in report editor.";

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
    private final ReportSectionPromptBuilder promptBuilder;
    private final TitlePageRenderer titlePageRenderer;
    private final FrontMatterRenderer frontMatterRenderer;
    private final DocumentStructureExtractors structureExtractors;
    private final ReportDocumentCompiler reportDocumentCompiler;
    private final TransactionTemplate transactionTemplate;
    private final AcademicSectionGenerationService academicSectionGenerationService;

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
            ReportSectionPromptBuilder promptBuilder, TitlePageRenderer titlePageRenderer,
            FrontMatterRenderer frontMatterRenderer, DocumentStructureExtractors structureExtractors,
            ReportDocumentCompiler reportDocumentCompiler,
            AcademicSectionGenerationService academicSectionGenerationService,
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
        this.promptBuilder = promptBuilder;
        this.titlePageRenderer = titlePageRenderer;
        this.frontMatterRenderer = frontMatterRenderer;
        this.structureExtractors = structureExtractors;
        this.reportDocumentCompiler = reportDocumentCompiler;
        this.academicSectionGenerationService = academicSectionGenerationService;
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
        report.setType(request.type() == null ? defaultReportType(project) : request.type());
        ResearchReportTemplate template = request.templateId() != null
                ? loadTemplate(request.templateId(), report.getType(), workspaceType(project))
                : project.getReportTemplate() != null
                        ? project.getReportTemplate()
                        : loadTemplate(null, report.getType(), workspaceType(project));
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
                    : loadTemplate(null, report.getType(), workspaceType(project));
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
        if (request.templateId() != null) {
            ResearchReportTemplate template = loadTemplate(request.templateId(), report.getType(), workspaceType(report.getProject()));
            report.setTemplate(template);
            if (template.getDefaultCitationStyle() != null && !template.isCitationStyleLocked()) {
                report.setCitationStyle(template.getDefaultCitationStyle());
            }
            ensureReportStructure(report, user);
        }
        if (request.institutionName() != null) report.setInstitutionName(optional(request.institutionName()));
        if (request.departmentName() != null) report.setDepartmentName(optional(request.departmentName()));
        if (request.authorName() != null) report.setAuthorName(optional(request.authorName()));
        if (request.supervisorName() != null) report.setSupervisorName(optional(request.supervisorName()));
        if (request.degreeProgram() != null) report.setDegreeProgram(optional(request.degreeProgram()));
        if (request.submissionYear() != null) report.setSubmissionYear(request.submissionYear());
        boolean refreshReferences = false;
        if (request.citationStyle() != null && request.citationStyle() != report.getCitationStyle()) {
            report.setCitationStyle(request.citationStyle());
            refreshReferences = true;
        }
        report.setRevisionNumber(report.getRevisionNumber() + 1);
        if (refreshReferences) {
            refreshReportReferencesInternal(report, user);
        }
        cacheInvalidationService.evictProjectMetadata(report.getProject().getId());
        return ReportResponse.from(report);
    }

    @Transactional
    public ReportResponse applyTemplateFormatting(UUID reportId, User user) {
        ResearchReport report = loadReportForEdit(reportId, user);
        if (report.getTemplate() == null) {
            report.setTemplate(loadTemplate(null, report.getType(), workspaceType(report.getProject())));
        }
        if (report.getTemplate() != null && report.getTemplate().getDefaultCitationStyle() != null) {
            report.setCitationStyle(report.getTemplate().getDefaultCitationStyle());
        }
        ensureReportStructure(report, user);
        refreshReportReferencesInternal(report, user);
        report.setRevisionNumber(report.getRevisionNumber() + 1);
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
                : loadTemplate(null, defaultReportType(project), workspaceType(project));
        ResearchReport report = new ResearchReport();
        report.setProject(project);
        report.setTemplate(template);
        report.setTitle(defaultReportTitle(project));
        report.setType(template == null ? defaultReportType(project) : template.getType());
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
            chapter.setRequired(chNode.has("required") ? chNode.get("required").asBoolean() : chapter.isRequired());
            chapter.setSystemDefined(chNode.has("systemDefined") ? chNode.get("systemDefined").asBoolean() : true);
            chapterRepository.save(chapter);
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

    private int nextChapterDisplayOrder(ResearchReport report) {
        return chapterRepository.findAllByReportIdOrderByDisplayOrderAsc(report.getId()).stream()
                .mapToInt(ResearchReportChapter::getDisplayOrder)
                .max()
                .orElse(0) + 1;
    }

    private int nextChapterDisplayOrder(ResearchReport report, ReportChapterType type) {
        if (type == ReportChapterType.PRELIMINARY) {
            return 1;
        }
        if (!isNumberedChapterType(type)) {
            return nextChapterDisplayOrder(report);
        }
        return chapterRepository.findAllByReportIdOrderByDisplayOrderAsc(report.getId()).stream()
                .filter(chapter -> chapter.getType() == ReportChapterType.REFERENCES || chapter.getType() == ReportChapterType.APPENDICES)
                .mapToInt(ResearchReportChapter::getDisplayOrder)
                .min()
                .orElseGet(() -> nextChapterDisplayOrder(report));
    }

    private void shiftChaptersAtOrAfter(ResearchReport report, int displayOrder) {
        List<ResearchReportChapter> toShift = chapterRepository.findAllByReportIdOrderByDisplayOrderAsc(report.getId()).stream()
                .filter(chapter -> chapter.getDisplayOrder() >= displayOrder)
                .toList();
        for (ResearchReportChapter chapter : toShift) {
            chapter.setDisplayOrder(chapter.getDisplayOrder() + 1);
        }
        chapterRepository.saveAll(toShift);
        chapterRepository.flush();
    }

    private int nextChapterNumber(ResearchReport report) {
        return chapterRepository.findAllByReportIdOrderByDisplayOrderAsc(report.getId()).stream()
                .filter(chapter -> isNumberedChapterType(chapter.getType()))
                .map(ResearchReportChapter::getChapterNumber)
                .filter(Objects::nonNull)
                .mapToInt(Integer::intValue)
                .max()
                .orElse(0) + 1;
    }

    private int nextSectionDisplayOrder(UUID chapterId, UUID parentSectionId) {
        return parentSectionId == null
                ? (int) sectionRepository.countByChapterIdAndParentSectionIsNull(chapterId) + 1
                : (int) sectionRepository.countByParentSectionId(parentSectionId) + 1;
    }

    private boolean isNumberedChapterType(ReportChapterType type) {
        return type != ReportChapterType.PRELIMINARY
                && type != ReportChapterType.REFERENCES
                && type != ReportChapterType.APPENDICES;
    }

    private ChapterTitleParts parseChapterTitle(String rawTitle) {
        String title = rawTitle == null ? "" : rawTitle.trim();
        java.util.regex.Matcher matcher = java.util.regex.Pattern
                .compile("(?i)^\\s*chapter\\s+(\\d+|one|two|three|four|five|six|seven|eight|nine|ten|eleven|twelve|thirteen|fourteen|fifteen|sixteen|seventeen|eighteen|nineteen|twenty)(?:(?:\\s*[:\\-.\u2013\u2014]\\s*|\\s+)(.+))?$")
                .matcher(title);
        if (!matcher.matches()) {
            return new ChapterTitleParts(title, null);
        }
        Integer chapterNumber = parseChapterNumberWord(matcher.group(1));
        String cleanedTitle = (matcher.group(2) == null || matcher.group(2).isBlank()) ? title : matcher.group(2).trim();
        return new ChapterTitleParts(cleanedTitle, chapterNumber);
    }

    private Integer parseChapterNumberWord(String value) {
        if (value == null || value.isBlank()) return null;
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException ignored) {
            return switch (value.toLowerCase(Locale.ROOT)) {
                case "one" -> 1;
                case "two" -> 2;
                case "three" -> 3;
                case "four" -> 4;
                case "five" -> 5;
                case "six" -> 6;
                case "seven" -> 7;
                case "eight" -> 8;
                case "nine" -> 9;
                case "ten" -> 10;
                case "eleven" -> 11;
                case "twelve" -> 12;
                case "thirteen" -> 13;
                case "fourteen" -> 14;
                case "fifteen" -> 15;
                case "sixteen" -> 16;
                case "seventeen" -> 17;
                case "eighteen" -> 18;
                case "nineteen" -> 19;
                case "twenty" -> 20;
                default -> null;
            };
        }
    }

    private record ChapterTitleParts(String title, Integer chapterNumber) {}

    private void ensureTemplateSections(ResearchReportChapter chapter, JsonNode sectionsNode, User user) {
        ensureTemplateSections(chapter, null, sectionsNode, user);
    }

    private void ensureTemplateSections(ResearchReportChapter chapter, ResearchReportSection parent, JsonNode sectionsNode, User user) {
        List<ResearchReportSection> existing = new ArrayList<>(sectionRepository.findAllByChapterIdOrderByDisplayOrderAsc(chapter.getId()));
        int order = 1;
        for (JsonNode secNode : sectionsNode) {
            ReportSectionType type = parseSectionType(secNode.has("type") ? secNode.get("type").asText() : "CUSTOM");
            String heading = secNode.has("heading") ? secNode.get("heading").asText() : "Section " + order;
            int displayOrder = order++;
            ResearchReportSection section = findSection(existing, type, heading, parent == null ? null : parent.getId())
                    .orElseGet(() -> {
                        ResearchReportSection created = new ResearchReportSection();
                        created.setChapter(chapter);
                        created.setParentSection(parent);
                        created.setType(type);
                        created.setHeading(heading);
                        created.setDisplayOrder(displayOrder);
                        created.setOrigin(ContentOrigin.USER);
                        created.setCreatedBy(user);
                        ResearchReportSection saved = sectionRepository.save(created);
                        existing.add(saved);
                        return saved;
                    });
            section.setRequired(secNode.has("required") ? secNode.get("required").asBoolean() : section.isRequired());
            section.setSystemDefined(secNode.has("systemDefined") ? secNode.get("systemDefined").asBoolean() : true);
            if (secNode.has("semanticPurpose")) {
                try {
                    section.setSemanticPurpose(SectionSemanticPurpose.valueOf(secNode.get("semanticPurpose").asText()));
                } catch (Exception ignored) {
                    section.setSemanticPurpose(section.resolveSemanticPurpose());
                }
            } else {
                SectionSemanticPurpose inferred = inferTemplateSemanticPurpose(type, heading, chapter.getType());
                if (section.getSemanticPurpose() == null || section.getSemanticPurpose() == SectionSemanticPurpose.CUSTOM || inferred != SectionSemanticPurpose.CUSTOM) {
                    section.setSemanticPurpose(inferred);
                }
            }

            if (secNode.has("generationPolicy")) {
                try {
                    section.setGenerationPolicy(SectionGenerationPolicy.valueOf(secNode.get("generationPolicy").asText()));
                } catch (Exception ignored) {
                    section.setGenerationPolicy(section.resolveGenerationPolicy());
                }
            } else {
                section.setGenerationPolicy(section.resolveGenerationPolicy());
            }

            boolean isDeterministic = section.resolveGenerationPolicy() == SectionGenerationPolicy.DETERMINISTIC;
            section.setAiEnabled(!isDeterministic && (!secNode.has("aiEnabled") || secNode.get("aiEnabled").asBoolean()));
            if (isDeterministic) {
                section.setOrigin(ContentOrigin.DETERMINISTIC_SYSTEM);
                if (section.getContent() == null || section.getContent().isBlank() || looksLikeGeneratedLiteratureProse(section.getContent())) {
                    initializeDeterministicContent(chapter.getReport(), section, user);
                }
            } else if (section.resolveSemanticPurpose() == SectionSemanticPurpose.DECLARATION && (section.getContent() == null || section.getContent().isBlank())) {
                initializeDeclarationContent(chapter.getReport(), section);
            } else if (section.resolveSemanticPurpose() == SectionSemanticPurpose.CERTIFICATION && (section.getContent() == null || section.getContent().isBlank())) {
                initializeCertificationContent(chapter.getReport(), section);
            }
            sectionRepository.save(section);
            ensureRichSection(section);
            JsonNode childrenNode = secNode.has("subsections") ? secNode.get("subsections") : secNode.get("sections");
            if (childrenNode != null && childrenNode.isArray()) {
                ensureTemplateSections(chapter, section, childrenNode, user);
            }
        }
    }

    private SectionSemanticPurpose inferTemplateSemanticPurpose(
            ReportSectionType type,
            String heading,
            ReportChapterType chapterType
    ) {
        ResearchReportSection probe = new ResearchReportSection();
        ResearchReportChapter probeChapter = new ResearchReportChapter();
        probeChapter.setType(chapterType);
        probe.setChapter(probeChapter);
        probe.setType(type);
        probe.setHeading(heading);
        return probe.resolveSemanticPurpose();
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

    private Optional<ResearchReportSection> findSection(List<ResearchReportSection> sections, ReportSectionType type, String heading, UUID parentId) {
        return sections.stream()
                .filter(section -> Objects.equals(section.getParentSection() == null ? null : section.getParentSection().getId(), parentId))
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
        ResearchProject project = authorizationService.requireProjectViewer(projectId, user).project();
        long datasetCount = datasetRepository.countByProjectId(projectId);
        long resultCount = resultRepository.countByProjectId(projectId);
        long findingCount = findingRepository.countByProjectId(projectId);

        boolean hasData = datasetCount > 0 || resultCount > 0;
        boolean hasFindings = findingCount > 0;

        List<SectionCapability> capabilities = new ArrayList<>();
        switch (workspaceType(project)) {
            case ACADEMIC_PROJECT -> {
                capabilities.add(new SectionCapability("BACKGROUND", "READY", "Project background and context", false, false));
                capabilities.add(new SectionCapability("PROBLEM_STATEMENT", "READY", "Practical or technical problem definition", false, false));
                capabilities.add(new SectionCapability("OBJECTIVES", "READY", "Project aim and objectives", false, false));
                capabilities.add(new SectionCapability("LITERATURE_REVIEW", "READY", "Related work and technical literature synthesis", false, false));
                capabilities.add(new SectionCapability("REQUIREMENTS", "READY", "Requirements, constraints, and design criteria", false, false));
                capabilities.add(new SectionCapability("SYSTEM_DESIGN", "READY", "System analysis, design, architecture, or database design", false, false));
                capabilities.add(new SectionCapability("IMPLEMENTATION", "READY", "Implementation discussion without inventing completed work", false, false));
                capabilities.add(new SectionCapability("TESTING", hasData ? "READY" : "DATA_REQUIRED", "Testing or evaluation discussion grounded in actual results", true, false));
                capabilities.add(new SectionCapability("CONCLUSION", hasFindings ? "READY" : "FINDINGS_REQUIRED", "Conclusion based on actual project outcomes", true, true));
                capabilities.add(new SectionCapability("FUTURE_WORK", "READY", "Future work and maintenance recommendations", false, false));
            }
            case COURSEWORK -> {
                capabilities.add(new SectionCapability("OUTLINE", "READY", "Coursework outline based on instructions", false, false));
                capabilities.add(new SectionCapability("INTRODUCTION", "READY", "Introductory coursework section", false, false));
                capabilities.add(new SectionCapability("LITERATURE_REVIEW", "READY", "Source-grounded literature discussion where sources are selected", false, false));
                capabilities.add(new SectionCapability("MAIN_DISCUSSION", "READY", "Main discussion or argument", false, false));
                capabilities.add(new SectionCapability("CASE_STUDY", "READY", "Case study discussion based on selected scope", false, false));
                capabilities.add(new SectionCapability("ANALYSIS", "READY", "Coursework analysis from instructions and sources", false, false));
                capabilities.add(new SectionCapability("CONCLUSION", "READY", "Conclusion without invented findings", false, false));
                capabilities.add(new SectionCapability("RECOMMENDATIONS", "READY", "Recommendations where appropriate to the assignment", false, false));
            }
            case ACADEMIC_RESEARCH -> {
                capabilities.add(new SectionCapability("BACKGROUND", "READY", "Broader scholarly and contextual foundation", false, false));
                capabilities.add(new SectionCapability("PROBLEM_STATEMENT", "READY", "Formulate empirical gap, context, and problem magnitude", false, false));
                capabilities.add(new SectionCapability("OBJECTIVES", "READY", "Hierarchical research aims derived from topic and problem", false, false));
                capabilities.add(new SectionCapability("RESEARCH_QUESTIONS", "READY", "Empirically answerable research questions aligned with aims", false, false));
                capabilities.add(new SectionCapability("LITERATURE_REVIEW", "READY", "Thematic synthesis, comparative analysis, and research gaps across sources", false, false));
                capabilities.add(new SectionCapability("RESEARCH_GAP", "READY", "Synthesized omissions and methodological limitations", false, false));
                capabilities.add(new SectionCapability("CONCEPTUAL_FRAMEWORK", "READY", "Key constructs, variables, and hypothesized interrelationships", false, false));
                capabilities.add(new SectionCapability("THEORETICAL_FRAMEWORK", "READY", "Grounding theoretical paradigms and explanatory models", false, false));
                capabilities.add(new SectionCapability("METHODOLOGY", "READY", "Research design, population, and sampling strategy proposal", false, false));
                capabilities.add(new SectionCapability("FINDINGS", hasData ? "READY" : "DATA_REQUIRED", "Empirical results synthesized from actual research data", true, false));
                capabilities.add(new SectionCapability("DISCUSSION", hasFindings ? "READY" : "FINDINGS_REQUIRED", "Interpretation of empirical findings contextualized against literature", true, true));
                capabilities.add(new SectionCapability("CONCLUSION", hasFindings ? "READY" : "FINDINGS_REQUIRED", "Synthesized conclusions directly addressing research objectives", true, true));
                capabilities.add(new SectionCapability("RECOMMENDATIONS", hasFindings ? "READY" : "FINDINGS_REQUIRED", "Actionable practical, policy, and future research recommendations", true, true));
            }
        }
        capabilities.add(new SectionCapability("CUSTOM_DOCUMENT_SECTION", "READY", "Generate for any user-created chapter, heading, subheading, or appendix target node", false, false));

        return new SectionCapabilitiesResponse(projectId, capabilities);
    }

    @Transactional(readOnly = true)
    public TableOfContentsResponse generateTableOfContents(UUID reportId, User user) {
        ResearchReport report = loadReport(reportId);
        authorizationService.requireProjectViewer(report.getProject().getId(), user);
        List<ResearchReportChapter> chapters = chapterRepository.findAllByReportIdOrderByDisplayOrderAsc(reportId);

        StringBuilder md = new StringBuilder();
        md.append("# TABLE OF CONTENTS\n\n");

        boolean isCoursework = isCourseworkReport(report);
        List<TableOfContentsItem> chapterItems = new ArrayList<>();
        for (ResearchReportChapter ch : chapters) {
            List<ResearchReportSection> sections = sectionRepository.findAllByChapterIdOrderByDisplayOrderAsc(ch.getId());
            Map<UUID, List<ResearchReportSection>> sectionsByParent = sections.stream()
                    .filter(section -> section.getParentSection() != null)
                    .collect(java.util.stream.Collectors.groupingBy(section -> section.getParentSection().getId()));
            List<ResearchReportSection> rootSections = sections.stream()
                    .filter(section -> section.getParentSection() == null)
                    .toList();

            List<TableOfContentsSectionItem> sectionItems = new ArrayList<>();
            if (!isCoursework) {
                String chapterPrefix = ch.getChapterNumber() != null ? "Chapter " + ch.getChapterNumber() + ": " : "";
                md.append("### ").append(chapterPrefix).append(ch.getTitle()).append("\n");
            }
            for (ResearchReportSection sec : rootSections) {
                sectionItems.add(tocSectionItem(sec, sectionsByParent, md, 0, isCoursework));
            }
            if (!isCoursework) {
                md.append("\n");
            }
            chapterItems.add(new TableOfContentsItem(ch.getTitle(), ch.getChapterNumber(), ch.getDisplayOrder(), sectionItems));
        }

        return new TableOfContentsResponse(reportId, report.getTitle(), chapterItems, md.toString());
    }

    private TableOfContentsSectionItem tocSectionItem(ResearchReportSection section,
            Map<UUID, List<ResearchReportSection>> sectionsByParent, StringBuilder md, int depth, boolean isCoursework) {
        String indent = "  ".repeat(Math.max(0, depth));
        String prefix;
        if (section.getSectionNumber() == null || section.getSectionNumber().isBlank()) {
            prefix = "";
        } else if (isCoursework && !section.getSectionNumber().contains(".")) {
            prefix = section.getSectionNumber() + ".";
        } else {
            prefix = section.getSectionNumber();
        }
        if (prefix.isBlank()) {
            md.append(indent).append(section.getHeading()).append("\n");
        } else {
            md.append(indent).append(prefix).append(" ").append(section.getHeading()).append("\n");
        }
        List<TableOfContentsSectionItem> children = sectionsByParent.getOrDefault(section.getId(), List.of()).stream()
                .sorted(Comparator.comparingInt(ResearchReportSection::getDisplayOrder))
                .map(child -> tocSectionItem(child, sectionsByParent, md, depth + 1, isCoursework))
                .toList();
        return new TableOfContentsSectionItem(section.getHeading(), section.getSectionNumber(),
                section.getDisplayOrder(), section.getType(), children);
    }

    @Transactional(readOnly = true)
    public ReportValidationResponse validateReport(UUID reportId, User user) {
        ResearchReport report = loadReport(reportId);
        authorizationService.requireProjectViewer(report.getProject().getId(), user);
        List<ValidationIssue> errors = new ArrayList<>();
        List<ValidationIssue> warnings = new ArrayList<>();
        List<ValidationIssue> info = new ArrayList<>();
        UUID projectId = report.getProject().getId();
        AcademicWorkspaceType workspaceType = workspaceType(report.getProject());
        if (workspaceType == AcademicWorkspaceType.ACADEMIC_RESEARCH) {
            if (problemRepository.findAllByProjectId(projectId).isEmpty()) errors.add(new ValidationIssue("ERROR", "MISSING_RESEARCH_PROBLEM", "Research problem is missing.", projectId));
            if (objectiveRepository.findAllByProjectId(projectId).isEmpty()) errors.add(new ValidationIssue("ERROR", "MISSING_OBJECTIVES", "Research objectives are missing.", projectId));
            if (methodologyRepository.findAllByProjectIdOrderByRevisionNumberDesc(projectId).isEmpty()) warnings.add(new ValidationIssue("WARNING", "MISSING_METHODOLOGY", "Methodology has not been created.", projectId));
            if (resultRepository.countByProjectId(projectId) == 0) warnings.add(new ValidationIssue("WARNING", "NO_ANALYSIS_RESULTS", "No analysis results are available.", projectId));
            if (findingRepository.countByProjectId(projectId) == 0) warnings.add(new ValidationIssue("WARNING", "NO_FINDINGS", "No findings have been created from analysis results.", projectId));
            if (conclusionRepository.countByProjectId(projectId) == 0) warnings.add(new ValidationIssue("WARNING", "NO_CONCLUSIONS", "No conclusions are available.", projectId));
            if (recommendationRepository.countByProjectId(projectId) == 0) info.add(new ValidationIssue("INFO", "NO_RECOMMENDATIONS", "No recommendations are available yet.", projectId));
        } else if (workspaceType == AcademicWorkspaceType.ACADEMIC_PROJECT) {
            if (resultRepository.countByProjectId(projectId) == 0 && findingRepository.countByProjectId(projectId) == 0) {
                warnings.add(new ValidationIssue("WARNING", "NO_PROJECT_EVALUATION_RESULTS", "No implementation, testing, or evaluation findings are available yet.", projectId));
            }
        } else {
            info.add(new ValidationIssue("INFO", "COURSEWORK_VALIDATION_SCOPE", "Coursework validation uses the document template and citation checks; research methodology artifacts are not required.", projectId));
        }
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
        prepareFinalDocument(reportId, user);
        report.setStatus(ResearchReportStatus.FINAL);
        report.setRevisionNumber(report.getRevisionNumber() + 1);
        auditService.record(user.getId(), SecurityAuditEventType.REPORT_FINALIZED);
        return ReportResponse.from(report);
    }

    @Transactional
    public FinalDocumentResponse prepareFinalDocument(UUID reportId, User user) {
        ResearchReport report = loadReportForEdit(reportId, user);
        ensureReportStructure(report, user);
        ReportDocumentCompiler.CompiledAcademicDocument compiled = reportDocumentCompiler.compile(report);
        if (compiled.plainText() == null || compiled.plainText().isBlank()) {
            throw new IllegalStateException("REPORT_FINAL_DOCUMENT_EMPTY: compiled final document contains no text.");
        }

        int maxVersion = documentVersionRepository.maxVersionNumber(reportId);
        int nextVersion = maxVersion + 1;

        ReportDocumentVersion docVersion = new ReportDocumentVersion();
        docVersion.setReport(report);
        docVersion.setProject(report.getProject());
        docVersion.setVersionNumber(nextVersion);
        docVersion.setTitle(report.getTitle() + " - Final Draft v" + nextVersion);
        docVersion.setStatus(ReportDocumentVersionStatus.FINAL_REVIEW);
        docVersion.setCitationStyle(report.getCitationStyle());
        docVersion.setContentJson(compiled.contentJson());
        docVersion.setPlainText(compiled.plainText());
        docVersion.setSourceReportRevisionNumber(report.getRevisionNumber());
        docVersion.setSectionRevisionSnapshotJson(compiled.sectionRevisionSnapshotJson());
        docVersion.setReferencesSnapshotJson(compiled.referencesSnapshotJson());
        docVersion.setTemplateSnapshotJson(compiled.templateSnapshotJson());
        docVersion.setCreatedBy(user);
        docVersion.setUpdatedBy(user);

        ReportDocumentVersion saved = documentVersionRepository.save(docVersion);
        auditService.record(user.getId(), SecurityAuditEventType.REPORT_ASSEMBLED);
        return FinalDocumentResponse.from(saved);
    }

    private void appendSectionDocumentParts(ResearchReportSection section, int headingLevel,
            List<ReportRichTextService.DocumentPart> parts, Map<String, Object> sectionRevisions,
            CitationStyle citationStyle, Map<UUID, Integer> referenceNumbers) {
        ensureRichSection(section);
        sectionRevisions.put(section.getId().toString(), section.getRevisionNumber());
        String heading = section.getSectionNumber() != null ? section.getSectionNumber() + " " + section.getHeading() : section.getHeading();
        parts.add(renderedSectionPart(section, heading, Math.min(headingLevel, 4), citationStyle, referenceNumbers));
        for (ResearchReportSection child : sectionRepository.findAllByParentSectionIdOrderByDisplayOrderAsc(section.getId())) {
            appendSectionDocumentParts(child, headingLevel + 1, parts, sectionRevisions, citationStyle, referenceNumbers);
        }
    }

    private ReportRichTextService.DocumentPart renderedSectionPart(ResearchReportSection section, int level,
            CitationStyle citationStyle, Map<UUID, Integer> referenceNumbers) {
        return renderedSectionPart(section, section.getHeading(), level, citationStyle, referenceNumbers);
    }

    private ReportRichTextService.DocumentPart renderedSectionPart(ResearchReportSection section, String heading, int level,
            CitationStyle citationStyle, Map<UUID, Integer> referenceNumbers) {
        String renderedMarkdown = exportText(section, citationStyle, referenceNumbers);
        if (renderedMarkdown.isBlank()) {
            renderedMarkdown = "*Draft content pending for " + heading + ". Use the section editor or AI Draft to write content.*";
        }
        return new ReportRichTextService.DocumentPart(
                heading,
                level,
                richTextService.markdownToDocumentJson(renderedMarkdown),
                richTextService.plainTextFromMarkdown(renderedMarkdown)
        );
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
            String incomingPlainText = request.plainText() != null ? request.plainText() : richTextService.plainTextFromDocumentJson(request.contentJson());
            boolean existingNonEmpty = version.getPlainText() != null && !version.getPlainText().isBlank();
            if (existingNonEmpty && (incomingPlainText == null || incomingPlainText.isBlank())) {
                throw new IllegalArgumentException("REPORT_FINAL_DOCUMENT_EMPTY: refusing to overwrite a non-empty final document with empty editor content.");
            }
            version.setContentJson(request.contentJson());
            version.setPlainText(incomingPlainText);
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
        ChapterTitleParts titleParts = parseChapterTitle(required(request.title(), "Chapter title is required."));
        ReportChapterType type = request.type() == null ? ReportChapterType.CUSTOM : request.type();
        ResearchReportChapter chapter = new ResearchReportChapter();
        chapter.setReport(report);
        chapter.setType(type);
        chapter.setTitle(titleParts.title());
        chapter.setChapterNumber(isNumberedChapterType(type)
                ? request.chapterNumber() != null ? request.chapterNumber() : titleParts.chapterNumber() != null ? titleParts.chapterNumber() : nextChapterNumber(report)
                : null);
        int displayOrder = request.displayOrder() == null ? nextChapterDisplayOrder(report, type) : request.displayOrder();
        shiftChaptersAtOrAfter(report, displayOrder);
        chapter.setDisplayOrder(displayOrder);
        chapter.setRequired(request.required() != null && request.required());
        chapter.setSystemDefined(request.systemDefined() != null && request.systemDefined());
        ResearchReportChapter saved = chapterRepository.save(chapter);
        recalculateSectionNumbers(report);
        report.setRevisionNumber(report.getRevisionNumber() + 1);
        return ChapterResponse.from(saved);
    }

    @Transactional
    public ChapterResponse updateChapter(UUID chapterId, User user, UpdateChapterRequest request) {
        ResearchReportChapter chapter = chapterRepository.findById(chapterId).orElseThrow(() -> new ResourceNotFoundException("Report chapter not found."));
        ResearchReport report = loadReportForEdit(chapter.getReport().getId(), user);
        if (request.type() != null && !chapter.isSystemDefined()) chapter.setType(request.type());
        if (request.title() != null) {
            ChapterTitleParts titleParts = parseChapterTitle(required(request.title(), "Chapter title is required."));
            chapter.setTitle(titleParts.title());
            if (request.chapterNumber() == null && titleParts.chapterNumber() != null && isNumberedChapterType(chapter.getType())) {
                chapter.setChapterNumber(titleParts.chapterNumber());
            }
        }
        if (request.chapterNumber() != null) chapter.setChapterNumber(request.chapterNumber());
        if (request.displayOrder() != null) chapter.setDisplayOrder(request.displayOrder());
        if (request.required() != null) {
            if (chapter.isSystemDefined() && !request.required()) {
                throw new IllegalArgumentException("This chapter is required by the selected report template.");
            }
            chapter.setRequired(request.required());
        }
        if (request.systemDefined() != null && !chapter.isSystemDefined()) chapter.setSystemDefined(request.systemDefined());
        chapterRepository.save(chapter);
        recalculateSectionNumbers(report);
        report.setRevisionNumber(report.getRevisionNumber() + 1);
        return ChapterResponse.from(chapter);
    }

    @Transactional
    public void deleteChapter(UUID chapterId, User user) {
        ResearchReportChapter chapter = chapterRepository.findById(chapterId).orElseThrow(() -> new ResourceNotFoundException("Report chapter not found."));
        ResearchReport report = loadReportForEdit(chapter.getReport().getId(), user);
        if (chapter.isRequired()) {
            throw new IllegalArgumentException("This section is required by the selected report template.");
        }
        List<ResearchReportSection> sections = sectionRepository.findAllByChapterIdOrderByDisplayOrderAsc(chapterId);
        for (ResearchReportSection sec : sections) {
            deleteSectionHierarchy(sec);
        }
        chapterRepository.delete(chapter);
        recalculateSectionNumbers(report);
        report.setRevisionNumber(report.getRevisionNumber() + 1);
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
        UUID parentSectionId = request.parentSectionId();
        if (request.parentSectionId() != null) {
            ResearchReportSection parent = sectionRepository.findById(request.parentSectionId())
                    .orElseThrow(() -> new ResourceNotFoundException("Parent section not found."));
            if (!parent.getChapter().getId().equals(chapterId)) {
                throw new IllegalArgumentException("Parent section belongs to another chapter.");
            }
            section.setParentSection(parent);
        }
        ReportSectionType type = request.type() == null ? ReportSectionType.CUSTOM : request.type();
        section.setType(type);
        section.setHeading(required(request.heading(), "Section heading is required."));
        section.setSemanticPurpose(section.resolveSemanticPurpose());
        section.setGenerationPolicy(section.resolveGenerationPolicy());
        applySectionContent(section, request.content(), request.contentJson(), request.plainText());
        if (request.status() != null) section.setStatus(request.status());
        section.setDisplayOrder(request.displayOrder() == null ? nextSectionDisplayOrder(chapterId, parentSectionId) : request.displayOrder());
        section.setRequired(request.required() != null && request.required());
        section.setSystemDefined(request.systemDefined() != null && request.systemDefined());
        boolean isDeterministic = section.resolveGenerationPolicy() == SectionGenerationPolicy.DETERMINISTIC;
        section.setAiEnabled(!isDeterministic && type != ReportSectionType.REFERENCES && (request.aiEnabled() == null || request.aiEnabled()));
        if (isDeterministic) {
            section.setOrigin(ContentOrigin.DETERMINISTIC_SYSTEM);
        } else {
            section.setOrigin(defaultOrigin(request.origin()));
        }
        section.setSourceArtifactType(optional(request.sourceArtifactType()));
        section.setSourceArtifactId(request.sourceArtifactId());
        section.setSourceRevisionNumber(resolveSourceRevision(section.getSourceArtifactType(), section.getSourceArtifactId()));
        section.setCreatedBy(user);
        if (section.getContentJson() == null) {
            section.setContentJson(richTextService.markdownToDocumentJson(section.getContent()));
            section.setPlainText(richTextService.plainTextFromDocumentJson(section.getContentJson()));
        }
        ResearchReportSection saved = sectionRepository.save(section);
        if (syncInlineCitationUsages(saved, user)) {
            refreshReportReferencesInternal(report, user);
        }
        recalculateSectionNumbers(report);
        report.setRevisionNumber(report.getRevisionNumber() + 1);
        ResearchReportSection refreshed = sectionRepository.findById(saved.getId()).orElse(saved);
        return SectionResponse.from(refreshed);
    }

    @Transactional
    public SectionResponse updateSection(UUID sectionId, User user, UpdateSectionRequest request) {
        ResearchReportSection section = sectionRepository.findById(sectionId).orElseThrow(() -> new ResourceNotFoundException("Report section not found."));
        ResearchReport report = loadReportForEdit(section.getChapter().getReport().getId(), user);
        if (request.parentSectionId() != null) {
            ResearchReportSection parent = sectionRepository.findById(request.parentSectionId())
                    .orElseThrow(() -> new ResourceNotFoundException("Parent section not found."));
            if (!parent.getChapter().getId().equals(section.getChapter().getId())) {
                throw new IllegalArgumentException("Parent section belongs to another chapter.");
            }
            if (parent.getId().equals(section.getId())) {
                throw new IllegalArgumentException("A section cannot be nested under itself.");
            }
            if (isDescendantOf(parent, section.getId())) {
                throw new IllegalArgumentException("A section cannot be moved under one of its own subsections.");
            }
            section.setParentSection(parent);
        }
        if (request.type() != null && !section.isSystemDefined()) section.setType(request.type());
        if (request.heading() != null) {
            section.setHeading(optional(request.heading()));
            section.setSemanticPurpose(section.resolveSemanticPurpose());
            section.setGenerationPolicy(section.resolveGenerationPolicy());
        }
        applySectionContent(section, request.content(), request.contentJson(), request.plainText());
        if (request.status() != null) section.setStatus(request.status());
        if (request.displayOrder() != null) section.setDisplayOrder(request.displayOrder());
        if (request.required() != null) {
            if (section.isSystemDefined() && !request.required()) {
                throw new IllegalArgumentException("This section is required by the selected report template.");
            }
            section.setRequired(request.required());
        }
        if (request.systemDefined() != null && !section.isSystemDefined()) section.setSystemDefined(request.systemDefined());
        boolean isDeterministic = section.resolveGenerationPolicy() == SectionGenerationPolicy.DETERMINISTIC;
        if (isDeterministic) {
            section.setAiEnabled(false);
            if (section.getOrigin() == ContentOrigin.AI_GENERATED) {
                section.setOrigin(ContentOrigin.DETERMINISTIC_SYSTEM);
            }
        } else if (request.aiEnabled() != null) {
            section.setAiEnabled(section.getType() != ReportSectionType.REFERENCES && request.aiEnabled());
        }
        if (request.origin() != null) section.setOrigin(request.origin());
        if (request.sourceOutOfDate() != null) section.setSourceOutOfDate(request.sourceOutOfDate());
        section.setUpdatedBy(user);
        section.setRevisionNumber(section.getRevisionNumber() + 1);
        if (section.getType() == ReportSectionType.REFERENCES) {
            section.setAiEnabled(false);
        }
        if (syncInlineCitationUsages(section, user)) {
            refreshReportReferencesInternal(report, user);
        }
        auditService.record(user.getId(), SecurityAuditEventType.REPORT_SECTION_UPDATED);
        recalculateSectionNumbers(report);
        report.setRevisionNumber(report.getRevisionNumber() + 1);
        return SectionResponse.from(section);
    }

    @Transactional
    public void deleteSection(UUID sectionId, User user) {
        ResearchReportSection section = sectionRepository.findById(sectionId).orElseThrow(() -> new ResourceNotFoundException("Report section not found."));
        ResearchReport report = loadReportForEdit(section.getChapter().getReport().getId(), user);
        if (section.isRequired()) {
            throw new IllegalArgumentException("This section is required by the selected report template.");
        }
        deleteSectionHierarchy(section);
        recalculateSectionNumbers(report);
        report.setRevisionNumber(report.getRevisionNumber() + 1);
    }

    private boolean isDescendantOf(ResearchReportSection candidate, UUID ancestorId) {
        ResearchReportSection current = candidate;
        while (current.getParentSection() != null) {
            UUID parentId = current.getParentSection().getId();
            if (ancestorId.equals(parentId)) {
                return true;
            }
            current = current.getParentSection();
        }
        return false;
    }

    private void deleteSectionHierarchy(ResearchReportSection section) {
        List<ResearchReportSection> children = sectionRepository.findAllByParentSectionIdOrderByDisplayOrderAsc(section.getId());
        for (ResearchReportSection child : children) {
            deleteSectionHierarchy(child);
        }
        citationRepository.deleteAllBySectionId(section.getId());
        sectionRepository.delete(section);
    }

    private boolean syncInlineCitationUsages(ResearchReportSection section, User user) {
        List<UUID> referencedIds = extractInlineCitationReferenceIds(section.getContentJson());
        List<ResearchReportCitation> existing = citationRepository.findAllBySectionIdOrderByCitationOrdinalAsc(section.getId());
        List<ResearchReportCitation> manualExisting = existing.stream()
                .filter(citation -> MANUAL_CITATION_SNAPSHOT.equals(citation.getSupportingTextSnapshot()))
                .toList();

        if (referencedIds.isEmpty()) {
            if (!manualExisting.isEmpty()) {
                citationRepository.deleteAll(manualExisting);
                citationRepository.flush();
                return true;
            }
            return false;
        }

        if (!manualExisting.isEmpty()) {
            citationRepository.deleteAll(manualExisting);
            citationRepository.flush();
        }

        UUID projectId = section.getChapter().getReport().getProject().getId();
        List<ResearchReportCitation> citations = new ArrayList<>();
        int ordinal = 1;
        for (UUID referenceOrProjectReferenceId : referencedIds) {
            ProjectReference projectReference = projectReferenceRepository.findById(referenceOrProjectReferenceId)
                    .or(() -> projectReferenceRepository.findByProjectIdAndReferenceId(projectId, referenceOrProjectReferenceId))
                    .orElse(null);
            if (projectReference == null
                    || projectReference.getStatus() != ProjectReferenceStatus.ACTIVE
                    || !projectReference.isAvailableForCitation()
                    || projectReference.getReference() == null) {
                continue;
            }
            var sourceLink = referenceSourceLinkRepository.findAllByReferenceId(projectReference.getReference().getId()).stream()
                    .filter(link -> link.getDocument() != null)
                    .findFirst()
                    .orElse(null);
            if (sourceLink == null) {
                continue;
            }
            var document = sourceLink.getDocument();
            var version = sourceLink.getDocumentVersion() != null ? sourceLink.getDocumentVersion() : document.getCurrentVersion();
            if (version == null) {
                continue;
            }
            ResearchReportCitation citation = new ResearchReportCitation();
            citation.setSection(section);
            citation.setDocument(document);
            citation.setDocumentVersion(version);
            citation.setDocumentCode(document.getDocumentCode());
            citation.setCitationOrdinal(ordinal++);
            citation.setSupportingTextSnapshot(MANUAL_CITATION_SNAPSHOT);
            citation.setReference(projectReference.getReference());
            citation.setProjectReference(projectReference);
            citations.add(citation);
        }

        if (!citations.isEmpty()) {
            citationRepository.saveAll(citations);
        }
        return !manualExisting.isEmpty() || !citations.isEmpty();
    }

    private List<UUID> extractInlineCitationReferenceIds(String contentJson) {
        if (contentJson == null || contentJson.isBlank()) {
            return List.of();
        }
        try {
            JsonNode root = objectMapper.readTree(contentJson);
            List<UUID> ids = new ArrayList<>();
            collectInlineCitationReferenceIds(root, ids);
            return ids;
        } catch (Exception ignored) {
            return List.of();
        }
    }

    private void collectInlineCitationReferenceIds(JsonNode node, List<UUID> ids) {
        if (node == null || node.isNull()) return;
        if ("citation".equals(node.path("type").asText())) {
            String rawId = node.path("attrs").path("referenceId").asText(null);
            if (rawId != null && !rawId.isBlank()) {
                try {
                    ids.add(UUID.fromString(rawId));
                } catch (IllegalArgumentException ignored) {
                    // Ignore malformed legacy citation node IDs.
                }
            }
        }
        JsonNode content = node.get("content");
        if (content != null && content.isArray()) {
            for (JsonNode child : content) {
                collectInlineCitationReferenceIds(child, ids);
            }
        }
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
                sec.isManuallyEdited(), sec.getRevisionNumber(),
                sec.resolveSemanticPurpose(),
                sec.resolveGenerationPolicy(),
                childResponses
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
                            ResearchReportSection parent = sectionRepository.findById(item.parentId())
                                    .orElseThrow(() -> new ResourceNotFoundException("Parent section not found."));
                            if (!parent.getChapter().getReport().getId().equals(reportId)) {
                                throw new IllegalArgumentException("Parent section belongs to another report.");
                            }
                            if (!parent.getChapter().getId().equals(sec.getChapter().getId())) {
                                throw new IllegalArgumentException("Parent section belongs to another chapter.");
                            }
                            if (parent.getId().equals(sec.getId()) || isDescendantOf(parent, sec.getId())) {
                                throw new IllegalArgumentException("A section cannot be moved under itself or its own subsection.");
                            }
                            sec.setParentSection(parent);
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
        report.setRevisionNumber(report.getRevisionNumber() + 1);
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

    @Transactional
    public SectionResponse refreshTitlePage(UUID reportId, User user) {
        ResearchReport report = loadReportForEdit(reportId, user);
        return SectionResponse.from(refreshTitlePageInternal(report, user));
    }

    @Transactional
    public SectionResponse updateTitlePageDetails(UUID reportId, User user, UpdateTitlePageDetailsRequest request) {
        ResearchReport report = loadReportForEdit(reportId, user);
        if (request.title() != null && !request.title().isBlank()) report.setTitle(request.title().trim());
        if (request.authorName() != null) report.setAuthorName(request.authorName().trim());
        if (request.institutionName() != null) report.setInstitutionName(request.institutionName().trim());
        if (request.departmentName() != null) report.setDepartmentName(request.departmentName().trim());
        if (request.degreeProgram() != null) report.setDegreeProgram(request.degreeProgram().trim());
        if (request.supervisorName() != null) report.setSupervisorName(request.supervisorName().trim());
        if (request.submissionYear() != null) report.setSubmissionYear(request.submissionYear());
        reportRepository.save(report);

        ResearchReportSection titleSection = findOrCreateSectionByPurpose(report, SectionSemanticPurpose.TITLE_PAGE, "Title Page", user);
        TitlePageRenderer.TitlePageData data = new TitlePageRenderer.TitlePageData(
                report.getTitle(),
                report.getAuthorName() != null ? report.getAuthorName() : (report.getProject().getCreatedBy() != null ? report.getProject().getCreatedBy().getFullName() : "Student Name"),
                request.studentId(),
                report.getInstitutionName(),
                report.getDepartmentName(),
                report.getDegreeProgram(),
                report.getSupervisorName(),
                request.academicYear() != null ? request.academicYear() : (report.getProject().getAcademicYear()),
                report.getSubmissionYear()
        );
        String md = titlePageRenderer.renderMarkdown(data);
        titleSection.setContent(md);
        titleSection.setContentJson(richTextService.markdownToDocumentJson(md));
        titleSection.setPlainText(richTextService.plainTextFromDocumentJson(titleSection.getContentJson()));
        titleSection.setOrigin(ContentOrigin.DETERMINISTIC_SYSTEM);
        titleSection.setStatus(ReportSectionStatus.ACCEPTED);
        titleSection.setAiEnabled(false);
        titleSection.setUpdatedBy(user);
        titleSection.setRevisionNumber(titleSection.getRevisionNumber() + 1);
        citationRepository.deleteAllBySectionId(titleSection.getId());
        return SectionResponse.from(sectionRepository.save(titleSection));
    }

    @Transactional
    public SectionResponse refreshTableOfContents(UUID reportId, User user) {
        ResearchReport report = loadReportForEdit(reportId, user);
        return SectionResponse.from(refreshTableOfContentsInternal(report, user));
    }

    @Transactional
    public SectionResponse refreshListOfFigures(UUID reportId, User user) {
        ResearchReport report = loadReportForEdit(reportId, user);
        return SectionResponse.from(refreshListOfFiguresInternal(report, user));
    }

    @Transactional
    public SectionResponse refreshListOfTables(UUID reportId, User user) {
        ResearchReport report = loadReportForEdit(reportId, user);
        return SectionResponse.from(refreshListOfTablesInternal(report, user));
    }

    @Transactional
    public DeterministicRepairResponse repairDeterministicNodes(UUID reportId, User user, boolean forceReset) {
        ResearchReport report = loadReportForEdit(reportId, user);
        List<ResearchReportSection> allSections = sectionRepository.findAllByChapterReportId(reportId);
        List<String> repaired = new ArrayList<>();

        for (ResearchReportSection sec : allSections) {
            SectionSemanticPurpose purpose = sec.resolveSemanticPurpose();
            SectionGenerationPolicy policy = sec.resolveGenerationPolicy();
            boolean isDeterministic = policy == SectionGenerationPolicy.DETERMINISTIC;
            boolean corrupted = looksLikeGeneratedLiteratureProse(sec.getContent());
            boolean wasAi = sec.getOrigin() == ContentOrigin.AI_GENERATED;

            if (isDeterministic || purpose == SectionSemanticPurpose.DECLARATION || purpose == SectionSemanticPurpose.CERTIFICATION) {
                if (forceReset || corrupted || (wasAi && !sec.isManuallyEdited())) {
                    switch (purpose) {
                        case TITLE_PAGE -> {
                            refreshTitlePageInternal(report, user);
                            repaired.add("Title Page");
                        }
                        case TABLE_OF_CONTENTS -> {
                            refreshTableOfContentsInternal(report, user);
                            repaired.add("Table of Contents");
                        }
                        case LIST_OF_FIGURES -> {
                            refreshListOfFiguresInternal(report, user);
                            repaired.add("List of Figures");
                        }
                        case LIST_OF_TABLES -> {
                            refreshListOfTablesInternal(report, user);
                            repaired.add("List of Tables");
                        }
                        case REFERENCES -> {
                            refreshReportReferencesInternal(report, user);
                            repaired.add("References");
                        }
                        case DECLARATION -> {
                            renderDeclarationInternal(report, sec, user);
                            repaired.add("Declaration");
                        }
                        case CERTIFICATION -> {
                            renderCertificationInternal(report, sec, user);
                            repaired.add("Certification");
                        }
                        default -> {}
                    }
                } else {
                    sec.setAiEnabled(false);
                    sec.setSemanticPurpose(purpose);
                    sec.setGenerationPolicy(policy);
                    if (isDeterministic && sec.getOrigin() == ContentOrigin.AI_GENERATED) {
                        sec.setOrigin(ContentOrigin.DETERMINISTIC_SYSTEM);
                    }
                    sectionRepository.save(sec);
                }
            }
        }
        return new DeterministicRepairResponse(
                repaired.size(),
                repaired,
                "Repaired " + repaired.size() + " deterministic report sections."
        );
    }

    public GeneratedDraftResponse generateSectionForReport(UUID reportId, User user, GenerateSectionRequest request) {
        ResearchReport report = loadReportForEdit(reportId, user);
        ResearchReportSection targetSection = null;
        if (request != null && request.targetNodeId() != null) {
            targetSection = sectionRepository.findById(request.targetNodeId()).orElse(null);
        }
        if (targetSection == null && request != null && request.targetNodeTitle() != null && !request.targetNodeTitle().isBlank()) {
            String normTarget = normalizeTitle(request.targetNodeTitle());
            List<ResearchReportSection> allSections = sectionRepository.findAllByChapterReportId(reportId);
            for (ResearchReportSection sec : allSections) {
                if (normalizeTitle(sec.getHeading()).equals(normTarget)) {
                    targetSection = sec;
                    break;
                }
            }
        }
        if (targetSection == null) {
            List<ResearchReportChapter> chapters = chapterRepository.findAllByReportIdOrderByDisplayOrderAsc(reportId);
            ResearchReportChapter bodyChapter = chapters.stream()
                    .filter(c -> c.getType() != ReportChapterType.PRELIMINARY && c.getType() != ReportChapterType.REFERENCES && c.getType() != ReportChapterType.APPENDICES)
                    .findFirst()
                    .orElse(chapters.getFirst());
            ResearchReportSection newSec = new ResearchReportSection();
            newSec.setChapter(bodyChapter);
            newSec.setType(ReportSectionType.CUSTOM);
            newSec.setHeading(request != null && request.targetNodeTitle() != null && !request.targetNodeTitle().isBlank() ? request.targetNodeTitle() : "Custom Section");
            newSec.setDisplayOrder((int) sectionRepository.countByChapterIdAndParentSectionIsNull(bodyChapter.getId()) + 1);
            newSec.setCreatedBy(user);
            newSec.setOrigin(ContentOrigin.USER);
            newSec.setAiEnabled(true);
            newSec.setSemanticPurpose(SectionSemanticPurpose.CUSTOM);
            newSec.setGenerationPolicy(SectionGenerationPolicy.CONTEXTUAL_AI);
            targetSection = sectionRepository.save(newSec);
            recalculateSectionNumbers(report);
        }
        return generateSection(targetSection.getId(), user, request);
    }

    public GeneratedDraftResponse generateSection(UUID sectionId, User user, GenerateSectionRequest request) {
        ResearchReportSection section = sectionRepository.findWithChapterReportProjectById(sectionId).orElseThrow(() -> new ResourceNotFoundException("Report section not found."));
        UUID projectId = section.getChapter().getReport().getProject().getId();
        authorizationService.requireProjectEditor(projectId, user);

        if (request != null && request.targetNodeId() != null && !request.targetNodeId().equals(section.getId())) {
            throw new IllegalArgumentException("Target node ID does not match the requested section.");
        }

        SectionGenerationPolicy policy = section.resolveGenerationPolicy();
        SectionSemanticPurpose purpose = section.resolveSemanticPurpose();
        ResearchReport report = section.getChapter().getReport();

        // 1. Deterministic System Nodes: ZERO AI calls, ZERO credits
        if (policy == SectionGenerationPolicy.DETERMINISTIC) {
            ResearchReportSection refreshed;
            String safetyPolicy;
            switch (purpose) {
                case TITLE_PAGE -> {
                    refreshed = refreshTitlePageInternal(report, user);
                    safetyPolicy = "DETERMINISTIC_TITLE_PAGE";
                }
                case TABLE_OF_CONTENTS -> {
                    refreshed = refreshTableOfContentsInternal(report, user);
                    safetyPolicy = "DETERMINISTIC_TOC";
                }
                case LIST_OF_FIGURES -> {
                    refreshed = refreshListOfFiguresInternal(report, user);
                    safetyPolicy = "DETERMINISTIC_LIST_OF_FIGURES";
                }
                case LIST_OF_TABLES -> {
                    refreshed = refreshListOfTablesInternal(report, user);
                    safetyPolicy = "DETERMINISTIC_LIST_OF_TABLES";
                }
                case REFERENCES -> {
                    refreshed = refreshReportReferencesInternal(report, user);
                    safetyPolicy = "DETERMINISTIC_BIBLIOGRAPHY";
                }
                case DECLARATION -> {
                    refreshed = renderDeclarationInternal(report, section, user);
                    safetyPolicy = "DETERMINISTIC_DECLARATION";
                }
                case CERTIFICATION -> {
                    refreshed = renderCertificationInternal(report, section, user);
                    safetyPolicy = "DETERMINISTIC_CERTIFICATION";
                }
                default -> throw new IllegalArgumentException("AI generation is disabled for deterministic section: " + section.getHeading());
            }
            return new GeneratedDraftResponse(
                    refreshed.getContent(),
                    List.of(),
                    safetyPolicy,
                    List.of(),
                    null,
                    refreshed.getUpdatedAt()
            );
        }

        ReportSectionPromptBuilder.ProjectContextData pContext = loadProjectContextData(report.getProject());
        AcademicSectionGenerationService.AcademicSectionGenerationContext genContext = new AcademicSectionGenerationService.AcademicSectionGenerationContext(
                report.getProject().getWorkspace() != null ? report.getProject().getWorkspace().getId() : null,
                report.getProject().getWorkspaceType(),
                projectId,
                report.getProject().getAcademicProjectType(),
                report.getId(),
                section.getId(),
                section.getType(),
                purpose,
                policy,
                section.getHeading(),
                section.getParentSection() != null ? section.getParentSection().getHeading() : null,
                section.getChapter() != null ? section.getChapter().getTitle() : null,
                report.getProject().getTitle(),
                report.getProject().getDescription(),
                report.getProject().getResearchAim(),
                pContext.objectives(),
                pContext.questions(),
                pContext.problemStatement(),
                report.getProject().getStudyArea(),
                report.getProject().getResearchType(),
                pContext.methodologySummary(),
                pContext.findingsCount(),
                pContext.analysisRunsCount(),
                section.getTemplateGuidance(),
                request != null && request.documentIds() != null && !request.documentIds().isEmpty() ? "SELECTED_DOCUMENTS" : "ALL_PROJECT_DOCUMENTS",
                request != null && request.documentIds() != null ? new LinkedHashSet<>(request.documentIds()) : Set.of(),
                request != null ? request.instructions() : null,
                request != null ? request.evidenceLimit() : null,
                request != null ? request.applyMode() : "REPLACE"
        );
        return academicSectionGenerationService.generate(genContext, user);
    }

    private void logReportGeneration(
            ResearchReportSection section,
            SectionSemanticPurpose purpose,
            SectionGenerationPolicy policy,
            GroundedAnswerResponse answer
    ) {
        var summary = answer != null ? answer.retrievalSummary() : null;
        String strategy = summary != null && summary.generationStrategy() != null
                ? summary.generationStrategy()
                : purpose == SectionSemanticPurpose.LITERATURE_REVIEW ? "MULTI_SOURCE_LITERATURE_SYNTHESIS" : "DIRECT_RAG";
        log.info(
                "AI report section generation resolved: sectionId={} sectionType={} semanticPurpose={} sectionHeading={} chapterTitle={} resolvedGenerationStrategy={} generationPolicy={} retrievalStrategy={} documentCount={} evidenceCount={} provider={} model={}",
                section.getId(),
                section.getType(),
                purpose,
                section.getHeading(),
                section.getChapter() != null ? section.getChapter().getTitle() : null,
                strategy,
                policy,
                summary != null ? summary.retrievalMode() : null,
                summary != null ? summary.documentCount() : 0,
                summary != null ? summary.evidenceCount() : 0,
                "RAG_GROUNDED_GENERATOR",
                summary != null ? summary.configuredModel() : null
        );
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
        section.setSemanticPurpose(section.resolveSemanticPurpose());
        section.setGenerationPolicy(section.resolveGenerationPolicy());
        boolean isDeterministic = section.resolveGenerationPolicy() == SectionGenerationPolicy.DETERMINISTIC;
        section.setAiEnabled(!isDeterministic);
        if (isDeterministic) {
            section.setOrigin(ContentOrigin.DETERMINISTIC_SYSTEM);
        }
        sectionRepository.save(section);
    }

    private GeneratedDraftResponse persistGeneratedSectionAppend(UUID sectionId, GroundedAnswerResponse answer, User user) {
        return transactionTemplate.execute(status -> {
            ResearchReportSection managedSection = sectionRepository.findByIdForUpdate(sectionId)
                    .orElseThrow(() -> new ResourceNotFoundException("Report section not found."));
            String normalizedMarkdown = markdownRenderer.normalizeSectionMarkdown(managedSection.getHeading(), answer.answer());
            String existing = managedSection.getContent() != null ? managedSection.getContent().trim() : "";
            String merged = existing.isEmpty() ? normalizedMarkdown : existing + "\n\n" + normalizedMarkdown;
            managedSection.setContent(merged);
            managedSection.setContentJson(richTextService.markdownToDocumentJson(merged));
            managedSection.setPlainText(richTextService.plainTextFromDocumentJson(managedSection.getContentJson()));
            managedSection.setStatus(ReportSectionStatus.DRAFT);
            managedSection.setOrigin(ContentOrigin.AI_GENERATED);
            managedSection.setUpdatedBy(user);
            managedSection.setSourceOutOfDate(false);
            managedSection.setRevisionNumber(managedSection.getRevisionNumber() + 1);
            sectionRepository.save(managedSection);
            if (managedSection.resolveGenerationPolicy() == SectionGenerationPolicy.SOURCE_GROUNDED_AI) {
                persistSectionCitations(managedSection, answer, user);
            }
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

    private ResearchReportSection refreshTitlePageInternal(ResearchReport report, User user) {
        ResearchReportSection titleSection = findOrCreateSectionByPurpose(report, SectionSemanticPurpose.TITLE_PAGE, "Title Page", user);
        TitlePageRenderer.TitlePageData data = titlePageRenderer.resolveData(report);
        String md = titlePageRenderer.renderMarkdown(data);
        titleSection.setContent(md);
        titleSection.setContentJson(richTextService.markdownToDocumentJson(md));
        titleSection.setPlainText(richTextService.plainTextFromDocumentJson(titleSection.getContentJson()));
        titleSection.setOrigin(ContentOrigin.DETERMINISTIC_SYSTEM);
        titleSection.setStatus(ReportSectionStatus.ACCEPTED);
        titleSection.setAiEnabled(false);
        titleSection.setUpdatedBy(user);
        titleSection.setRevisionNumber(titleSection.getRevisionNumber() + 1);
        citationRepository.deleteAllBySectionId(titleSection.getId());
        return sectionRepository.save(titleSection);
    }

    private ResearchReportSection refreshTableOfContentsInternal(ResearchReport report, User user) {
        ResearchReportSection tocSection = findOrCreateSectionByPurpose(report, SectionSemanticPurpose.TABLE_OF_CONTENTS, "Table of Contents", user);
        recalculateSectionNumbers(report);
        List<ResearchReportChapter> chapters = chapterRepository.findAllByReportIdOrderByDisplayOrderAsc(report.getId());
        Map<UUID, List<ResearchReportSection>> sectionsByParent = new LinkedHashMap<>();
        List<ResearchReportSection> allSections = sectionRepository.findAllByChapterReportId(report.getId());
        for (ResearchReportSection s : allSections) {
            UUID pId = s.getParentSection() != null ? s.getParentSection().getId() : null;
            sectionsByParent.computeIfAbsent(pId, k -> new ArrayList<>()).add(s);
        }

        StringBuilder sb = new StringBuilder();
        sb.append("# TABLE OF CONTENTS\n\n");
        for (ResearchReportChapter chapter : chapters) {
            sb.append("### ").append(chapter.getTitle()).append("\n\n");
            List<ResearchReportSection> rootSections = allSections.stream()
                    .filter(s -> s.getChapter().getId().equals(chapter.getId()) && s.getParentSection() == null)
                    .sorted(Comparator.comparingInt(ResearchReportSection::getDisplayOrder))
                    .toList();
            for (ResearchReportSection root : rootSections) {
                appendTocSection(sb, root, sectionsByParent, 0);
            }
            sb.append("\n");
        }
        String md = sb.toString().trim();
        tocSection.setContent(md);
        tocSection.setContentJson(richTextService.markdownToDocumentJson(md));
        tocSection.setPlainText(richTextService.plainTextFromDocumentJson(tocSection.getContentJson()));
        tocSection.setOrigin(ContentOrigin.DETERMINISTIC_SYSTEM);
        tocSection.setStatus(ReportSectionStatus.ACCEPTED);
        tocSection.setAiEnabled(false);
        tocSection.setUpdatedBy(user);
        tocSection.setRevisionNumber(tocSection.getRevisionNumber() + 1);
        citationRepository.deleteAllBySectionId(tocSection.getId());
        return sectionRepository.save(tocSection);
    }

    private void appendTocSection(StringBuilder sb, ResearchReportSection section, Map<UUID, List<ResearchReportSection>> sectionsByParent, int depth) {
        String indent = "  ".repeat(depth);
        String num = section.getSectionNumber() != null && !section.getSectionNumber().isBlank()
                ? section.getSectionNumber() + " "
                : "";
        sb.append(indent).append("- ").append(num).append(section.getHeading()).append("\n");
        List<ResearchReportSection> children = sectionsByParent.getOrDefault(section.getId(), List.of()).stream()
                .sorted(Comparator.comparingInt(ResearchReportSection::getDisplayOrder))
                .toList();
        for (ResearchReportSection child : children) {
            appendTocSection(sb, child, sectionsByParent, depth + 1);
        }
    }

    private ResearchReportSection refreshListOfFiguresInternal(ResearchReport report, User user) {
        ResearchReportSection lofSection = findOrCreateSectionByPurpose(report, SectionSemanticPurpose.LIST_OF_FIGURES, "List of Figures", user);
        List<ResearchReportSection> allSections = sectionRepository.findAllByChapterReportId(report.getId());
        List<DocumentStructureExtractors.FigureEntry> figures = structureExtractors.extractFigures(allSections);
        String md = structureExtractors.renderFiguresMarkdown(figures);
        lofSection.setContent(md);
        lofSection.setContentJson(richTextService.markdownToDocumentJson(md));
        lofSection.setPlainText(richTextService.plainTextFromDocumentJson(lofSection.getContentJson()));
        lofSection.setOrigin(ContentOrigin.DETERMINISTIC_SYSTEM);
        lofSection.setStatus(ReportSectionStatus.ACCEPTED);
        lofSection.setAiEnabled(false);
        lofSection.setUpdatedBy(user);
        lofSection.setRevisionNumber(lofSection.getRevisionNumber() + 1);
        citationRepository.deleteAllBySectionId(lofSection.getId());
        return sectionRepository.save(lofSection);
    }

    private ResearchReportSection refreshListOfTablesInternal(ResearchReport report, User user) {
        ResearchReportSection lotSection = findOrCreateSectionByPurpose(report, SectionSemanticPurpose.LIST_OF_TABLES, "List of Tables", user);
        List<ResearchReportSection> allSections = sectionRepository.findAllByChapterReportId(report.getId());
        List<DocumentStructureExtractors.TableEntry> tables = structureExtractors.extractTables(allSections);
        String md = structureExtractors.renderTablesMarkdown(tables);
        lotSection.setContent(md);
        lotSection.setContentJson(richTextService.markdownToDocumentJson(md));
        lotSection.setPlainText(richTextService.plainTextFromDocumentJson(lotSection.getContentJson()));
        lotSection.setOrigin(ContentOrigin.DETERMINISTIC_SYSTEM);
        lotSection.setStatus(ReportSectionStatus.ACCEPTED);
        lotSection.setAiEnabled(false);
        lotSection.setUpdatedBy(user);
        lotSection.setRevisionNumber(lotSection.getRevisionNumber() + 1);
        citationRepository.deleteAllBySectionId(lotSection.getId());
        return sectionRepository.save(lotSection);
    }

    private ResearchReportSection renderDeclarationInternal(ResearchReport report, ResearchReportSection section, User user) {
        String md = frontMatterRenderer.renderDeclaration(report);
        section.setContent(md);
        section.setContentJson(richTextService.markdownToDocumentJson(md));
        section.setPlainText(richTextService.plainTextFromDocumentJson(section.getContentJson()));
        section.setOrigin(ContentOrigin.TEMPLATE);
        section.setStatus(ReportSectionStatus.ACCEPTED);
        section.setAiEnabled(false);
        section.setUpdatedBy(user);
        section.setRevisionNumber(section.getRevisionNumber() + 1);
        citationRepository.deleteAllBySectionId(section.getId());
        return sectionRepository.save(section);
    }

    private ResearchReportSection renderCertificationInternal(ResearchReport report, ResearchReportSection section, User user) {
        String md = frontMatterRenderer.renderCertification(report);
        section.setContent(md);
        section.setContentJson(richTextService.markdownToDocumentJson(md));
        section.setPlainText(richTextService.plainTextFromDocumentJson(section.getContentJson()));
        section.setOrigin(ContentOrigin.TEMPLATE);
        section.setStatus(ReportSectionStatus.ACCEPTED);
        section.setAiEnabled(false);
        section.setUpdatedBy(user);
        section.setRevisionNumber(section.getRevisionNumber() + 1);
        citationRepository.deleteAllBySectionId(section.getId());
        return sectionRepository.save(section);
    }

    private ResearchReportSection findOrCreateSectionByPurpose(ResearchReport report, SectionSemanticPurpose purpose, String defaultHeading, User user) {
        List<ResearchReportSection> allSections = sectionRepository.findAllByChapterReportId(report.getId());
        for (ResearchReportSection s : allSections) {
            if (s.resolveSemanticPurpose() == purpose) {
                return s;
            }
        }
        for (ResearchReportSection s : allSections) {
            if (normalizeTitle(s.getHeading()).equals(normalizeTitle(defaultHeading))) {
                s.setSemanticPurpose(purpose);
                s.setGenerationPolicy(purpose.defaultPolicy());
                return sectionRepository.save(s);
            }
        }
        List<ResearchReportChapter> chapters = chapterRepository.findAllByReportIdOrderByDisplayOrderAsc(report.getId());
        ResearchReportChapter prelimChapter = chapters.stream()
                .filter(ch -> ch.getType() == ReportChapterType.PRELIMINARY)
                .findFirst()
                .orElseGet(() -> {
                    ResearchReportChapter created = new ResearchReportChapter();
                    created.setReport(report);
                    created.setType(ReportChapterType.PRELIMINARY);
                    created.setTitle("Preliminary Pages");
                    created.setDisplayOrder(1);
                    created.setRequired(true);
                    created.setSystemDefined(true);
                    return chapterRepository.save(created);
                });

        ResearchReportSection created = new ResearchReportSection();
        created.setChapter(prelimChapter);
        created.setType(purpose.defaultSectionType());
        created.setHeading(defaultHeading);
        created.setSemanticPurpose(purpose);
        created.setGenerationPolicy(purpose.defaultPolicy());
        created.setOrigin(ContentOrigin.DETERMINISTIC_SYSTEM);
        created.setAiEnabled(purpose.defaultPolicy() != SectionGenerationPolicy.DETERMINISTIC);
        created.setRequired(true);
        created.setSystemDefined(true);
        created.setDisplayOrder((int) sectionRepository.countByChapterIdAndParentSectionIsNull(prelimChapter.getId()) + 1);
        created.setCreatedBy(user);
        return sectionRepository.save(created);
    }

    public static boolean looksLikeGeneratedLiteratureProse(String content) {
        if (content == null || content.isBlank()) return false;
        String lower = content.toLowerCase(Locale.ROOT);
        return lower.contains("overview and thematic context")
                || lower.contains("synthesis of grounded empirical literature")
                || lower.contains("synthesis of the grounded empirical literature")
                || lower.contains("this section examines the title page")
                || lower.contains("this section examines the table of contents")
                || lower.contains("this section examines the list of figures")
                || lower.contains("this section examines the list of tables")
                || lower.contains("this section examines the references");
    }

    private void initializeDeterministicContent(ResearchReport report, ResearchReportSection section, User user) {
        SectionSemanticPurpose purpose = section.resolveSemanticPurpose();
        switch (purpose) {
            case TITLE_PAGE -> {
                TitlePageRenderer.TitlePageData data = titlePageRenderer.resolveData(report);
                String md = titlePageRenderer.renderMarkdown(data);
                setSectionProse(section, md, ContentOrigin.DETERMINISTIC_SYSTEM, ReportSectionStatus.ACCEPTED);
            }
            case TABLE_OF_CONTENTS -> {
                String md = "# TABLE OF CONTENTS\n\n*Refresh table of contents to update hierarchy.*";
                setSectionProse(section, md, ContentOrigin.DETERMINISTIC_SYSTEM, ReportSectionStatus.ACCEPTED);
            }
            case LIST_OF_FIGURES -> {
                String md = "# LIST OF FIGURES\n\n*No figures have been registered in this document yet.*";
                setSectionProse(section, md, ContentOrigin.DETERMINISTIC_SYSTEM, ReportSectionStatus.ACCEPTED);
            }
            case LIST_OF_TABLES -> {
                String md = "# LIST OF TABLES\n\n*No tables have been registered in this document yet.*";
                setSectionProse(section, md, ContentOrigin.DETERMINISTIC_SYSTEM, ReportSectionStatus.ACCEPTED);
            }
            case REFERENCES -> {
                refreshReportReferencesInternal(report, user);
            }
            default -> {}
        }
    }

    private void initializeDeclarationContent(ResearchReport report, ResearchReportSection section) {
        String md = frontMatterRenderer.renderDeclaration(report);
        setSectionProse(section, md, ContentOrigin.TEMPLATE, ReportSectionStatus.DRAFT);
    }

    private void initializeCertificationContent(ResearchReport report, ResearchReportSection section) {
        String md = frontMatterRenderer.renderCertification(report);
        setSectionProse(section, md, ContentOrigin.TEMPLATE, ReportSectionStatus.DRAFT);
    }

    private void setSectionProse(ResearchReportSection section, String markdown, ContentOrigin origin, ReportSectionStatus status) {
        section.setContent(markdown);
        section.setContentJson(richTextService.markdownToDocumentJson(markdown));
        section.setPlainText(richTextService.plainTextFromDocumentJson(section.getContentJson()));
        section.setOrigin(origin);
        section.setStatus(status);
        section.setAiEnabled(false);
    }

    private ReportSectionPromptBuilder.ProjectContextData loadProjectContextData(ResearchProject project) {
        UUID projectId = project.getId();
        List<ResearchObjective> objectives = objectiveRepository.findAllByProjectId(projectId);
        List<ResearchQuestion> questions = questionRepository.findAllByProjectId(projectId);
        List<String> objTexts = objectives.stream().map(ResearchObjective::getText).toList();
        List<String> qTexts = questions.stream().map(ResearchQuestion::getText).toList();
        int findingsCount = (int) findingRepository.countByProjectId(projectId);
        int runsCount = (int) runRepository.countByProjectId(projectId);

        String problem = problemRepository.findAllByProjectId(projectId).stream()
                .map(ResearchProblem::getStatement)
                .filter(Objects::nonNull)
                .filter(statement -> !statement.isBlank())
                .findFirst()
                .orElse(null);
        String methodology = methodologyRepository.findByProjectIdAndStatus(
                        projectId,
                        com.researchassistant.methodology.entity.MethodologyStatus.ACTIVE
                )
                .or(() -> methodologyRepository.findAllByProjectIdOrderByRevisionNumberDesc(projectId).stream().findFirst())
                .map(com.researchassistant.methodology.entity.Methodology::getDesignDescription)
                .filter(description -> description != null && !description.isBlank())
                .orElse(null);

        return new ReportSectionPromptBuilder.ProjectContextData(
                project.getTitle(),
                project.getWorkspaceType() != null ? project.getWorkspaceType() : AcademicWorkspaceType.ACADEMIC_RESEARCH,
                project.getAcademicProjectType() != null ? project.getAcademicProjectType() : AcademicProjectType.GENERAL_ACADEMIC_PROJECT,
                project.getResearchAim(),
                objTexts,
                qTexts,
                problem != null ? problem : project.getDescription(),
                methodology != null ? methodology : project.getResearchType(),
                findingsCount,
                runsCount
        );
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
            if (managedSection.resolveGenerationPolicy() == SectionGenerationPolicy.SOURCE_GROUNDED_AI) {
                persistSectionCitations(managedSection, answer, user);
            }
            String headingLower = managedSection.getHeading() != null ? managedSection.getHeading().toLowerCase(Locale.ROOT) : "";
            boolean isLiteratureReview = managedSection.getType() == ReportSectionType.LITERATURE_REVIEW
                    || headingLower.contains("literature review")
                    || headingLower.contains("related systems")
                    || headingLower.contains("previous studies")
                    || headingLower.contains("conceptual review");
            if (isLiteratureReview) {
                cleanLiteratureReviewAndExtractMatrix(managedSection.getChapter().getReport(), user);
                managedSection = sectionRepository.findByIdForUpdate(sectionId)
                        .orElseThrow(() -> new ResourceNotFoundException("Report section not found."));
            }
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

                    section.setContent(prosePart.isBlank() ? null : prosePart);
                    section.setContentJson(richTextService.markdownToDocumentJson(prosePart));
                    section.setPlainText(richTextService.plainTextFromDocumentJson(section.getContentJson()));
                    section.setStatus(section.getPlainText() == null || section.getPlainText().isBlank()
                            ? ReportSectionStatus.NOT_STARTED
                            : section.getStatus() == ReportSectionStatus.NOT_STARTED ? ReportSectionStatus.DRAFT : section.getStatus());
                    sectionRepository.save(section);
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

        if (refSection.getContent() == null || refSection.getContent().isBlank() || looksLikeGeneratedReferenceProse(refSection.getContent())) {
            refreshReportReferencesInternal(report, user);
        }
    }

    private boolean looksLikeGeneratedReferenceProse(String content) {
        if (content == null) return false;
        String normalized = content.toLowerCase(Locale.ROOT);
        return normalized.contains("the supplied evidence presents")
                || normalized.contains("cross-study synthesis")
                || normalized.contains("digital agriculture as a broad")
                || normalized.contains("interdisciplinary field")
                || normalized.contains("methodological patterns")
                || normalized.contains("research gaps");
    }

    private Map<UUID, ReferenceEntry> citedReferences(ResearchReport report) {
        Map<UUID, ReferenceEntry> references = new LinkedHashMap<>();
        for (ResearchReportCitation citation : citationRepository.findAllBySectionChapterReportIdOrderByCitationOrdinalAsc(report.getId())) {
            ReferenceEntry reference = citation.getReference();
            if (reference == null && citation.getProjectReference() != null) {
                reference = citation.getProjectReference().getReference();
            }
            if (reference != null) {
                references.putIfAbsent(reference.getId(), reference);
            }
        }
        if (report.isIncludeUncitedReferences()) {
            for (ProjectReference pr : projectReferenceRepository.findAllByProjectId(report.getProject().getId())) {
                if (pr.getStatus() == ProjectReferenceStatus.ACTIVE && pr.isAvailableForCitation() && pr.getReference() != null) {
                    references.putIfAbsent(pr.getReference().getId(), pr.getReference());
                }
            }
        }
        return references;
    }

    private Map<UUID, Integer> citationNumberMap(ResearchReport report) {
        Map<UUID, Integer> numbers = new LinkedHashMap<>();
        int next = 1;
        for (ReferenceEntry reference : citedReferences(report).values()) {
            numbers.putIfAbsent(reference.getId(), next++);
        }
        return numbers;
    }

    private String resolveSectionRawText(ResearchReportSection section) {
        if (section.getContent() != null && !section.getContent().isBlank()) {
            return section.getContent();
        }
        if (section.getContentJson() != null && !section.getContentJson().isBlank() && richTextService.isValidDocumentJson(section.getContentJson())) {
            String md = richTextService.documentJsonToMarkdown(section.getContentJson());
            if (md != null && !md.isBlank()) {
                return md;
            }
        }
        if (section.getPlainText() != null && !section.getPlainText().isBlank()) {
            return section.getPlainText();
        }
        return "";
    }

    private String exportText(ResearchReportSection section, CitationStyle style, Map<UUID, Integer> referenceNumbers) {
        String text = resolveSectionRawText(section);
        text = text.replace("[citation metadata incomplete]", "")
                .replace("[Citation metadata incomplete]", "")
                .replace("REFERENCE_METADATA_INCOMPLETE", "");
        List<ResearchReportCitation> citations = citationRepository.findAllBySectionIdOrderByCitationOrdinalAsc(section.getId());
        for (ResearchReportCitation citation : citations) {
            String display = displayCitation(citation, style, referenceNumbers);
            if (display == null || display.isBlank()) display = "";
            int ordinal = citation.getCitationOrdinal();
            text = text.replaceAll("\\[E" + ordinal + "]", Matcher.quoteReplacement(display));
            text = text.replaceAll("\\bE" + ordinal + "\\b", Matcher.quoteReplacement(display));
            if (citation.getDocumentCode() != null) {
                text = text.replaceAll("\\[?" + Pattern.quote(citation.getDocumentCode()) + "(?:\\s*,\\s*p\\.?\\s*\\d+)?]?", Matcher.quoteReplacement(display));
            }
        }
        text = replaceInlineCitationTokens(text, section, style, referenceNumbers);
        text = text.replaceAll("\\[?E\\d+]?\\b", "");
        text = text.replaceAll("(?i)\\[?DOC-\\d{3,}(?:\\s*,\\s*p\\.?\\s*\\d+)?]?", "");
        return text.replaceAll("[ \\t]{2,}", " ").trim();
    }

    private String replaceInlineCitationTokens(String text, ResearchReportSection section, CitationStyle style, Map<UUID, Integer> referenceNumbers) {
        if (text == null || text.isBlank() || !text.contains("[[citation:")) {
            return text;
        }
        Pattern tokenPattern = Pattern.compile("\\[\\[citation:([a-fA-F0-9-]{36})(?::[^\\]]+)?]]");
        Matcher matcher = tokenPattern.matcher(text);
        StringBuffer out = new StringBuffer();
        while (matcher.find()) {
            UUID referenceOrProjectReferenceId;
            try {
                referenceOrProjectReferenceId = UUID.fromString(matcher.group(1));
            } catch (IllegalArgumentException ignored) {
                matcher.appendReplacement(out, "");
                continue;
            }
            UUID projectId = section.getChapter().getReport().getProject().getId();
            ProjectReference projectReference = projectReferenceRepository.findById(referenceOrProjectReferenceId)
                    .or(() -> projectReferenceRepository.findByProjectIdAndReferenceId(projectId, referenceOrProjectReferenceId))
                    .orElse(null);
            String replacement = "";
            if (projectReference != null && projectReference.getReference() != null) {
                replacement = displayCitation(projectReference.getReference(), style, referenceNumbers);
            }
            matcher.appendReplacement(out, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(out);
        return out.toString();
    }

    private String displayCitation(ResearchReportCitation citation, CitationStyle style, Map<UUID, Integer> referenceNumbers) {
        ReferenceEntry reference = citation.getReference();
        if (reference == null && citation.getProjectReference() != null) {
            reference = citation.getProjectReference().getReference();
        }
        if (reference == null) {
            return "";
        }
        return displayCitation(reference, style, referenceNumbers);
    }

    private String displayCitation(ReferenceEntry reference, CitationStyle style, Map<UUID, Integer> referenceNumbers) {
        Integer number = referenceNumbers.get(reference.getId());
        CitationContext context = style == CitationStyle.IEEE || style == CitationStyle.VANCOUVER || style == CitationStyle.NUMERIC_APA
                ? CitationContext.NUMERIC
                : CitationContext.IN_TEXT_PARENTHETICAL;
        var formatted = citationFormattingService.format(reference, style, context, number);
        if (!formatted.metadataComplete() && context != CitationContext.NUMERIC) {
            return "";
        }
        return formatted.text();
    }

    private void recalculateSectionNumbers(ResearchReport report) {
        if (isCourseworkReport(report)) {
            recalculateCourseworkSectionNumbers(report);
            return;
        }
        List<ResearchReportChapter> chapters = chapterRepository.findAllByReportIdOrderByDisplayOrderAsc(report.getId());
        int nextChapterNumber = 1;
        for (ResearchReportChapter chapter : chapters) {
            if (isNumberedChapterType(chapter.getType())) {
                if (!Objects.equals(chapter.getChapterNumber(), nextChapterNumber)) {
                    chapter.setChapterNumber(nextChapterNumber);
                    chapterRepository.save(chapter);
                }
                nextChapterNumber++;
            } else if (chapter.getChapterNumber() != null) {
                chapter.setChapterNumber(null);
                chapterRepository.save(chapter);
            }

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
                assignSectionNumber(root, rootNum);
            }
        }
    }

    private void assignSectionNumber(ResearchReportSection section, String sectionNumber) {
        if (!Objects.equals(section.getSectionNumber(), sectionNumber)) {
            section.setSectionNumber(sectionNumber);
            sectionRepository.save(section);
        }
        List<ResearchReportSection> children = sectionRepository.findAllByParentSectionIdOrderByDisplayOrderAsc(section.getId());
        for (int i = 0; i < children.size(); i++) {
            assignSectionNumber(children.get(i), sectionNumber == null ? null : sectionNumber + "." + (i + 1));
        }
    }

    private void recalculateCourseworkSectionNumbers(ResearchReport report) {
        List<ResearchReportChapter> chapters = chapterRepository.findAllByReportIdOrderByDisplayOrderAsc(report.getId());
        int nextRootNumber = 1;
        for (ResearchReportChapter chapter : chapters) {
            if (chapter.getChapterNumber() != null) {
                chapter.setChapterNumber(null);
                chapterRepository.save(chapter);
            }
            List<ResearchReportSection> rootSections = sectionRepository.findAllByChapterIdAndParentSectionIsNullOrderByDisplayOrderAsc(chapter.getId());
            if (chapter.getType() == ReportChapterType.REFERENCES
                    || chapter.getType() == ReportChapterType.APPENDICES
                    || chapter.getType() == ReportChapterType.PRELIMINARY) {
                for (ResearchReportSection root : rootSections) {
                    assignSectionNumber(root, null);
                }
                continue;
            }
            for (ResearchReportSection root : rootSections) {
                if (root.getType() == ReportSectionType.REFERENCES) {
                    assignSectionNumber(root, null);
                } else {
                    assignSectionNumber(root, String.valueOf(nextRootNumber++));
                }
            }
        }
    }

    private boolean isCourseworkReport(ResearchReport report) {
        return report.getType() == ResearchReportType.COURSEWORK
                || workspaceType(report.getProject()) == AcademicWorkspaceType.COURSEWORK;
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
                if (pr.getStatus() == ProjectReferenceStatus.ACTIVE && pr.isAvailableForCitation() && pr.getReference() != null) {
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
    private ResearchReportTemplate loadTemplate(UUID id, ResearchReportType type, AcademicWorkspaceType workspaceType) {
        if (id != null) {
            ResearchReportTemplate template = templateRepository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException("Report template not found."));
            if (!templateSupportsWorkspace(template, workspaceType)) {
                throw new IllegalArgumentException("Report template is not compatible with this workspace type.");
            }
            return template;
        }
        return templateRepository.findSystemTemplatesForWorkspaceType(
                        type,
                        workspaceType.name(),
                        PageRequest.of(0, 1)
                )
                .stream()
                .findFirst()
                .or(() -> templateRepository.findFirstByTypeAndSystemTemplateTrueOrderByCreatedAtAsc(type))
                .orElse(null);
    }
    private AcademicWorkspaceType workspaceType(ResearchProject project) { return project.getWorkspaceType() == null ? AcademicWorkspaceType.ACADEMIC_RESEARCH : project.getWorkspaceType(); }
    private ResearchReportType defaultReportType(ResearchProject project) {
        return switch (workspaceType(project)) {
            case ACADEMIC_RESEARCH -> ResearchReportType.RESEARCH_REPORT;
            case ACADEMIC_PROJECT -> ResearchReportType.ACADEMIC_PROJECT_REPORT;
            case COURSEWORK -> ResearchReportType.COURSEWORK;
        };
    }
    private String defaultReportTitle(ResearchProject project) {
        String suffix = switch (workspaceType(project)) {
            case ACADEMIC_RESEARCH -> "Research Report";
            case ACADEMIC_PROJECT -> "Project Report";
            case COURSEWORK -> "Coursework Document";
        };
        return project.getTitle() == null || project.getTitle().isBlank()
                ? suffix
                : project.getTitle() + " " + suffix;
    }
    private boolean templateSupportsWorkspace(ResearchReportTemplate template, AcademicWorkspaceType workspaceType) {
        String supportedTypes = template.getSupportedWorkspaceTypes();
        return supportedTypes == null || supportedTypes.isBlank() || supportedTypes.contains(workspaceType.name());
    }
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
