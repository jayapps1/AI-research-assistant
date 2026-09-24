package com.researchassistant.analysis.controller;

import com.researchassistant.analysis.dto.AnalysisDtos.*;
import com.researchassistant.analysis.entity.ResearchFindingStatus;
import com.researchassistant.analysis.entity.ResearchFindingType;
import com.researchassistant.analysis.service.AnalysisWorkflowService;
import com.researchassistant.identity.entity.User;
import com.researchassistant.identity.service.AuthenticatedUserResolver;
import com.researchassistant.project.dto.PageResponse;

import jakarta.validation.Valid;

import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
public class AnalysisWorkflowController {
    private static final int MAX_PAGE_SIZE = 100;
    private final AnalysisWorkflowService service;
    private final AuthenticatedUserResolver userResolver;

    public AnalysisWorkflowController(AnalysisWorkflowService service, AuthenticatedUserResolver userResolver) {
        this.service = service;
        this.userResolver = userResolver;
    }

    @PostMapping("/projects/{projectId}/analysis-runs")
    @ResponseStatus(HttpStatus.CREATED)
    public AnalysisRunResponse createRun(Authentication authentication, @PathVariable UUID projectId, @Valid @RequestBody CreateAnalysisRunRequest request) {
        return service.createRun(projectId, user(authentication), request);
    }

    @GetMapping("/projects/{projectId}/analysis-runs")
    public PageResponse<AnalysisRunResponse> listRuns(Authentication authentication, @PathVariable UUID projectId, @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        return PageResponse.from(service.listRuns(projectId, user(authentication), page(page, size)));
    }

    @PostMapping("/analysis-runs/{analysisRunId}/complete")
    public AnalysisResultResponse completeRun(Authentication authentication, @PathVariable UUID analysisRunId, @Valid @RequestBody CompleteAnalysisRunRequest request) {
        return service.completeRun(analysisRunId, user(authentication), request);
    }

    @PostMapping("/projects/{projectId}/findings")
    @ResponseStatus(HttpStatus.CREATED)
    public FindingResponse createFinding(Authentication authentication, @PathVariable UUID projectId, @Valid @RequestBody CreateFindingRequest request) {
        return service.createFinding(projectId, user(authentication), request);
    }

    @GetMapping("/projects/{projectId}/findings")
    public PageResponse<FindingResponse> listFindings(Authentication authentication, @PathVariable UUID projectId,
            @RequestParam(required = false) UUID objectiveId, @RequestParam(required = false) UUID questionId,
            @RequestParam(required = false) UUID hypothesisId, @RequestParam(required = false) ResearchFindingStatus status,
            @RequestParam(required = false) ResearchFindingType type, @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return PageResponse.from(service.listFindings(projectId, user(authentication), objectiveId, questionId, hypothesisId, status, type, page(page, size)));
    }

    @GetMapping("/findings/{findingId}")
    public FindingResponse getFinding(Authentication authentication, @PathVariable UUID findingId) {
        return service.getFinding(findingId, user(authentication));
    }

    @PatchMapping("/findings/{findingId}")
    public FindingResponse updateFinding(Authentication authentication, @PathVariable UUID findingId, @Valid @RequestBody UpdateFindingRequest request) {
        return service.updateFinding(findingId, user(authentication), request);
    }

    @PostMapping("/findings/{findingId}/approve")
    public FindingResponse approveFinding(Authentication authentication, @PathVariable UUID findingId) {
        return service.approveFinding(findingId, user(authentication));
    }

    @PostMapping("/findings/{findingId}/archive")
    public FindingResponse archiveFinding(Authentication authentication, @PathVariable UUID findingId) {
        return service.archiveFinding(findingId, user(authentication));
    }

    @PostMapping("/projects/{projectId}/findings/generate")
    public GeneratedDraftResponse generateFinding(Authentication authentication, @PathVariable UUID projectId, @Valid @RequestBody GenerateFindingRequest request) {
        return service.generateFinding(projectId, user(authentication), request);
    }

    @PostMapping("/findings/{findingId}/discussion")
    @ResponseStatus(HttpStatus.CREATED)
    public DiscussionResponse createDiscussion(Authentication authentication, @PathVariable UUID findingId, @Valid @RequestBody CreateDiscussionRequest request) {
        return service.createDiscussion(findingId, user(authentication), request);
    }

    @GetMapping("/findings/{findingId}/discussion")
    public java.util.List<DiscussionResponse> listDiscussions(Authentication authentication, @PathVariable UUID findingId) {
        return service.listDiscussions(findingId, user(authentication));
    }

    @PatchMapping("/discussions/{discussionId}")
    public DiscussionResponse updateDiscussion(Authentication authentication, @PathVariable UUID discussionId, @Valid @RequestBody UpdateDiscussionRequest request) {
        return service.updateDiscussion(discussionId, user(authentication), request);
    }

    @PostMapping("/findings/{findingId}/discussion/generate")
    public GeneratedDraftResponse generateDiscussion(Authentication authentication, @PathVariable UUID findingId) {
        return service.generateDiscussion(findingId, user(authentication));
    }

    @GetMapping("/discussions/{discussionId}/evidence")
    public java.util.List<DiscussionEvidenceResponse> discussionEvidence(Authentication authentication, @PathVariable UUID discussionId) {
        return service.discussionEvidence(discussionId, user(authentication));
    }

    @PostMapping("/discussions/{discussionId}/approve")
    public DiscussionResponse approveDiscussion(Authentication authentication, @PathVariable UUID discussionId) {
        return service.approveDiscussion(discussionId, user(authentication));
    }

    @PostMapping("/projects/{projectId}/conclusions")
    @ResponseStatus(HttpStatus.CREATED)
    public ConclusionResponse createConclusion(Authentication authentication, @PathVariable UUID projectId, @Valid @RequestBody CreateConclusionRequest request) {
        return service.createConclusion(projectId, user(authentication), request);
    }

    @GetMapping("/projects/{projectId}/conclusions")
    public PageResponse<ConclusionResponse> listConclusions(Authentication authentication, @PathVariable UUID projectId, @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        return PageResponse.from(service.listConclusions(projectId, user(authentication), page(page, size)));
    }

    @GetMapping("/conclusions/{conclusionId}")
    public ConclusionResponse getConclusion(Authentication authentication, @PathVariable UUID conclusionId) {
        return service.getConclusion(conclusionId, user(authentication));
    }

    @PatchMapping("/conclusions/{conclusionId}")
    public ConclusionResponse updateConclusion(Authentication authentication, @PathVariable UUID conclusionId, @Valid @RequestBody UpdateConclusionRequest request) {
        return service.updateConclusion(conclusionId, user(authentication), request);
    }

    @PostMapping("/conclusions/{conclusionId}/approve")
    public ConclusionResponse approveConclusion(Authentication authentication, @PathVariable UUID conclusionId) {
        return service.approveConclusion(conclusionId, user(authentication));
    }

    @PostMapping("/projects/{projectId}/conclusions/generate")
    public GeneratedDraftResponse generateConclusion(Authentication authentication, @PathVariable UUID projectId) {
        return service.generateConclusion(projectId, user(authentication));
    }

    @PostMapping("/projects/{projectId}/recommendations")
    @ResponseStatus(HttpStatus.CREATED)
    public RecommendationResponse createRecommendation(Authentication authentication, @PathVariable UUID projectId, @Valid @RequestBody CreateRecommendationRequest request) {
        return service.createRecommendation(projectId, user(authentication), request);
    }

    @GetMapping("/projects/{projectId}/recommendations")
    public PageResponse<RecommendationResponse> listRecommendations(Authentication authentication, @PathVariable UUID projectId, @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        return PageResponse.from(service.listRecommendations(projectId, user(authentication), page(page, size)));
    }

    @GetMapping("/recommendations/{recommendationId}")
    public RecommendationResponse getRecommendation(Authentication authentication, @PathVariable UUID recommendationId) {
        return service.getRecommendation(recommendationId, user(authentication));
    }

    @PatchMapping("/recommendations/{recommendationId}")
    public RecommendationResponse updateRecommendation(Authentication authentication, @PathVariable UUID recommendationId, @Valid @RequestBody UpdateRecommendationRequest request) {
        return service.updateRecommendation(recommendationId, user(authentication), request);
    }

    @PostMapping("/recommendations/{recommendationId}/approve")
    public RecommendationResponse approveRecommendation(Authentication authentication, @PathVariable UUID recommendationId) {
        return service.approveRecommendation(recommendationId, user(authentication));
    }

    @PostMapping("/projects/{projectId}/recommendations/generate")
    public GeneratedDraftResponse generateRecommendation(Authentication authentication, @PathVariable UUID projectId) {
        return service.generateRecommendation(projectId, user(authentication));
    }

    @PostMapping("/projects/{projectId}/reports")
    @ResponseStatus(HttpStatus.CREATED)
    public ReportResponse createReport(Authentication authentication, @PathVariable UUID projectId, @Valid @RequestBody CreateReportRequest request) {
        return service.createReport(projectId, user(authentication), request);
    }

    @PostMapping("/projects/{projectId}/reports/ensure")
    public ReportResponse ensureReportInitialized(Authentication authentication, @PathVariable UUID projectId) {
        return service.ensureReportInitialized(projectId, user(authentication));
    }

    @GetMapping("/projects/{projectId}/reports")
    public PageResponse<ReportResponse> listReports(Authentication authentication, @PathVariable UUID projectId, @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        return PageResponse.from(service.listReports(projectId, user(authentication), page(page, size)));
    }

    @GetMapping("/reports/{reportId}")
    public ReportResponse getReport(Authentication authentication, @PathVariable UUID reportId) {
        return service.getReport(reportId, user(authentication));
    }

    @PatchMapping("/reports/{reportId}")
    public ReportResponse updateReport(Authentication authentication, @PathVariable UUID reportId, @Valid @RequestBody UpdateReportRequest request) {
        return service.updateReport(reportId, user(authentication), request);
    }

    @PostMapping("/reports/{reportId}/assemble")
    public ReportResponse assembleReport(Authentication authentication, @PathVariable UUID reportId) {
        return service.assembleReport(reportId, user(authentication));
    }

    @PostMapping("/reports/{reportId}/validate")
    public ReportValidationResponse validateReport(Authentication authentication, @PathVariable UUID reportId) {
        return service.validateReport(reportId, user(authentication));
    }

    @GetMapping("/report-templates")
    public java.util.List<TemplateResponse> listTemplates(Authentication authentication) {
        return service.listTemplates(user(authentication));
    }

    @GetMapping("/projects/{projectId}/section-capabilities")
    public SectionCapabilitiesResponse getSectionCapabilities(Authentication authentication, @PathVariable UUID projectId) {
        return service.getSectionCapabilities(projectId, user(authentication));
    }

    @GetMapping("/reports/{reportId}/table-of-contents")
    public TableOfContentsResponse getTableOfContents(Authentication authentication, @PathVariable UUID reportId) {
        return service.generateTableOfContents(reportId, user(authentication));
    }

    @PostMapping("/reports/{reportId}/finalize")
    public ReportResponse finalizeReport(Authentication authentication, @PathVariable UUID reportId) {
        return service.finalizeReport(reportId, user(authentication));
    }

    @PostMapping("/reports/{reportId}/final-document/prepare")
    public FinalDocumentResponse prepareFinalDocument(Authentication authentication, @PathVariable UUID reportId) {
        return service.prepareFinalDocument(reportId, user(authentication));
    }

    @GetMapping("/reports/{reportId}/final-document")
    public ResponseEntity<FinalDocumentResponse> getFinalDocument(Authentication authentication, @PathVariable UUID reportId) {
        FinalDocumentResponse response = service.getFinalDocument(reportId, user(authentication));
        return response == null ? ResponseEntity.noContent().build() : ResponseEntity.ok(response);
    }

    @PatchMapping("/reports/{reportId}/final-document")
    public FinalDocumentResponse updateFinalDocument(Authentication authentication, @PathVariable UUID reportId, @Valid @RequestBody UpdateFinalDocumentRequest request) {
        return service.updateFinalDocument(reportId, user(authentication), request);
    }

    @PutMapping("/reports/{reportId}/final-document")
    public FinalDocumentResponse putFinalDocument(Authentication authentication, @PathVariable UUID reportId, @Valid @RequestBody UpdateFinalDocumentRequest request) {
        return service.updateFinalDocument(reportId, user(authentication), request);
    }

    @GetMapping("/reports/{reportId}/chapters")
    public java.util.List<ChapterResponse> listChapters(Authentication authentication, @PathVariable UUID reportId) {
        return service.listChapters(reportId, user(authentication));
    }

    @PostMapping("/reports/{reportId}/chapters")
    @ResponseStatus(HttpStatus.CREATED)
    public ChapterResponse createChapter(Authentication authentication, @PathVariable UUID reportId, @Valid @RequestBody CreateChapterRequest request) {
        return service.createChapter(reportId, user(authentication), request);
    }

    @PatchMapping("/report-chapters/{chapterId}")
    public ChapterResponse updateChapter(Authentication authentication, @PathVariable UUID chapterId, @Valid @RequestBody UpdateChapterRequest request) {
        return service.updateChapter(chapterId, user(authentication), request);
    }

    @GetMapping("/reports/{reportId}/structure")
    public ReportStructureResponse getReportStructure(Authentication authentication, @PathVariable UUID reportId) {
        return service.getReportStructure(reportId, user(authentication));
    }

    @PostMapping("/reports/{reportId}/structure/reorder")
    public ReportStructureResponse reorderStructure(Authentication authentication, @PathVariable UUID reportId, @Valid @RequestBody ReorderStructureRequest request) {
        return service.reorderStructure(reportId, request, user(authentication));
    }

    @DeleteMapping("/report-chapters/{chapterId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteChapter(Authentication authentication, @PathVariable UUID chapterId) {
        service.deleteChapter(chapterId, user(authentication));
    }

    @GetMapping("/report-chapters/{chapterId}/sections")
    public java.util.List<SectionResponse> listSections(Authentication authentication, @PathVariable UUID chapterId) {
        return service.listSections(chapterId, user(authentication));
    }

    @PostMapping("/report-chapters/{chapterId}/sections")
    @ResponseStatus(HttpStatus.CREATED)
    public SectionResponse createSection(Authentication authentication, @PathVariable UUID chapterId, @Valid @RequestBody CreateSectionRequest request) {
        return service.createSection(chapterId, user(authentication), request);
    }

    @PatchMapping("/report-sections/{sectionId}")
    public SectionResponse updateSection(Authentication authentication, @PathVariable UUID sectionId, @Valid @RequestBody UpdateSectionRequest request) {
        return service.updateSection(sectionId, user(authentication), request);
    }

    @DeleteMapping("/report-sections/{sectionId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteSection(Authentication authentication, @PathVariable UUID sectionId) {
        service.deleteSection(sectionId, user(authentication));
    }

    @PostMapping("/reports/{reportId}/references/refresh")
    public SectionResponse refreshReportReferences(Authentication authentication, @PathVariable UUID reportId) {
        return service.refreshReportReferences(reportId, user(authentication));
    }

    @PatchMapping("/reports/{reportId}/settings")
    public ReportResponse updateReportSettings(Authentication authentication, @PathVariable UUID reportId, @Valid @RequestBody UpdateReportSettingsRequest request) {
        return service.updateReportSettings(reportId, user(authentication), request);
    }

    @GetMapping("/projects/{projectId}/literature-matrix")
    public LiteratureMatrixResponse getLiteratureMatrix(Authentication authentication, @PathVariable UUID projectId) {
        return service.getLiteratureMatrix(projectId, user(authentication));
    }

    @PostMapping("/projects/{projectId}/literature-matrix")
    public LiteratureMatrixResponse saveLiteratureMatrix(Authentication authentication, @PathVariable UUID projectId, @Valid @RequestBody SaveLiteratureMatrixRequest request) {
        return service.saveLiteratureMatrix(projectId, user(authentication), request);
    }

    @PostMapping("/report-sections/{sectionId}/generate")
    public GeneratedDraftResponse generateSection(Authentication authentication, @PathVariable UUID sectionId, @RequestBody(required = false) GenerateSectionRequest request) {
        return service.generateSection(sectionId, user(authentication), request);
    }

    @GetMapping("/report-sections/{sectionId}/citations")
    public java.util.List<CitationResponse> listCitations(Authentication authentication, @PathVariable UUID sectionId) {
        return service.listCitations(sectionId, user(authentication));
    }

    @GetMapping("/projects/{projectId}/traceability-matrix")
    public TraceabilityMatrixResponse traceabilityMatrix(Authentication authentication, @PathVariable UUID projectId) {
        return service.traceabilityMatrix(projectId, user(authentication));
    }

    private PageRequest page(int page, int size) {
        return PageRequest.of(Math.max(0, page), Math.min(Math.max(1, size), MAX_PAGE_SIZE));
    }

    private User user(Authentication authentication) {
        return userResolver.requireActiveUser(authentication);
    }
}
