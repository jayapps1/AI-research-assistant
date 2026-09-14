package com.researchassistant.analysis.service;

import com.researchassistant.analysis.dto.AnalysisDtos.*;
import com.researchassistant.analysis.entity.*;
import com.researchassistant.analysis.repository.*;
import com.researchassistant.cache.CacheInvalidationService;
import com.researchassistant.common.enums.ContentOrigin;
import com.researchassistant.common.exception.ResourceNotFoundException;
import com.researchassistant.dataset.model.ResearchDataset;
import com.researchassistant.dataset.repository.ResearchDatasetRepository;
import com.researchassistant.identity.entity.User;
import com.researchassistant.methodology.repository.MethodologyRepository;
import com.researchassistant.project.entity.ResearchProject;
import com.researchassistant.project.service.ProjectAuthorizationService;
import com.researchassistant.rag.exception.RagCapabilityUnavailableException;
import com.researchassistant.researchdesign.entity.*;
import com.researchassistant.researchdesign.repository.*;
import com.researchassistant.security.audit.SecurityAuditEventType;
import com.researchassistant.security.audit.SecurityAuditService;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    private final ResearchObjectiveRepository objectiveRepository;
    private final ResearchQuestionRepository questionRepository;
    private final ResearchHypothesisRepository hypothesisRepository;
    private final ResearchProblemRepository problemRepository;
    private final MethodologyRepository methodologyRepository;
    private final ResearchDatasetRepository datasetRepository;
    private final ProjectAuthorizationService authorizationService;
    private final CacheInvalidationService cacheInvalidationService;
    private final SecurityAuditService auditService;

    public AnalysisWorkflowService(AnalysisRunRepository runRepository, AnalysisResultRepository resultRepository,
            ResearchFindingRepository findingRepository, FindingDiscussionRepository discussionRepository,
            DiscussionEvidenceRepository discussionEvidenceRepository, ResearchConclusionRepository conclusionRepository,
            ResearchRecommendationRepository recommendationRepository, ResearchReportRepository reportRepository,
            ResearchReportTemplateRepository templateRepository, ResearchReportChapterRepository chapterRepository,
            ResearchReportSectionRepository sectionRepository, ResearchReportCitationRepository citationRepository,
            ResearchObjectiveRepository objectiveRepository, ResearchQuestionRepository questionRepository,
            ResearchHypothesisRepository hypothesisRepository, ResearchProblemRepository problemRepository,
            MethodologyRepository methodologyRepository, ResearchDatasetRepository datasetRepository,
            ProjectAuthorizationService authorizationService, CacheInvalidationService cacheInvalidationService,
            SecurityAuditService auditService) {
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
        this.objectiveRepository = objectiveRepository;
        this.questionRepository = questionRepository;
        this.hypothesisRepository = hypothesisRepository;
        this.problemRepository = problemRepository;
        this.methodologyRepository = methodologyRepository;
        this.datasetRepository = datasetRepository;
        this.authorizationService = authorizationService;
        this.cacheInvalidationService = cacheInvalidationService;
        this.auditService = auditService;
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
        report.setTemplate(loadTemplate(request.templateId(), report.getType()));
        report.setInstitutionName(optional(request.institutionName()));
        report.setDepartmentName(optional(request.departmentName()));
        report.setAuthorName(optional(request.authorName()));
        report.setSupervisorName(optional(request.supervisorName()));
        report.setDegreeProgram(optional(request.degreeProgram()));
        report.setSubmissionYear(request.submissionYear());
        report.setCitationStyle(request.citationStyle() == null ? CitationStyle.APA_7 : request.citationStyle());
        report.setOrigin(defaultOrigin(request.origin()));
        report.setCreatedBy(user);
        ResearchReport saved = reportRepository.save(report);
        auditService.record(user.getId(), SecurityAuditEventType.REPORT_CREATED);
        return ReportResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public Page<ReportResponse> listReports(UUID projectId, User user, Pageable pageable) {
        authorizationService.requireProjectViewer(projectId, user);
        return reportRepository.findAllByProjectIdOrderByUpdatedAtDesc(projectId, pageable).map(ReportResponse::from);
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
        if (!chapterRepository.findAllByReportIdOrderByDisplayOrderAsc(reportId).isEmpty()) return ReportResponse.from(report);
        createDefaultChapters(report, user);
        auditService.record(user.getId(), SecurityAuditEventType.REPORT_ASSEMBLED);
        return ReportResponse.from(report);
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
        }
        auditService.record(user.getId(), SecurityAuditEventType.REPORT_VALIDATED);
        return new ReportValidationResponse(errors, warnings, info);
    }

    @Transactional
    public ReportResponse finalizeReport(UUID reportId, User user) {
        ResearchReport report = loadReport(reportId);
        authorizationService.requireProjectAdminAccess(report.getProject().getId(), user);
        ReportValidationResponse validation = validateReport(reportId, user);
        if (!validation.errors().isEmpty()) throw new IllegalStateException("Report cannot be finalized while validation errors exist.");
        report.setStatus(ResearchReportStatus.FINAL);
        report.setRevisionNumber(report.getRevisionNumber() + 1);
        auditService.record(user.getId(), SecurityAuditEventType.REPORT_FINALIZED);
        return ReportResponse.from(report);
    }

    @Transactional(readOnly = true)
    public List<ChapterResponse> listChapters(UUID reportId, User user) {
        ResearchReport report = loadReport(reportId);
        authorizationService.requireProjectViewer(report.getProject().getId(), user);
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
        return ChapterResponse.from(chapterRepository.save(chapter));
    }

    @Transactional
    public ChapterResponse updateChapter(UUID chapterId, User user, UpdateChapterRequest request) {
        ResearchReportChapter chapter = chapterRepository.findById(chapterId).orElseThrow(() -> new ResourceNotFoundException("Report chapter not found."));
        loadReportForEdit(chapter.getReport().getId(), user);
        if (request.type() != null) chapter.setType(request.type());
        if (request.title() != null) chapter.setTitle(optional(request.title()));
        if (request.chapterNumber() != null) chapter.setChapterNumber(request.chapterNumber());
        if (request.displayOrder() != null) chapter.setDisplayOrder(request.displayOrder());
        return ChapterResponse.from(chapter);
    }

    @Transactional(readOnly = true)
    public List<SectionResponse> listSections(UUID chapterId, User user) {
        ResearchReportChapter chapter = chapterRepository.findById(chapterId).orElseThrow(() -> new ResourceNotFoundException("Report chapter not found."));
        authorizationService.requireProjectViewer(chapter.getReport().getProject().getId(), user);
        return sectionRepository.findAllByChapterIdOrderByDisplayOrderAsc(chapterId).stream().map(SectionResponse::from).toList();
    }

    @Transactional
    public SectionResponse createSection(UUID chapterId, User user, CreateSectionRequest request) {
        ResearchReportChapter chapter = chapterRepository.findById(chapterId).orElseThrow(() -> new ResourceNotFoundException("Report chapter not found."));
        loadReportForEdit(chapter.getReport().getId(), user);
        ResearchReportSection section = new ResearchReportSection();
        section.setChapter(chapter);
        section.setType(request.type());
        section.setHeading(required(request.heading(), "Section heading is required."));
        section.setContent(optional(request.content()));
        section.setDisplayOrder(request.displayOrder() == null ? 1 : request.displayOrder());
        section.setOrigin(defaultOrigin(request.origin()));
        section.setSourceArtifactType(optional(request.sourceArtifactType()));
        section.setSourceArtifactId(request.sourceArtifactId());
        section.setSourceRevisionNumber(resolveSourceRevision(section.getSourceArtifactType(), section.getSourceArtifactId()));
        section.setCreatedBy(user);
        return SectionResponse.from(sectionRepository.save(section));
    }

    @Transactional
    public SectionResponse updateSection(UUID sectionId, User user, UpdateSectionRequest request) {
        ResearchReportSection section = sectionRepository.findById(sectionId).orElseThrow(() -> new ResourceNotFoundException("Report section not found."));
        loadReportForEdit(section.getChapter().getReport().getId(), user);
        if (request.type() != null) section.setType(request.type());
        if (request.heading() != null) section.setHeading(optional(request.heading()));
        if (request.content() != null) { section.setContent(optional(request.content())); section.setManuallyEdited(true); }
        if (request.displayOrder() != null) section.setDisplayOrder(request.displayOrder());
        if (request.origin() != null) section.setOrigin(request.origin());
        if (request.sourceOutOfDate() != null) section.setSourceOutOfDate(request.sourceOutOfDate());
        section.setUpdatedBy(user);
        section.setRevisionNumber(section.getRevisionNumber() + 1);
        auditService.record(user.getId(), SecurityAuditEventType.REPORT_SECTION_UPDATED);
        return SectionResponse.from(section);
    }

    public GeneratedDraftResponse generateSection(UUID sectionId, User user) {
        ResearchReportSection section = sectionRepository.findById(sectionId).orElseThrow(() -> new ResourceNotFoundException("Report section not found."));
        authorizationService.requireProjectEditor(section.getChapter().getReport().getProject().getId(), user);
        throw new RagCapabilityUnavailableException("AI report-section generation is disabled. Assembly and manual editing remain available.");
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
                {ReportChapterType.PRELIMINARY, "Preliminary Pages", null},
                {ReportChapterType.INTRODUCTION, "Chapter One: Introduction", 1},
                {ReportChapterType.LITERATURE_REVIEW, "Chapter Two: Literature Review", 2},
                {ReportChapterType.METHODOLOGY, "Chapter Three: Methodology", 3},
                {ReportChapterType.RESULTS, "Chapter Four: Results / Findings", 4},
                {ReportChapterType.DISCUSSION, "Chapter Five: Discussion, Conclusions and Recommendations", 5},
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
        switch (chapter.getType()) {
            case INTRODUCTION -> { section.setType(ReportSectionType.PROBLEM_STATEMENT); section.setHeading("Problem Statement"); section.setSourceArtifactType("ResearchProblem"); }
            case LITERATURE_REVIEW -> { section.setType(ReportSectionType.LITERATURE_REVIEW); section.setHeading("Literature Review"); section.setSourceArtifactType("LiteratureReview"); }
            case METHODOLOGY -> { section.setType(ReportSectionType.METHODOLOGY); section.setHeading("Methodology"); section.setSourceArtifactType("Methodology"); }
            case RESULTS -> { section.setType(ReportSectionType.FINDINGS); section.setHeading("Findings"); section.setSourceArtifactType("ResearchFinding"); }
            case DISCUSSION -> { section.setType(ReportSectionType.DISCUSSION); section.setHeading("Discussion, Conclusions and Recommendations"); section.setSourceArtifactType("DiscussionOfFinding"); }
            default -> { section.setType(ReportSectionType.CUSTOM); section.setHeading(chapter.getTitle()); }
        }
        sectionRepository.save(section);
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
}
