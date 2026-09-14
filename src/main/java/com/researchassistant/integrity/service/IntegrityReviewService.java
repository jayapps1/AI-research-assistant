package com.researchassistant.integrity.service;

import com.researchassistant.analysis.entity.*;
import com.researchassistant.analysis.repository.*;
import com.researchassistant.common.enums.ContentOrigin;
import com.researchassistant.common.exception.ResourceNotFoundException;
import com.researchassistant.document.entity.DocumentChunk;
import com.researchassistant.document.repository.DocumentChunkRepository;
import com.researchassistant.identity.entity.User;
import com.researchassistant.integrity.dto.IntegrityDtos.*;
import com.researchassistant.integrity.entity.*;
import com.researchassistant.integrity.repository.*;
import com.researchassistant.project.service.ProjectAuthorizationService;
import com.researchassistant.security.audit.SecurityAuditEventType;
import com.researchassistant.security.audit.SecurityAuditService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.regex.Pattern;

@Service
public class IntegrityReviewService {
    private static final String LOCAL_LIMITATION = "This comparison covers only sources available within the project and is not a global plagiarism check.";
    private final SimilarityCheckRepository similarityCheckRepository;
    private final SimilarityMatchRepository similarityMatchRepository;
    private final AcademicWritingReviewRepository writingReviewRepository;
    private final AcademicWritingIssueRepository writingIssueRepository;
    private final ResearchIntegrityReviewRepository integrityReviewRepository;
    private final ResearchIntegrityIssueRepository integrityIssueRepository;
    private final ResearchReportRepository reportRepository;
    private final ResearchReportSectionRepository sectionRepository;
    private final ResearchReportCitationRepository citationRepository;
    private final ResearchFindingRepository findingRepository;
    private final ResearchConclusionRepository conclusionRepository;
    private final ResearchRecommendationRepository recommendationRepository;
    private final DocumentChunkRepository chunkRepository;
    private final ProjectAuthorizationService authorizationService;
    private final SecurityAuditService auditService;

    public IntegrityReviewService(SimilarityCheckRepository similarityCheckRepository, SimilarityMatchRepository similarityMatchRepository,
            AcademicWritingReviewRepository writingReviewRepository, AcademicWritingIssueRepository writingIssueRepository,
            ResearchIntegrityReviewRepository integrityReviewRepository, ResearchIntegrityIssueRepository integrityIssueRepository,
            ResearchReportRepository reportRepository, ResearchReportSectionRepository sectionRepository,
            ResearchReportCitationRepository citationRepository, ResearchFindingRepository findingRepository,
            ResearchConclusionRepository conclusionRepository, ResearchRecommendationRepository recommendationRepository,
            DocumentChunkRepository chunkRepository, ProjectAuthorizationService authorizationService, SecurityAuditService auditService) {
        this.similarityCheckRepository = similarityCheckRepository;
        this.similarityMatchRepository = similarityMatchRepository;
        this.writingReviewRepository = writingReviewRepository;
        this.writingIssueRepository = writingIssueRepository;
        this.integrityReviewRepository = integrityReviewRepository;
        this.integrityIssueRepository = integrityIssueRepository;
        this.reportRepository = reportRepository;
        this.sectionRepository = sectionRepository;
        this.citationRepository = citationRepository;
        this.findingRepository = findingRepository;
        this.conclusionRepository = conclusionRepository;
        this.recommendationRepository = recommendationRepository;
        this.chunkRepository = chunkRepository;
        this.authorizationService = authorizationService;
        this.auditService = auditService;
    }

    @Transactional
    public SimilarityResponse runSimilarity(UUID projectId, User user, SimilarityRequest request) {
        authorizationService.requireProjectEditor(projectId, user);
        SimilarityCheck check = new SimilarityCheck();
        check.setProject(authorizationService.requireProjectViewer(projectId, user).project());
        check.setTargetType(request.targetType());
        check.setTargetId(request.targetId());
        check.setCustomText(request.customText());
        check.setProvider(SimilarityProvider.LOCAL_PROJECT);
        check.setRequestedBy(user);
        check.setStatus(SimilarityCheckStatus.RUNNING);
        similarityCheckRepository.save(check);
        auditService.record(user.getId(), SecurityAuditEventType.SIMILARITY_CHECK_REQUESTED);
        String target = targetText(projectId, request.targetType(), request.targetId(), request.customText());
        int total = 0;
        for (DocumentChunk chunk : chunkRepository.findAllByDocumentVersionDocumentProjectId(projectId)) {
            Optional<String> overlap = substantialOverlap(target, chunk.getTextContent());
            if (overlap.isPresent()) {
                SimilarityMatch match = new SimilarityMatch();
                match.setCheck(check);
                match.setChunk(chunk);
                match.setPage(chunk.getPage());
                match.setDocumentVersion(chunk.getDocumentVersion());
                match.setDocument(chunk.getDocumentVersion().getDocument());
                match.setMatchedText(overlap.get());
                match.setSourceTextSnapshot(chunk.getTextContent().substring(0, Math.min(1000, chunk.getTextContent().length())));
                match.setSimilarityScore(1.0);
                match.setType(overlap.get().length() > 180 ? SimilarityMatchType.POSSIBLE_UNATTRIBUTED_OVERLAP : SimilarityMatchType.COMMON_PHRASE);
                match.setTargetStart(Math.max(0, target.indexOf(overlap.get())));
                match.setTargetEnd(match.getTargetStart() + overlap.get().length());
                similarityMatchRepository.save(match);
                total++;
            }
        }
        check.setOverallSimilarityPercent(target.isBlank() ? 0.0 : Math.min(100.0, total * 10.0));
        check.setProviderReportReference(LOCAL_LIMITATION);
        check.setStatus(SimilarityCheckStatus.COMPLETED);
        check.setCompletedAt(OffsetDateTime.now());
        auditService.record(user.getId(), SecurityAuditEventType.SIMILARITY_CHECK_COMPLETED);
        return similarityResponse(check);
    }

    @Transactional(readOnly = true)
    public SimilarityResponse getSimilarity(UUID checkId, User user) {
        SimilarityCheck check = similarityCheckRepository.findById(checkId).orElseThrow(() -> new ResourceNotFoundException("Similarity check not found."));
        authorizationService.requireProjectViewer(check.getProject().getId(), user);
        return similarityResponse(check);
    }

    @Transactional
    public WritingReviewResponse writingReview(UUID reportId, User user) {
        ResearchReport report = loadReportForEdit(reportId, user);
        AcademicWritingReview review = new AcademicWritingReview();
        review.setProject(report.getProject());
        review.setTargetType(WritingReviewTargetType.REPORT);
        review.setTargetId(reportId);
        review.setRequestedBy(user);
        review.setStatus(AcademicWritingReviewStatus.COMPLETED);
        review.setCompletedAt(OffsetDateTime.now());
        writingReviewRepository.save(review);
        for (ResearchReportSection section : sectionRepository.findAllByChapterReportId(reportId)) {
            String content = section.getContent() == null ? "" : section.getContent();
            if (content.contains("[citation needed]")) issue(review, WritingIssueType.MISSING_CITATION, IssueSeverity.WARNING, "Unresolved citation placeholder found.", null);
            if (Pattern.compile("\\b(proves|caused|guarantees)\\b", Pattern.CASE_INSENSITIVE).matcher(content).find()) issue(review, WritingIssueType.CAUSAL_LANGUAGE, IssueSeverity.WARNING, "Potentially overstated causal language requires review.", "Use cautious language unless the study design supports causality.");
            if (section.getSourceArtifactType() != null && section.getSourceArtifactId() == null) issue(review, WritingIssueType.UNSUPPORTED_CLAIM, IssueSeverity.WARNING, "Section declares a source artifact type but no source artifact id.", null);
        }
        auditService.record(user.getId(), SecurityAuditEventType.WRITING_REVIEW_COMPLETED);
        return writingResponse(review);
    }

    @Transactional(readOnly = true)
    public WritingReviewResponse getWritingReview(UUID reviewId, User user) {
        AcademicWritingReview review = writingReviewRepository.findById(reviewId).orElseThrow(() -> new ResourceNotFoundException("Writing review not found."));
        authorizationService.requireProjectViewer(review.getProject().getId(), user);
        return writingResponse(review);
    }

    @Transactional
    public IntegrityReviewResponse integrityReview(UUID reportId, User user) {
        ResearchReport report = loadReportForEdit(reportId, user);
        ResearchIntegrityReview review = new ResearchIntegrityReview();
        review.setProject(report.getProject());
        review.setReport(report);
        review.setRequestedBy(user);
        review.setStatus(IntegrityReviewStatus.COMPLETED);
        review.setCompletedAt(OffsetDateTime.now());
        integrityReviewRepository.save(review);
        UUID projectId = report.getProject().getId();
        findingRepository.findAllByProjectIdOrderByDisplayOrderAsc(projectId, org.springframework.data.domain.Pageable.unpaged()).forEach(f -> { if (f.getAnalysisResults().isEmpty() && f.getAnalysisResult() == null) integrityIssue(review, ResearchIntegrityIssueType.FINDING_WITHOUT_ANALYSIS, IssueSeverity.ERROR, "Finding has no linked analysis result.", "ResearchFinding", f.getId()); });
        conclusionRepository.findAllByProjectIdOrderByDisplayOrderAsc(projectId, org.springframework.data.domain.Pageable.unpaged()).forEach(c -> { if (c.getFindings().isEmpty()) integrityIssue(review, ResearchIntegrityIssueType.CONCLUSION_WITHOUT_FINDING, IssueSeverity.ERROR, "Conclusion has no supporting finding.", "ResearchConclusion", c.getId()); });
        recommendationRepository.findAllByProjectIdOrderByDisplayOrderAsc(projectId, org.springframework.data.domain.Pageable.unpaged()).forEach(r -> { if (r.getFindings().isEmpty() && r.getConclusions().isEmpty()) integrityIssue(review, ResearchIntegrityIssueType.RECOMMENDATION_WITHOUT_SUPPORT, IssueSeverity.ERROR, "Recommendation has no supporting finding or conclusion.", "ResearchRecommendation", r.getId()); });
        for (ResearchReportSection section : sectionRepository.findAllByChapterReportId(reportId)) {
            if ((section.getContent() != null && section.getContent().contains("DOC-")) && citationRepository.findAllBySectionIdOrderByCitationOrdinalAsc(section.getId()).isEmpty()) {
                integrityIssue(review, ResearchIntegrityIssueType.INVALID_REPORT_CITATION, IssueSeverity.WARNING, "Section mentions a document code but has no verified report citation.", "ResearchReportSection", section.getId());
            }
            if (section.getOrigin() == ContentOrigin.AI_GENERATED) integrityIssue(review, ResearchIntegrityIssueType.UNRESOLVED_AI_CONTENT, IssueSeverity.INFO, "AI-generated report section should be disclosed according to policy.", "ResearchReportSection", section.getId());
        }
        auditService.record(user.getId(), SecurityAuditEventType.INTEGRITY_REVIEW_COMPLETED);
        return integrityResponse(review);
    }

    @Transactional(readOnly = true)
    public IntegrityReviewResponse getIntegrityReview(UUID reviewId, User user) {
        ResearchIntegrityReview review = integrityReviewRepository.findById(reviewId).orElseThrow(() -> new ResourceNotFoundException("Integrity review not found."));
        authorizationService.requireProjectViewer(review.getProject().getId(), user);
        return integrityResponse(review);
    }

    @Transactional(readOnly = true)
    public AiUsageSummary aiUsage(UUID projectId, User user) {
        authorizationService.requireProjectViewer(projectId, user);
        List<String> summaries = new ArrayList<>();
        long generated = 0, assisted = 0;
        for (ResearchReportSection s : sectionRepository.findAllByChapterReportProjectId(projectId)) {
            if (s.getOrigin() == ContentOrigin.AI_GENERATED) { generated++; summaries.add("ResearchReportSection:" + s.getId()); }
            if (s.getOrigin() == ContentOrigin.AI_ASSISTED) { assisted++; summaries.add("ResearchReportSection:" + s.getId()); }
        }
        return new AiUsageSummary(projectId, generated, assisted, summaries);
    }

    private ResearchReport loadReportForEdit(UUID reportId, User user) {
        ResearchReport report = reportRepository.findById(reportId).orElseThrow(() -> new ResourceNotFoundException("Report not found."));
        authorizationService.requireProjectEditor(report.getProject().getId(), user);
        return report;
    }

    private String targetText(UUID projectId, SimilarityTargetType type, UUID id, String customText) {
        if (type == SimilarityTargetType.CUSTOM_TEXT) return customText == null ? "" : customText;
        if (id == null) throw new IllegalArgumentException("A target id is required for " + type + ".");
        if (type == SimilarityTargetType.REPORT_SECTION) {
            ResearchReportSection section = sectionRepository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException("Report section not found."));
            if (!section.getChapter().getReport().getProject().getId().equals(projectId)) {
                throw new ResourceNotFoundException("Report section not found.");
            }
            return section.getContent() == null ? "" : section.getContent();
        }
        if (type == SimilarityTargetType.REPORT) {
            ResearchReport report = reportRepository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException("Report not found."));
            if (!report.getProject().getId().equals(projectId)) {
                throw new ResourceNotFoundException("Report not found.");
            }
            return sectionRepository.findAllByChapterReportId(id).stream().map(s -> s.getContent() == null ? "" : s.getContent()).reduce("", (a, b) -> a + "\n" + b);
        }
        return "";
    }

    private Optional<String> substantialOverlap(String target, String source) {
        String normalizedTarget = normalize(target);
        String normalizedSource = normalize(source);
        if (normalizedTarget.length() < 80 || normalizedSource.length() < 80) return Optional.empty();
        for (int i = 0; i + 120 <= normalizedTarget.length(); i += 40) {
            String window = normalizedTarget.substring(i, i + 120);
            if (normalizedSource.contains(window)) return Optional.of(target.substring(Math.min(i, target.length()), Math.min(target.length(), i + 120)));
        }
        return Optional.empty();
    }

    private String normalize(String text) { return text == null ? "" : text.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", " ").replaceAll("\\s+", " ").trim(); }
    private void issue(AcademicWritingReview review, WritingIssueType type, IssueSeverity severity, String message, String suggestion) { AcademicWritingIssue i = new AcademicWritingIssue(); i.setReview(review); i.setType(type); i.setSeverity(severity); i.setMessage(message); i.setSuggestion(suggestion); writingIssueRepository.save(i); }
    private void integrityIssue(ResearchIntegrityReview review, ResearchIntegrityIssueType type, IssueSeverity severity, String message, String artifactType, UUID artifactId) { ResearchIntegrityIssue i = new ResearchIntegrityIssue(); i.setReview(review); i.setType(type); i.setSeverity(severity); i.setMessage(message); i.setArtifactType(artifactType); i.setArtifactId(artifactId); integrityIssueRepository.save(i); }
    private SimilarityResponse similarityResponse(SimilarityCheck check) { return new SimilarityResponse(check.getId(), check.getTargetType(), check.getProvider(), check.getStatus(), check.getOverallSimilarityPercent(), LOCAL_LIMITATION, similarityMatchRepository.findAllByCheckId(check.getId()).stream().map(m -> new SimilarityMatchResponse(m.getId(), m.getType(), m.getSimilarityScore(), m.getMatchedText(), m.getSourceTextSnapshot(), m.getDocument() == null ? null : m.getDocument().getId())).toList()); }
    private WritingReviewResponse writingResponse(AcademicWritingReview review) { return new WritingReviewResponse(review.getId(), review.getStatus(), writingIssueRepository.findAllByReviewId(review.getId()).stream().map(i -> new WritingIssueResponse(i.getType(), i.getSeverity(), i.getMessage(), i.getSuggestion())).toList()); }
    private IntegrityReviewResponse integrityResponse(ResearchIntegrityReview review) { return new IntegrityReviewResponse(review.getId(), review.getStatus(), integrityIssueRepository.findAllByReviewId(review.getId()).stream().map(i -> new IntegrityIssueResponse(i.getType(), i.getSeverity(), i.getMessage(), i.getArtifactType(), i.getArtifactId())).toList()); }
}
