package com.researchassistant.analysis.service;

import com.researchassistant.analysis.dto.AnalysisDtos.GeneratedDraftResponse;
import com.researchassistant.analysis.dto.AnalysisDtos.GenerateSectionRequest;
import com.researchassistant.common.enums.ContentOrigin;
import com.researchassistant.analysis.entity.ReportSectionStatus;
import com.researchassistant.analysis.entity.ReportSectionType;
import com.researchassistant.analysis.entity.ResearchReport;
import com.researchassistant.analysis.entity.ResearchReportCitation;
import com.researchassistant.analysis.entity.ResearchReportSection;
import com.researchassistant.analysis.entity.SectionGenerationPolicy;
import com.researchassistant.analysis.entity.SectionSemanticPurpose;
import com.researchassistant.analysis.exception.InsufficientProjectEvidenceException;
import com.researchassistant.analysis.repository.AnalysisResultRepository;
import com.researchassistant.analysis.repository.AnalysisRunRepository;
import com.researchassistant.analysis.repository.ResearchConclusionRepository;
import com.researchassistant.analysis.repository.ResearchFindingRepository;
import com.researchassistant.analysis.repository.ResearchRecommendationRepository;
import com.researchassistant.analysis.repository.ResearchReportCitationRepository;
import com.researchassistant.analysis.repository.ResearchReportSectionRepository;
import com.researchassistant.common.exception.ResourceNotFoundException;
import com.researchassistant.document.entity.DocumentStatus;
import com.researchassistant.document.repository.DocumentRepository;
import com.researchassistant.identity.entity.User;
import com.researchassistant.literature.entity.LiteratureMatrix;
import com.researchassistant.literature.repository.LiteratureMatrixRepository;
import com.researchassistant.project.entity.AcademicProjectType;
import com.researchassistant.project.entity.AcademicWorkspaceType;
import com.researchassistant.project.entity.ResearchProject;
import com.researchassistant.project.service.ProjectAuthorizationService;
import com.researchassistant.rag.dto.request.SubmitRagQueryRequest;
import com.researchassistant.rag.dto.response.CitationResponse;
import com.researchassistant.rag.dto.response.GroundedAnswerResponse;
import com.researchassistant.rag.entity.RagQueryEvidence;
import com.researchassistant.rag.repository.RagQueryEvidenceRepository;
import com.researchassistant.rag.scope.RetrievalScopeType;
import com.researchassistant.rag.service.RagQueryService;
import com.researchassistant.reference.entity.ProjectReference;
import com.researchassistant.reference.service.ProjectReferenceRegistryService;
import com.researchassistant.security.audit.SecurityAuditEventType;
import com.researchassistant.security.audit.SecurityAuditService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Central academic section generation service.
 * Unified entry point for Report AI Draft and AI Assistant Generate.
 * Handles deterministic system nodes (0 AI calls, 0 credits), evidence gating,
 * purpose-specific prompt construction, and staged RAG generation.
 */
@Service
public class AcademicSectionGenerationService {

    private static final Logger log = LoggerFactory.getLogger(AcademicSectionGenerationService.class);

    private final AcademicSectionPromptService promptService;
    private final RagQueryService ragQueryService;
    private final ProjectAuthorizationService authorizationService;
    private final ResearchReportSectionRepository sectionRepository;
    private final AnalysisRunRepository runRepository;
    private final ResearchFindingRepository findingRepository;
    private final AnalysisResultRepository resultRepository;
    private final ResearchConclusionRepository conclusionRepository;
    private final ResearchRecommendationRepository recommendationRepository;
    private final DocumentRepository documentRepository;
    private final ResearchReportCitationRepository citationRepository;
    private final RagQueryEvidenceRepository ragEvidenceRepository;
    private final ProjectReferenceRegistryService referenceRegistryService;
    private final LiteratureMatrixRepository literatureMatrixRepository;
    private final ReportMarkdownRenderer markdownRenderer;
    private final ReportRichTextService richTextService;
    private final TransactionTemplate transactionTemplate;
    private final SecurityAuditService auditService;
    private final com.researchassistant.evidence.repository.ProjectEvidenceRepository evidenceRepository;
    private final com.researchassistant.evidence.service.ProjectEvidenceNumberingService numberingService;
    private final AcademicGeneratedFigureService generatedFigureService;

    public AcademicSectionGenerationService(
            AcademicSectionPromptService promptService,
            RagQueryService ragQueryService,
            ProjectAuthorizationService authorizationService,
            ResearchReportSectionRepository sectionRepository,
            AnalysisRunRepository runRepository,
            ResearchFindingRepository findingRepository,
            AnalysisResultRepository resultRepository,
            ResearchConclusionRepository conclusionRepository,
            ResearchRecommendationRepository recommendationRepository,
            DocumentRepository documentRepository,
            ResearchReportCitationRepository citationRepository,
            RagQueryEvidenceRepository ragEvidenceRepository,
            ProjectReferenceRegistryService referenceRegistryService,
            LiteratureMatrixRepository literatureMatrixRepository,
            ReportMarkdownRenderer markdownRenderer,
            ReportRichTextService richTextService,
            PlatformTransactionManager transactionManager,
            SecurityAuditService auditService
    ) {
        this(promptService, ragQueryService, authorizationService, sectionRepository,
                runRepository, findingRepository, resultRepository, conclusionRepository,
                recommendationRepository, documentRepository, citationRepository,
                ragEvidenceRepository, referenceRegistryService, literatureMatrixRepository,
                markdownRenderer, richTextService, transactionManager, auditService,
                null, null, null);
    }

    @org.springframework.beans.factory.annotation.Autowired
    public AcademicSectionGenerationService(
            AcademicSectionPromptService promptService,
            RagQueryService ragQueryService,
            ProjectAuthorizationService authorizationService,
            ResearchReportSectionRepository sectionRepository,
            AnalysisRunRepository runRepository,
            ResearchFindingRepository findingRepository,
            AnalysisResultRepository resultRepository,
            ResearchConclusionRepository conclusionRepository,
            ResearchRecommendationRepository recommendationRepository,
            DocumentRepository documentRepository,
            ResearchReportCitationRepository citationRepository,
            RagQueryEvidenceRepository ragEvidenceRepository,
            ProjectReferenceRegistryService referenceRegistryService,
            LiteratureMatrixRepository literatureMatrixRepository,
            ReportMarkdownRenderer markdownRenderer,
            ReportRichTextService richTextService,
            PlatformTransactionManager transactionManager,
            SecurityAuditService auditService,
            com.researchassistant.evidence.repository.ProjectEvidenceRepository evidenceRepository,
            com.researchassistant.evidence.service.ProjectEvidenceNumberingService numberingService,
            @org.springframework.beans.factory.annotation.Autowired(required = false) AcademicGeneratedFigureService generatedFigureService
    ) {
        this.promptService = promptService;
        this.ragQueryService = ragQueryService;
        this.authorizationService = authorizationService;
        this.sectionRepository = sectionRepository;
        this.runRepository = runRepository;
        this.findingRepository = findingRepository;
        this.resultRepository = resultRepository;
        this.conclusionRepository = conclusionRepository;
        this.recommendationRepository = recommendationRepository;
        this.documentRepository = documentRepository;
        this.citationRepository = citationRepository;
        this.ragEvidenceRepository = ragEvidenceRepository;
        this.referenceRegistryService = referenceRegistryService;
        this.literatureMatrixRepository = literatureMatrixRepository;
        this.markdownRenderer = markdownRenderer;
        this.richTextService = richTextService;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
        this.auditService = auditService;
        this.evidenceRepository = evidenceRepository;
        this.numberingService = numberingService;
        this.generatedFigureService = generatedFigureService;
    }

    public record AcademicSectionGenerationContext(
            UUID workspaceId,
            AcademicWorkspaceType workspaceType,
            UUID projectId,
            AcademicProjectType projectType,
            UUID reportId,
            UUID targetSectionId,
            ReportSectionType sectionType,
            SectionSemanticPurpose semanticPurpose,
            SectionGenerationPolicy generationPolicy,
            String sectionTitle,
            String parentSection,
            String chapterTitle,
            String projectTitle,
            String projectDescription,
            String researchAim,
            List<String> objectives,
            List<String> researchQuestions,
            String problemStatement,
            String studyArea,
            String researchType,
            String methodologySummary,
            int findingsCount,
            int analysisRunsCount,
            String templateRequirements,
            String selectedSourceScope,
            Set<UUID> selectedDocumentIds,
            String customInstructions,
            Integer evidenceLimit,
            String applyMode
    ) {}

    /**
     * Central generation execution method.
     */
    public GeneratedDraftResponse generate(AcademicSectionGenerationContext context, User user) {
        if (context.projectId() != null) {
            authorizationService.requireProjectEditor(context.projectId(), user);
        }

        SectionGenerationPolicy policy = context.generationPolicy() != null
                ? context.generationPolicy()
                : (context.semanticPurpose() != null ? context.semanticPurpose().defaultPolicy() : SectionGenerationPolicy.CONTEXTUAL_AI);
        SectionSemanticPurpose purpose = context.semanticPurpose() != null
                ? context.semanticPurpose()
                : SectionSemanticPurpose.CUSTOM;

        // 1. Evidence Gating
        if (policy == SectionGenerationPolicy.PROJECT_EVIDENCE_REQUIRED && context.projectId() != null) {
            validateEvidenceGating(context.projectId(), purpose);
        }

        // 2. User-Authored Front Matter validation
        if (policy == SectionGenerationPolicy.USER_AUTHORED_FRONT_MATTER) {
            if (context.customInstructions() == null || context.customInstructions().trim().isEmpty()) {
                throw new IllegalArgumentException(
                        context.sectionTitle() + " is a user-authored section. Please supply personal or contextual information in custom instructions to assist with drafting."
                );
            }
        }

        // 3. Build section-specific prompt and retrieval query
        List<AcademicSectionPromptService.StructuredFigureContext> figureContexts = new ArrayList<>();
        if (evidenceRepository != null && context.targetSectionId() != null) {
            List<com.researchassistant.evidence.entity.ProjectEvidence> evidenceList =
                    evidenceRepository.findAllBySectionIdOrderByDisplayOrderAscCreatedAtAsc(context.targetSectionId());
            Map<UUID, String> labels = numberingService != null && context.projectId() != null
                    ? numberingService.computeDynamicLabelsForProject(context.projectId())
                    : Map.of();
            for (com.researchassistant.evidence.entity.ProjectEvidence e : evidenceList) {
                String label = labels.getOrDefault(e.getId(), e.getFigureLabel() != null ? e.getFigureLabel() : "Figure");
                figureContexts.add(new AcademicSectionPromptService.StructuredFigureContext(
                        e.getId(),
                        label,
                        e.getCaption(),
                        e.getEvidenceType().name(),
                        e.getDescription(),
                        e.getAiVisualAnalysis(),
                        e.getMetadataJson()
                ));
            }
        }

        AcademicSectionPromptService.SectionGenerationContext promptContext = new AcademicSectionPromptService.SectionGenerationContext(
                context.workspaceType(),
                context.projectType(),
                "REPORT",
                context.chapterTitle(),
                context.targetSectionId(),
                context.sectionType(),
                purpose,
                policy,
                context.sectionTitle(),
                context.parentSection(),
                context.projectTitle(),
                context.projectDescription(),
                context.researchAim(),
                context.objectives(),
                context.researchQuestions(),
                context.problemStatement(),
                context.studyArea(),
                context.researchType(),
                context.methodologySummary(),
                context.findingsCount(),
                context.analysisRunsCount(),
                context.templateRequirements(),
                context.customInstructions(),
                context.selectedSourceScope(),
                figureContexts
        );

        String prompt = promptService.build(promptContext);
        String retrievalQuery = promptService.buildRetrievalQuery(promptContext);

        // 4. Source Scope selection
        Set<UUID> documentIds = resolveDocumentScope(context, policy);

        SubmitRagQueryRequest ragRequest = new SubmitRagQueryRequest(
                prompt,
                documentIds.isEmpty() ? RetrievalScopeType.PROJECT_ALL_DOCUMENTS : RetrievalScopeType.SELECTED_DOCUMENTS,
                documentIds.isEmpty() ? null : documentIds,
                context.evidenceLimit() == null || context.evidenceLimit() <= 0 ? 10 : context.evidenceLimit(),
                retrievalQuery
        );

        // 5. Staged Literature Review vs standard project RAG
        GroundedAnswerResponse answer = (purpose == SectionSemanticPurpose.LITERATURE_REVIEW)
                ? ragQueryService.submitLiteratureReviewForProject(
                        context.projectId(),
                        user,
                        ragRequest,
                        "Generate " + context.sectionTitle()
                )
                : ragQueryService.submitForProject(
                        context.projectId(),
                        user,
                        ragRequest,
                        "Generate " + context.sectionTitle()
                );

        String mode = context.applyMode() != null && !context.applyMode().isBlank()
                ? context.applyMode().toUpperCase(Locale.ROOT)
                : "REPLACE";

        if ("PREVIEW".equals(mode) || context.targetSectionId() == null) {
            return new GeneratedDraftResponse(
                    answer.answer(),
                    answer.citations() != null ? answer.citations().stream().map(CitationResponse::documentId).distinct().toList() : List.of(),
                    "PREVIEW",
                    answer.citations(),
                    answer.retrievalSummary(),
                    OffsetDateTime.now()
            );
        } else if ("APPEND".equals(mode)) {
            return persistGeneratedSectionAppend(context.targetSectionId(), answer, user);
        } else {
            return persistGeneratedSection(context.targetSectionId(), answer, user);
        }
    }

    private void validateEvidenceGating(UUID projectId, SectionSemanticPurpose purpose) {
        long evidenceCount = evidenceRepository != null ? evidenceRepository.countByProjectId(projectId) : 0;
        if (purpose == SectionSemanticPurpose.TESTING) {
            long runs = runRepository.countByProjectId(projectId);
            if (runs == 0 && evidenceCount == 0) {
                throw new InsufficientProjectEvidenceException(
                        "Testing section generation requires actual test execution records, analysis runs, or project test evidence. Please record test runs or upload test evidence before generating this section."
                );
            }
        } else if (purpose == SectionSemanticPurpose.FINDINGS) {
            long findings = findingRepository.countByProjectId(projectId);
            long results = resultRepository.countByProjectId(projectId);
            if (findings == 0 && results == 0 && evidenceCount == 0) {
                throw new InsufficientProjectEvidenceException(
                        "Findings section generation requires actual analysis results, research findings, or project evidence. Please record analysis results, findings, or figures first."
                );
            }
        } else if (purpose == SectionSemanticPurpose.DISCUSSION) {
            long findings = findingRepository.countByProjectId(projectId);
            if (findings == 0 && evidenceCount == 0) {
                throw new InsufficientProjectEvidenceException(
                        "Discussion section requires actual empirical findings or project evidence to interpret. Please record research findings or evidence first."
                );
            }
        } else if (purpose == SectionSemanticPurpose.CONCLUSIONS) {
            long conclusions = conclusionRepository.countByProjectId(projectId);
            long findings = findingRepository.countByProjectId(projectId);
            if (conclusions == 0 && findings == 0 && evidenceCount == 0) {
                throw new InsufficientProjectEvidenceException(
                        "Conclusions section requires actual findings, conclusions, or project evidence. Please record findings or evidence first."
                );
            }
        }
    }

    private Set<UUID> resolveDocumentScope(AcademicSectionGenerationContext context, SectionGenerationPolicy policy) {
        Set<UUID> documentIds;
        if (policy == SectionGenerationPolicy.SOURCE_GROUNDED_AI) {
            documentIds = context.selectedDocumentIds() == null
                    ? Set.of()
                    : new LinkedHashSet<>(context.selectedDocumentIds());
            if (documentIds.isEmpty() && context.projectId() != null) {
                documentIds = citationEnabledDocumentIds(context.projectId());
            }
        } else {
            documentIds = context.selectedDocumentIds() != null
                    ? new LinkedHashSet<>(context.selectedDocumentIds())
                    : Set.of();
        }
        return documentIds;
    }

    private Set<UUID> citationEnabledDocumentIds(UUID projectId) {
        return new LinkedHashSet<>(documentRepository.findAllByProjectIdAndStatus(projectId, DocumentStatus.READY, org.springframework.data.domain.Pageable.unpaged())
                .getContent()
                .stream()
                .filter(doc -> doc.getCurrentVersion() != null)
                .map(com.researchassistant.document.entity.Document::getId)
                .toList());
    }

    private GeneratedDraftResponse persistGeneratedSection(UUID sectionId, GroundedAnswerResponse answer, User user) {
        return transactionTemplate.execute(status -> {
            ResearchReportSection managedSection = sectionRepository.findByIdForUpdate(sectionId)
                    .orElseThrow(() -> new ResourceNotFoundException("Report section not found."));
            String normalizedMarkdown = markdownRenderer.normalizeSectionMarkdown(managedSection.getHeading(), answer.answer());
            Map<UUID, ReportRichTextService.FigureRenderData> figureRenderData = figureRenderDataForSection(managedSection);
            if (generatedFigureService != null) {
                AcademicGeneratedFigureService.FigureProcessingResult processed =
                        generatedFigureService.process(managedSection, normalizedMarkdown, user);
                normalizedMarkdown = processed.markdown();
                figureRenderData = new java.util.LinkedHashMap<>(figureRenderData);
                figureRenderData.putAll(processed.figures());
            }
            managedSection.setContent(normalizedMarkdown);
            managedSection.setContentJson(richTextService.markdownToDocumentJson(normalizedMarkdown, figureRenderData));
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
                    answer.citations().stream().map(CitationResponse::documentId).distinct().toList(),
                    "SOURCE_GROUNDED_RAG",
                    answer.citations(),
                    answer.retrievalSummary(),
                    managedSection.getUpdatedAt()
            );
        });
    }

    private GeneratedDraftResponse persistGeneratedSectionAppend(UUID sectionId, GroundedAnswerResponse answer, User user) {
        return transactionTemplate.execute(status -> {
            ResearchReportSection managedSection = sectionRepository.findByIdForUpdate(sectionId)
                    .orElseThrow(() -> new ResourceNotFoundException("Report section not found."));
            String existing = managedSection.getContent() != null ? managedSection.getContent() : "";
            String normalizedNew = markdownRenderer.normalizeSectionMarkdown(managedSection.getHeading(), answer.answer());
            Map<UUID, ReportRichTextService.FigureRenderData> figureRenderData = figureRenderDataForSection(managedSection);
            if (generatedFigureService != null) {
                AcademicGeneratedFigureService.FigureProcessingResult processed =
                        generatedFigureService.process(managedSection, normalizedNew, user);
                normalizedNew = processed.markdown();
                figureRenderData = new java.util.LinkedHashMap<>(figureRenderData);
                figureRenderData.putAll(processed.figures());
            }
            String appended = existing.isBlank() ? normalizedNew : existing + "\n\n" + normalizedNew;
            managedSection.setContent(appended);
            managedSection.setContentJson(richTextService.markdownToDocumentJson(appended, figureRenderData));
            managedSection.setPlainText(richTextService.plainTextFromDocumentJson(managedSection.getContentJson()));
            managedSection.setStatus(ReportSectionStatus.DRAFT);
            managedSection.setOrigin(ContentOrigin.AI_GENERATED);
            managedSection.setUpdatedBy(user);
            managedSection.setRevisionNumber(managedSection.getRevisionNumber() + 1);
            sectionRepository.save(managedSection);
            if (managedSection.resolveGenerationPolicy() == SectionGenerationPolicy.SOURCE_GROUNDED_AI) {
                persistSectionCitations(managedSection, answer, user);
            }
            auditService.record(user.getId(), SecurityAuditEventType.REPORT_SECTION_UPDATED);
            return new GeneratedDraftResponse(
                    managedSection.getContent(),
                    answer.citations().stream().map(CitationResponse::documentId).distinct().toList(),
                    "SOURCE_GROUNDED_RAG_APPEND",
                    answer.citations(),
                    answer.retrievalSummary(),
                    managedSection.getUpdatedAt()
            );
        });
    }

    private Map<UUID, ReportRichTextService.FigureRenderData> figureRenderDataForSection(ResearchReportSection section) {
        if (evidenceRepository == null || numberingService == null || section == null || section.getId() == null) {
            return Map.of();
        }
        Map<UUID, String> labels = section.getChapter() != null && section.getChapter().getReport() != null
                ? numberingService.computeDynamicLabelsForReport(section.getChapter().getReport().getId())
                : Map.of();
        Map<UUID, ReportRichTextService.FigureRenderData> data = new java.util.LinkedHashMap<>();
        for (com.researchassistant.evidence.entity.ProjectEvidence e : evidenceRepository.findAllBySectionIdOrderByDisplayOrderAscCreatedAtAsc(section.getId())) {
            String label = labels.getOrDefault(e.getId(), e.getFigureLabel() != null ? e.getFigureLabel() : "Figure");
            String src = e.getStorageObject() != null ? "/api/v1/storage-objects/" + e.getStorageObject().getId() + "/download" : "";
            data.put(e.getId(), new ReportRichTextService.FigureRenderData(
                    e.getId(), label, e.getCaption(), src, e.getAltText(), e.getStructuredDefinition(), "MERMAID"
            ));
        }
        return data;
    }

    private void persistSectionCitations(ResearchReportSection section, GroundedAnswerResponse answer, User user) {
        citationRepository.deleteAllBySectionId(section.getId());
        citationRepository.flush();
        List<RagQueryEvidence> evidence = ragEvidenceRepository.findWithTraceByQueryIdOrderByEvidenceOrdinalAsc(answer.queryId());
        List<ResearchReportCitation> citations = new ArrayList<>();
        int ordinal = 1;
        for (CitationResponse renderedCitation : answer.citations()) {
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
}
