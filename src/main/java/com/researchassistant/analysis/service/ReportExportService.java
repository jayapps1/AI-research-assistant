package com.researchassistant.analysis.service;

import com.researchassistant.analysis.dto.AnalysisDtos.ReportValidationResponse;
import com.researchassistant.analysis.controller.ReportExportController.DocumentExportSelection;
import com.researchassistant.analysis.entity.*;
import com.researchassistant.analysis.exception.ReportValidationException;
import com.researchassistant.analysis.repository.*;
import com.researchassistant.common.storage.StorageObjectCategory;
import com.researchassistant.common.storage.StorageObjectEntity;
import com.researchassistant.common.storage.StorageObjectMetadataService;
import com.researchassistant.common.storage.StoredObject;
import com.researchassistant.common.exception.ResourceNotFoundException;
import com.researchassistant.document.storage.*;
import com.researchassistant.identity.entity.User;
import com.researchassistant.project.service.ProjectAuthorizationService;
import com.researchassistant.literature.repository.LiteratureMatrixRepository;
import com.researchassistant.reference.dto.ReferenceDtos.CitationContext;
import com.researchassistant.reference.entity.ProjectReference;
import com.researchassistant.reference.entity.ReferenceEntry;
import com.researchassistant.reference.repository.ProjectReferenceRepository;
import com.researchassistant.reference.service.CitationFormattingService;
import com.researchassistant.security.audit.SecurityAuditEventType;
import com.researchassistant.security.audit.SecurityAuditService;
import com.researchassistant.subscription.PlanFeature;
import com.researchassistant.usage.QuotaService;
import com.researchassistant.usage.UsageMetricType;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.poi.xwpf.model.XWPFHeaderFooterPolicy;
import org.apache.poi.xwpf.usermodel.*;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTR;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTP;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTSectPr;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.STFldCharType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.io.*;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class ReportExportService {
    private final ReportExportJobRepository exportJobRepository;
    private final ResearchReportRepository reportRepository;
    private final ResearchReportChapterRepository chapterRepository;
    private final ResearchReportSectionRepository sectionRepository;
    private final ResearchReportCitationRepository citationRepository;
    private final CitationFormattingService citationFormattingService;
    private final ReportMarkdownRenderer markdownRenderer;
    private final ReportRichTextService richTextService;
    private final DocumentStorageService storageService;
    private final ProjectAuthorizationService authorizationService;
    private final SecurityAuditService auditService;
    private final QuotaService quotaService;
    private final ReportDocumentVersionRepository documentVersionRepository;
    private final LiteratureMatrixRepository literatureMatrixRepository;
    private final ProjectReferenceRepository projectReferenceRepository;
    private final org.springframework.beans.factory.ObjectProvider<AnalysisWorkflowService> workflowServiceProvider;
    private final StorageObjectMetadataService storageObjectMetadataService;
    private final com.researchassistant.evidence.repository.ProjectEvidenceRepository evidenceRepository;
    private final com.researchassistant.evidence.service.ProjectEvidenceNumberingService evidenceNumberingService;
    private final com.researchassistant.evidence.service.ProjectEvidenceListService evidenceListService;
    private final com.researchassistant.common.storage.ObjectStorageService objectStorageService;
    private final ReportDocumentCompiler reportDocumentCompiler;

    public ReportExportService(ReportExportJobRepository exportJobRepository, ResearchReportRepository reportRepository,
            ResearchReportChapterRepository chapterRepository, ResearchReportSectionRepository sectionRepository,
            ResearchReportCitationRepository citationRepository, CitationFormattingService citationFormattingService,
            ReportMarkdownRenderer markdownRenderer, ReportRichTextService richTextService,
            DocumentStorageService storageService, ProjectAuthorizationService authorizationService, SecurityAuditService auditService,
            QuotaService quotaService, ReportDocumentVersionRepository documentVersionRepository,
            LiteratureMatrixRepository literatureMatrixRepository, ProjectReferenceRepository projectReferenceRepository,
            org.springframework.beans.factory.ObjectProvider<AnalysisWorkflowService> workflowServiceProvider,
            StorageObjectMetadataService storageObjectMetadataService) {
        this(exportJobRepository, reportRepository, chapterRepository, sectionRepository,
                citationRepository, citationFormattingService, markdownRenderer, richTextService,
                storageService, authorizationService, auditService, quotaService,
                documentVersionRepository, literatureMatrixRepository, projectReferenceRepository,
                workflowServiceProvider, storageObjectMetadataService, null, null, null, null, null);
    }

    @org.springframework.beans.factory.annotation.Autowired
    public ReportExportService(ReportExportJobRepository exportJobRepository, ResearchReportRepository reportRepository,
            ResearchReportChapterRepository chapterRepository, ResearchReportSectionRepository sectionRepository,
            ResearchReportCitationRepository citationRepository, CitationFormattingService citationFormattingService,
            ReportMarkdownRenderer markdownRenderer, ReportRichTextService richTextService,
            DocumentStorageService storageService, ProjectAuthorizationService authorizationService, SecurityAuditService auditService,
            QuotaService quotaService, ReportDocumentVersionRepository documentVersionRepository,
            LiteratureMatrixRepository literatureMatrixRepository, ProjectReferenceRepository projectReferenceRepository,
            org.springframework.beans.factory.ObjectProvider<AnalysisWorkflowService> workflowServiceProvider,
            StorageObjectMetadataService storageObjectMetadataService,
            com.researchassistant.evidence.repository.ProjectEvidenceRepository evidenceRepository,
            com.researchassistant.evidence.service.ProjectEvidenceNumberingService evidenceNumberingService,
            com.researchassistant.evidence.service.ProjectEvidenceListService evidenceListService,
            com.researchassistant.common.storage.ObjectStorageService objectStorageService,
            ReportDocumentCompiler reportDocumentCompiler) {
        this.exportJobRepository = exportJobRepository;
        this.reportRepository = reportRepository;
        this.chapterRepository = chapterRepository;
        this.sectionRepository = sectionRepository;
        this.citationRepository = citationRepository;
        this.citationFormattingService = citationFormattingService;
        this.markdownRenderer = markdownRenderer;
        this.richTextService = richTextService;
        this.storageService = storageService;
        this.authorizationService = authorizationService;
        this.auditService = auditService;
        this.quotaService = quotaService;
        this.documentVersionRepository = documentVersionRepository;
        this.literatureMatrixRepository = literatureMatrixRepository;
        this.projectReferenceRepository = projectReferenceRepository;
        this.workflowServiceProvider = workflowServiceProvider;
        this.storageObjectMetadataService = storageObjectMetadataService;
        this.evidenceRepository = evidenceRepository;
        this.evidenceNumberingService = evidenceNumberingService;
        this.evidenceListService = evidenceListService;
        this.objectStorageService = objectStorageService;
        this.reportDocumentCompiler = reportDocumentCompiler;
    }

    @Transactional
    public ReportExportJob createExport(UUID reportId, ReportExportFormat format, User user) {
        return createExport(reportId, format, false, user);
    }

    @Transactional
    public ReportExportJob createExport(UUID reportId, ReportExportFormat format, boolean isDraft, User user) {
        return createExport(reportId, format, isDraft, null, user);
    }

    @Transactional
    public ReportExportJob createExport(UUID reportId, ReportExportFormat format, boolean isDraft, DocumentExportSelection selection, User user) {
        ResearchReport report = reportRepository.findById(reportId).orElseThrow(() -> new ResourceNotFoundException("Report not found."));
        authorizationService.requireProjectEditor(report.getProject().getId(), user);
        if (format != ReportExportFormat.DOCX && format != ReportExportFormat.PDF) throw new IllegalArgumentException("Only DOCX and PDF exports are implemented.");
        ReportDocumentCompiler.DocumentCompilationScope compilationScope = toCompilationScope(report, selection);
        boolean selectedScope = compilationScope.mode() == ReportDocumentCompiler.CompilationMode.SELECTED;

        if (!isDraft && !selectedScope) {
            AnalysisWorkflowService workflowService = workflowServiceProvider.getIfAvailable();
            if (workflowService != null) {
                ReportValidationResponse validation = workflowService.validateReport(reportId, user);
                if (!validation.errors().isEmpty()) {
                    throw new ReportValidationException(validation);
                }
            }
        }

        quotaService.requireWithinQuota(report.getProject().getWorkspace().getId(),
                format == ReportExportFormat.PDF ? PlanFeature.REPORT_EXPORT_PDF : PlanFeature.REPORT_EXPORT_DOCX,
                UsageMetricType.REPORT_EXPORT, 1L);
        ReportExportJob job = new ReportExportJob();
        job.setReport(report);
        job.setFormat(format);
        job.setStatus(ReportExportStatus.RUNNING);
        job.setFilename(filename(report, format, isDraft, compilationScope));
        job.setMimeType(format == ReportExportFormat.DOCX ? "application/vnd.openxmlformats-officedocument.wordprocessingml.document" : "application/pdf");
        job.setReportRevisionNumber(report.getRevisionNumber());
        job.setCitationStyle(report.getCitationStyle());
        job.setTemplateId(report.getTemplate() == null ? null : report.getTemplate().getId());
        job.setStyleConfigurationSnapshot(defaultStyleJson());
        job.setRequestedBy(user);
        job.setStartedAt(OffsetDateTime.now());

        ReportDocumentVersion finalVersion = null;
        if (!isDraft && !selectedScope) {
            finalVersion = documentVersionRepository.findFirstByReportIdOrderByVersionNumberDesc(reportId)
                    .orElseThrow(() -> new IllegalStateException("Prepare a final document snapshot before final export."));
            if (finalVersion.getSourceReportRevisionNumber() != report.getRevisionNumber()) {
                throw new IllegalStateException("REPORT_VERSION_CONFLICT: final document is out of date. Update the final document before publication export.");
            }
            job.setFinalDocumentVersion(finalVersion);
        } else {
            documentVersionRepository.findFirstByReportIdOrderByVersionNumberDesc(reportId)
                    .ifPresent(job::setFinalDocumentVersion);
        }

        exportJobRepository.save(job);
        auditService.record(user.getId(), SecurityAuditEventType.REPORT_EXPORT_REQUESTED);
        try {
            ReportDocumentCompiler.CompiledAcademicDocument draftCompiled = (isDraft || selectedScope) && reportDocumentCompiler != null
                    ? reportDocumentCompiler.compile(report, compilationScope)
                    : null;
            byte[] bytes = format == ReportExportFormat.DOCX
                    ? ((isDraft || selectedScope) && draftCompiled != null ? renderDocx(draftCompiled, report, true) : renderDocx(finalVersion, report))
                    : ((isDraft || selectedScope) && draftCompiled != null ? renderPdf(draftCompiled, report, true) : renderPdf(finalVersion, report));
            String storageKey = "projects/" + report.getProject().getId() + "/exports/" + job.getId() + "/" + job.getFilename();
            StorageObjectEntity pendingStorageObject = storageObjectMetadataService.createPending(
                    user.getId(),
                    report.getProject().getWorkspace().getId(),
                    report.getProject().getId(),
                    StorageObjectCategory.REPORT_EXPORT,
                    storageKey,
                    job.getFilename(),
                    job.getFilename(),
                    job.getMimeType(),
                    bytes.length,
                    null
            );
            StoredDocumentObject stored;
            StorageObjectEntity availableStorageObject;
            try {
                stored = storageService.store(storageKey, new ByteArrayInputStream(bytes), job.getMimeType());
                availableStorageObject = storageObjectMetadataService.markAvailable(
                        pendingStorageObject.getId(),
                        new StoredObject(
                                stored.storageKey(),
                                stored.fileSizeBytes(),
                                stored.checksumSha256(),
                                stored.providerFileId(),
                                stored.providerParentId()
                        )
                );
            } catch (RuntimeException storageException) {
                storageObjectMetadataService.markFailed(
                        pendingStorageObject.getId(),
                        storageFailureCode(storageException),
                        storageException.getMessage()
                );
                throw storageException;
            }
            job.setStorageKey(stored.storageKey());
            job.setStorageObject(availableStorageObject);
            job.setFileSizeBytes(stored.fileSizeBytes());
            job.setChecksumSha256(stored.checksumSha256());
            job.setStatus(ReportExportStatus.COMPLETED);
            job.setCompletedAt(OffsetDateTime.now());
            auditService.record(user.getId(), SecurityAuditEventType.REPORT_EXPORT_COMPLETED);
        } catch (Exception exception) {
            job.setStatus(ReportExportStatus.FAILED);
            job.setErrorCode("EXPORT_FAILED");
            job.setErrorMessage("Report export failed: " + exception.getMessage());
            auditService.record(user.getId(), SecurityAuditEventType.REPORT_EXPORT_FAILED);
        }
        return job;
    }

    @Transactional(readOnly = true)
    public ReportDocumentCompiler.CompiledAcademicDocument preview(UUID reportId, DocumentExportSelection selection, User user) {
        ResearchReport report = reportRepository.findById(reportId).orElseThrow(() -> new ResourceNotFoundException("Report not found."));
        authorizationService.requireProjectViewer(report.getProject().getId(), user);
        return reportDocumentCompiler.compile(report, toCompilationScope(report, selection));
    }

    @Transactional(readOnly = true)
    public ReportExportJob get(UUID exportId, User user) {
        ReportExportJob job = exportJobRepository.findById(exportId).orElseThrow(() -> new ResourceNotFoundException("Report export not found."));
        authorizationService.requireProjectViewer(job.getReport().getProject().getId(), user);
        return job;
    }

    @Transactional(readOnly = true)
    public DocumentStorageObject download(UUID exportId, User user) {
        ReportExportJob job = get(exportId, user);
        if (job.getStatus() != ReportExportStatus.COMPLETED || job.getStorageKey() == null) throw new IllegalStateException("Export is not available for download.");
        return storageService.open(job.getStorageKey());
    }

    private byte[] renderDocx(ResearchReport report) throws IOException {
        return renderDocx(report, false);
    }

    public record FormatProfile(
            String pageSize,
            String orientation,
            double topMarginInches,
            double rightMarginInches,
            double bottomMarginInches,
            double leftMarginInches,
            String defaultFont,
            int bodyFontSize,
            String headingFont,
            double lineSpacing,
            int paragraphSpacingAfterPt,
            String alignment,
            double firstLineIndentInches,
            String chapterStart,
            String frontMatterNumbering,
            String mainContentNumbering,
            String citationStyle
    ) {
        public static FormatProfile defaultProfile() {
            return new FormatProfile("A4", "PORTRAIT", 1.0, 1.0, 1.0, 1.25,
                    "Times New Roman", 12, "Times New Roman", 1.5, 6, "JUSTIFY", 0.5,
                    "NEW_PAGE", "ROMAN_LOWER", "ARABIC", "APA_7");
        }

        public static FormatProfile fromTemplate(ResearchReportTemplate template) {
            if (template == null || template.getConfigurationJson() == null || template.getConfigurationJson().isBlank()) {
                return defaultProfile();
            }
            try {
                com.fasterxml.jackson.databind.JsonNode root = new com.fasterxml.jackson.databind.ObjectMapper().readTree(template.getConfigurationJson());
                com.fasterxml.jackson.databind.JsonNode fp = root.get("formatProfile");
                if (fp == null || !fp.isObject()) {
                    return defaultProfile();
                }
                String pageSize = fp.has("pageSize") ? fp.get("pageSize").asText("A4") : "A4";
                String orientation = fp.has("orientation") ? fp.get("orientation").asText("PORTRAIT") : "PORTRAIT";
                double top = 1.0, right = 1.0, bottom = 1.0, left = 1.25;
                if (fp.has("margins") && fp.get("margins").isObject()) {
                    var m = fp.get("margins");
                    top = m.has("topInches") ? m.get("topInches").asDouble(1.0) : 1.0;
                    right = m.has("rightInches") ? m.get("rightInches").asDouble(1.0) : 1.0;
                    bottom = m.has("bottomInches") ? m.get("bottomInches").asDouble(1.0) : 1.0;
                    left = m.has("leftInches") ? m.get("leftInches").asDouble(1.25) : 1.25;
                }
                String defaultFont = fp.has("defaultFont") ? fp.get("defaultFont").asText("Times New Roman") : "Times New Roman";
                int bodyFontSize = fp.has("bodyFontSize") ? fp.get("bodyFontSize").asInt(12) : 12;
                String headingFont = fp.has("headingFont") ? fp.get("headingFont").asText("Times New Roman") : "Times New Roman";
                double lineSpacing = fp.has("lineSpacing") ? fp.get("lineSpacing").asDouble(1.5) : 1.5;
                int paragraphSpacingAfterPt = fp.has("paragraphSpacingAfterPt") ? fp.get("paragraphSpacingAfterPt").asInt(6) : 6;
                String alignment = fp.has("alignment") ? fp.get("alignment").asText("JUSTIFY") : "JUSTIFY";
                double firstLineIndentInches = fp.has("firstLineIndentInches") ? fp.get("firstLineIndentInches").asDouble(0.5) : 0.5;
                String chapterStart = fp.has("chapterStart") ? fp.get("chapterStart").asText("NEW_PAGE") : "NEW_PAGE";
                String frontNumbering = "ROMAN_LOWER";
                String mainNumbering = "ARABIC";
                if (fp.has("pageNumbering") && fp.get("pageNumbering").isObject()) {
                    frontNumbering = fp.get("pageNumbering").has("frontMatter") ? fp.get("pageNumbering").get("frontMatter").asText("ROMAN_LOWER") : "ROMAN_LOWER";
                    mainNumbering = fp.get("pageNumbering").has("mainContent") ? fp.get("pageNumbering").get("mainContent").asText("ARABIC") : "ARABIC";
                }
                String citationStyle = fp.has("citationStyle") ? fp.get("citationStyle").asText("APA_7") : "APA_7";
                return new FormatProfile(pageSize, orientation, top, right, bottom, left, defaultFont, bodyFontSize,
                        headingFont, lineSpacing, paragraphSpacingAfterPt, alignment, firstLineIndentInches, chapterStart,
                        frontNumbering, mainNumbering, citationStyle);
            } catch (Exception ignored) {
                return defaultProfile();
            }
        }
    }

    private void applyDocxFormatProfile(XWPFDocument doc, FormatProfile profile) {
        try {
            CTSectPr sectPr = doc.getDocument().getBody().isSetSectPr()
                    ? doc.getDocument().getBody().getSectPr()
                    : doc.getDocument().getBody().addNewSectPr();
            org.openxmlformats.schemas.wordprocessingml.x2006.main.CTPageMar pageMar = sectPr.isSetPgMar()
                    ? sectPr.getPgMar()
                    : sectPr.addNewPgMar();
            pageMar.setTop(java.math.BigInteger.valueOf((long) (profile.topMarginInches() * 1440)));
            pageMar.setRight(java.math.BigInteger.valueOf((long) (profile.rightMarginInches() * 1440)));
            pageMar.setBottom(java.math.BigInteger.valueOf((long) (profile.bottomMarginInches() * 1440)));
            pageMar.setLeft(java.math.BigInteger.valueOf((long) (profile.leftMarginInches() * 1440)));
        } catch (Exception ignored) {
        }
    }

    private boolean isCourseworkReport(ResearchReport report) {
        if (report == null) return false;
        if (report.getType() == ResearchReportType.COURSEWORK) return true;
        return report.getProject() != null
                && report.getProject().getWorkspaceType() == com.researchassistant.project.entity.AcademicWorkspaceType.COURSEWORK;
    }

    private byte[] renderDocx(ResearchReport report, boolean isDraft) throws IOException {
        FormatProfile profile = FormatProfile.fromTemplate(report.getTemplate());
        try (XWPFDocument doc = new XWPFDocument(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            applyDocxFormatProfile(doc, profile);
            enableFieldUpdateOnOpen(doc);
            addPageNumberFooter(doc);
            Map<UUID, Integer> referenceNumbers = citationNumberMap(report);
            writeTitlePage(doc, report);
            if (isDraft) {
                XWPFParagraph draftPara = doc.createParagraph();
                draftPara.setAlignment(ParagraphAlignment.CENTER);
                XWPFRun draftRun = draftPara.createRun();
                draftRun.setText("*** DRAFT MANUSCRIPT - FOR REVIEW ONLY ***");
                draftRun.setBold(true);
                draftRun.setColor("CC0000");
                draftRun.setFontSize(14);
            }
            writeDocxAbstract(doc, report);
            writeDocxTableOfContents(doc);
            writeDocxListOfFigures(doc, report);
            writeDocxListOfTables(doc, report);
            Map<UUID, String> evidenceLabels = evidenceNumberingService != null
                    ? evidenceNumberingService.computeDynamicLabelsForReport(report.getId())
                    : Map.of();
            boolean coursework = isCourseworkReport(report);
            for (ResearchReportChapter chapter : chapterRepository.findAllByReportIdOrderByDisplayOrderAsc(report.getId())) {
                if (chapter.getType() == ReportChapterType.PRELIMINARY || chapter.getType() == ReportChapterType.REFERENCES || chapter.getType() == ReportChapterType.APPENDICES) {
                    continue;
                }
                if (!coursework) {
                    writeDocxHeading(doc, chapter.getTitle(), 1, true);
                }
                List<ResearchReportSection> rootSections = sectionRepository.findAllByChapterIdAndParentSectionIsNullOrderByDisplayOrderAsc(chapter.getId());
                for (ResearchReportSection section : rootSections) {
                    String heading = section.getSectionNumber() != null ? section.getSectionNumber() + " " + section.getHeading() : section.getHeading();
                    writeDocxHeading(doc, heading, coursework ? 1 : 2, coursework);
                    markdownRenderer.renderMarkdown(doc, exportText(section, report.getCitationStyle(), referenceNumbers), section.getHeading());
                    writeDocxSectionEvidence(doc, section, evidenceLabels);

                    List<ResearchReportSection> subsections = sectionRepository.findAllByParentSectionIdOrderByDisplayOrderAsc(section.getId());
                    for (ResearchReportSection sub : subsections) {
                        String subHeading = sub.getSectionNumber() != null ? sub.getSectionNumber() + " " + sub.getHeading() : sub.getHeading();
                        writeDocxHeading(doc, subHeading, coursework ? 2 : 3, false);
                        markdownRenderer.renderMarkdown(doc, exportText(sub, report.getCitationStyle(), referenceNumbers), sub.getHeading());
                        writeDocxSectionEvidence(doc, sub, evidenceLabels);

                        List<ResearchReportSection> subSubs = sectionRepository.findAllByParentSectionIdOrderByDisplayOrderAsc(sub.getId());
                        for (ResearchReportSection subSub : subSubs) {
                            String subSubHeading = subSub.getSectionNumber() != null ? subSub.getSectionNumber() + " " + subSub.getHeading() : subSub.getHeading();
                            writeDocxHeading(doc, subSubHeading, coursework ? 3 : 4, false);
                            markdownRenderer.renderMarkdown(doc, exportText(subSub, report.getCitationStyle(), referenceNumbers), subSub.getHeading());
                            writeDocxSectionEvidence(doc, subSub, evidenceLabels);
                        }
                    }
                }

                if (chapter.getType() == ReportChapterType.LITERATURE_REVIEW && "CHAPTER_TWO".equalsIgnoreCase(report.getLiteratureMatrixInclusion())) {
                    literatureMatrixRepository.findFirstByProjectIdOrderByCreatedAtDesc(report.getProject().getId())
                            .ifPresent(matrix -> {
                                if (matrix.getMarkdownTable() != null && !matrix.getMarkdownTable().isBlank()) {
                                    writeDocxHeading(doc, "Literature Evidence Assessment Matrix", 2, false);
                                    markdownRenderer.renderMarkdown(doc, matrix.getMarkdownTable(), "Literature Evidence Assessment Matrix");
                                }
                            });
                }
            }
            writeReferences(doc, report, referenceNumbers);
            writeAppendices(doc, report, referenceNumbers);
            doc.write(out);
            return out.toByteArray();
        }
    }

    private byte[] renderDocx(ReportDocumentVersion version, ResearchReport report) throws IOException {
        if (version == null) {
            throw new IllegalStateException("Final document snapshot is required for final DOCX export.");
        }
        FormatProfile profile = FormatProfile.fromTemplate(report.getTemplate());
        try (XWPFDocument doc = new XWPFDocument(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            applyDocxFormatProfile(doc, profile);
            enableFieldUpdateOnOpen(doc);
            addPageNumberFooter(doc);
            String markdown = finalVersionMarkdown(version);
            markdownRenderer.renderMarkdown(doc, markdown, null);
            doc.write(out);
            return out.toByteArray();
        }
    }

    private byte[] renderDocx(ReportDocumentCompiler.CompiledAcademicDocument compiled, ResearchReport report, boolean isDraft) throws IOException {
        FormatProfile profile = FormatProfile.fromTemplate(report.getTemplate());
        try (XWPFDocument doc = new XWPFDocument(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            applyDocxFormatProfile(doc, profile);
            enableFieldUpdateOnOpen(doc);
            addPageNumberFooter(doc);
            if (isDraft) {
                XWPFParagraph draftPara = doc.createParagraph();
                draftPara.setAlignment(ParagraphAlignment.CENTER);
                XWPFRun draftRun = draftPara.createRun();
                draftRun.setText("*** DRAFT MANUSCRIPT - FOR REVIEW ONLY ***");
                draftRun.setBold(true);
                draftRun.setColor("CC0000");
                draftRun.setFontSize(14);
            }
            markdownRenderer.renderMarkdown(doc, compiled.markdown(), null);
            doc.write(out);
            return out.toByteArray();
        }
    }

    private byte[] renderPdf(ResearchReport report) throws IOException {
        return renderPdf(report, false);
    }

    private byte[] renderPdf(ResearchReport report, boolean isDraft) throws IOException {
        try (PDDocument doc = new PDDocument(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            PDType1Font font = new PDType1Font(Standard14Fonts.FontName.TIMES_ROMAN);
            PdfCursor cursor = new PdfCursor(doc, font);
            Map<UUID, Integer> referenceNumbers = citationNumberMap(report);
            float y = pdfLine(cursor, 16, 60, cursor.y, report.getTitle());
            if (isDraft) {
                y = pdfLine(cursor, 12, 60, y - 5, "[ DRAFT MANUSCRIPT - FOR REVIEW ONLY ]");
            }
            y = writePdfAbstract(report, cursor, y);
            y = writePdfListOfFigures(report, cursor, y);
            y = writePdfListOfTables(report, cursor, y);
            Map<UUID, String> evidenceLabelsPdf = evidenceNumberingService != null
                    ? evidenceNumberingService.computeDynamicLabelsForReport(report.getId())
                    : Map.of();
            boolean coursework = isCourseworkReport(report);
            for (ResearchReportChapter chapter : chapterRepository.findAllByReportIdOrderByDisplayOrderAsc(report.getId())) {
                if (chapter.getType() == ReportChapterType.PRELIMINARY || chapter.getType() == ReportChapterType.REFERENCES || chapter.getType() == ReportChapterType.APPENDICES) {
                    continue;
                }
                if (!coursework) {
                    y = ensurePage(cursor, y);
                    y = pdfLine(cursor, 14, 60, y - 10, chapter.getTitle());
                }
                List<ResearchReportSection> rootSections = sectionRepository.findAllByChapterIdAndParentSectionIsNullOrderByDisplayOrderAsc(chapter.getId());
                for (ResearchReportSection section : rootSections) {
                    y = ensurePage(cursor, y);
                    String heading = section.getSectionNumber() != null ? section.getSectionNumber() + " " + section.getHeading() : section.getHeading();
                    y = pdfLine(cursor, coursework ? 14 : 12, coursework ? 60 : 70, y - (coursework ? 10 : 0), heading);
                    for (String paragraph : markdownRenderer.plainLines(exportText(section, report.getCitationStyle(), referenceNumbers), section.getHeading())) {
                        for (String line : wrap(paragraph, 95)) {
                            y = ensurePage(cursor, y);
                            y = pdfLine(cursor, 11, 70, y, line);
                        }
                    }
                    y = writePdfSectionEvidence(doc, cursor, y, section, evidenceLabelsPdf);

                    List<ResearchReportSection> subsections = sectionRepository.findAllByParentSectionIdOrderByDisplayOrderAsc(section.getId());
                    for (ResearchReportSection sub : subsections) {
                        y = ensurePage(cursor, y);
                        String subHeading = sub.getSectionNumber() != null ? sub.getSectionNumber() + " " + sub.getHeading() : sub.getHeading();
                        y = pdfLine(cursor, coursework ? 12 : 11, coursework ? 65 : 75, y, subHeading);
                        for (String paragraph : markdownRenderer.plainLines(exportText(sub, report.getCitationStyle(), referenceNumbers), sub.getHeading())) {
                            for (String line : wrap(paragraph, 93)) {
                                y = ensurePage(cursor, y);
                                y = pdfLine(cursor, 10, 75, y, line);
                            }
                        }
                        y = writePdfSectionEvidence(doc, cursor, y, sub, evidenceLabelsPdf);

                        List<ResearchReportSection> subSubs = sectionRepository.findAllByParentSectionIdOrderByDisplayOrderAsc(sub.getId());
                        for (ResearchReportSection subSub : subSubs) {
                            y = ensurePage(cursor, y);
                            String subSubHeading = subSub.getSectionNumber() != null ? subSub.getSectionNumber() + " " + subSub.getHeading() : subSub.getHeading();
                            y = pdfLine(cursor, 11, 80, y, subSubHeading);
                            for (String paragraph : markdownRenderer.plainLines(exportText(subSub, report.getCitationStyle(), referenceNumbers), subSub.getHeading())) {
                                for (String line : wrap(paragraph, 91)) {
                                    y = ensurePage(cursor, y);
                                    y = pdfLine(cursor, 10, 80, y, line);
                                }
                            }
                            y = writePdfSectionEvidence(doc, cursor, y, subSub, evidenceLabelsPdf);
                        }
                    }
                }

                if (chapter.getType() == ReportChapterType.LITERATURE_REVIEW && "CHAPTER_TWO".equalsIgnoreCase(report.getLiteratureMatrixInclusion())) {
                    var matrixOpt = literatureMatrixRepository.findFirstByProjectIdOrderByCreatedAtDesc(report.getProject().getId());
                    if (matrixOpt.isPresent() && matrixOpt.get().getMarkdownTable() != null && !matrixOpt.get().getMarkdownTable().isBlank()) {
                        y = ensurePage(cursor, y);
                        y = pdfLine(cursor, 12, 70, y, "Literature Evidence Assessment Matrix");
                        for (String paragraph : markdownRenderer.plainLines(matrixOpt.get().getMarkdownTable(), "Matrix")) {
                            for (String line : wrap(paragraph, 95)) {
                                y = ensurePage(cursor, y);
                                y = pdfLine(cursor, 11, 70, y, line);
                            }
                        }
                    }
                }
            }
            y = ensurePage(cursor, y);
            y = pdfLine(cursor, 14, 60, y - 10, "References");
            int number = 1;
            for (ReferenceEntry reference : citedReferences(report).values()) {
                String text = citationFormattingService.format(reference, report.getCitationStyle(), CitationContext.REFERENCE_LIST, referenceNumbers.getOrDefault(reference.getId(), number++)).text();
                for (String line : wrap(text, 95)) {
                    y = ensurePage(cursor, y);
                    y = pdfLine(cursor, 11, 70, y, line);
                }
            }
            for (ResearchReportChapter chapter : chapterRepository.findAllByReportIdOrderByDisplayOrderAsc(report.getId())) {
                if (chapter.getType() != ReportChapterType.APPENDICES) {
                    continue;
                }
                List<ResearchReportSection> sections = sectionRepository.findAllByChapterIdOrderByDisplayOrderAsc(chapter.getId());
                if (sections.stream().allMatch(section -> section.getContent() == null || section.getContent().isBlank())) {
                    continue;
                }
                y = ensurePage(cursor, y);
                y = pdfLine(cursor, 14, 60, y - 10, chapter.getTitle());
                for (ResearchReportSection section : sections) {
                    y = ensurePage(cursor, y);
                    y = pdfLine(cursor, 12, 70, y, section.getHeading());
                    for (String paragraph : markdownRenderer.plainLines(exportText(section, report.getCitationStyle(), referenceNumbers), section.getHeading())) {
                        for (String line : wrap(paragraph, 95)) {
                            y = ensurePage(cursor, y);
                            y = pdfLine(cursor, 11, 70, y, line);
                        }
                    }
                }
            }
            cursor.close();
            doc.save(out);
            return out.toByteArray();
        }
    }

    private byte[] renderPdf(ReportDocumentVersion version, ResearchReport report) throws IOException {
        if (version == null) {
            throw new IllegalStateException("Final document snapshot is required for final PDF export.");
        }
        try (PDDocument doc = new PDDocument(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            PDType1Font font = new PDType1Font(Standard14Fonts.FontName.TIMES_ROMAN);
            PdfCursor cursor = new PdfCursor(doc, font);
            float y = cursor.y;
            List<ReportMarkdownRenderer.MarkdownBlock> blocks = markdownRenderer.plainBlocks(finalVersionMarkdown(version), null);
            for (ReportMarkdownRenderer.MarkdownBlock block : blocks) {
                if (block.headingLevel() == 1) {
                    y = ensurePage(cursor, y);
                    y = pdfLine(cursor, 14, 60, y - 10, block.text());
                } else if (block.headingLevel() == 2) {
                    y = ensurePage(cursor, y);
                    y = pdfLine(cursor, 12, 65, y - 6, block.text());
                } else if (block.headingLevel() >= 3) {
                    y = ensurePage(cursor, y);
                    y = pdfLine(cursor, 11, 70, y - 4, block.text());
                } else {
                    for (String line : wrap(block.text(), 95)) {
                        y = ensurePage(cursor, y);
                        y = pdfLine(cursor, 11, 70, y, line);
                    }
                }
            }
            cursor.close();
            doc.save(out);
            return out.toByteArray();
        }
    }

    private byte[] renderPdf(ReportDocumentCompiler.CompiledAcademicDocument compiled, ResearchReport report, boolean isDraft) throws IOException {
        try (PDDocument doc = new PDDocument(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            PDType1Font font = new PDType1Font(Standard14Fonts.FontName.TIMES_ROMAN);
            PdfCursor cursor = new PdfCursor(doc, font);
            float y = cursor.y;
            if (isDraft) {
                y = pdfLine(cursor, 12, 60, y, "[ DRAFT MANUSCRIPT - FOR REVIEW ONLY ]");
            }
            List<ReportMarkdownRenderer.MarkdownBlock> blocks = markdownRenderer.plainBlocks(compiled.markdown(), null);
            for (ReportMarkdownRenderer.MarkdownBlock block : blocks) {
                if (block.headingLevel() == 1) {
                    y = ensurePage(cursor, y);
                    y = pdfLine(cursor, 14, 60, y - 10, block.text());
                } else if (block.headingLevel() == 2) {
                    y = ensurePage(cursor, y);
                    y = pdfLine(cursor, 12, 65, y - 6, block.text());
                } else if (block.headingLevel() >= 3) {
                    y = ensurePage(cursor, y);
                    y = pdfLine(cursor, 11, 70, y - 4, block.text());
                } else {
                    for (String line : wrap(block.text(), 95)) {
                        y = ensurePage(cursor, y);
                        y = pdfLine(cursor, 11, 70, y, line);
                    }
                }
            }
            cursor.close();
            doc.save(out);
            return out.toByteArray();
        }
    }

    private String finalVersionMarkdown(ReportDocumentVersion version) {
        String markdown = version.getContentJson() == null || version.getContentJson().isBlank()
                ? null
                : richTextService.documentJsonToMarkdown(version.getContentJson());
        if (markdown == null || markdown.isBlank()) {
            markdown = version.getPlainText() == null ? "" : version.getPlainText();
        }
        return markdown
                .replace("[citation metadata incomplete]", "")
                .replace("[Citation metadata incomplete]", "")
                .replace("REFERENCE_METADATA_INCOMPLETE", "")
                .trim();
    }

    private float ensurePage(PdfCursor cursor, float y) throws IOException {
        if (y > 60) return y;
        cursor.newPage();
        return 790;
    }

    private float pdfLine(PdfCursor cursor, int size, float x, float y, String text) throws IOException {
        cursor.contentStream.beginText();
        cursor.contentStream.setFont(cursor.font, size);
        cursor.contentStream.newLineAtOffset(x, y);
        cursor.contentStream.showText(safePdf(text));
        cursor.contentStream.endText();
        cursor.y = y - (size + 6);
        return cursor.y;
    }

    private List<String> wrap(String text, int width) {
        java.util.ArrayList<String> lines = new java.util.ArrayList<>();
        String[] words = text.replace('\n', ' ').split("\\s+");
        StringBuilder line = new StringBuilder();
        for (String word : words) {
            if (line.length() + word.length() + 1 > width) { lines.add(line.toString()); line.setLength(0); }
            if (!line.isEmpty()) line.append(' ');
            line.append(word);
        }
        if (!line.isEmpty()) lines.add(line.toString());
        return lines;
    }

    private void paragraph(XWPFDocument doc, String text, ParagraphAlignment alignment, boolean bold, int size) {
        XWPFParagraph p = doc.createParagraph(); p.setAlignment(alignment);
        XWPFRun r = p.createRun(); r.setFontFamily("Times New Roman"); r.setFontSize(size); r.setBold(bold); r.setText(text == null ? "" : text);
    }

    private void writeDocxHeading(XWPFDocument doc, String text, int level, boolean pageBreak) {
        XWPFParagraph p = doc.createParagraph();
        if (pageBreak) p.setPageBreak(true);
        p.setStyle("Heading" + level);
        XWPFRun r = p.createRun();
        r.setFontFamily("Times New Roman");
        r.setFontSize(level == 1 ? 14 : 13);
        r.setBold(true);
        r.setText(text == null ? "" : text);
    }

    private void writeDocxTableOfContents(XWPFDocument doc) {
        writeDocxHeading(doc, "Table of Contents", 1, true);
        XWPFParagraph toc = doc.createParagraph();
        addField(toc, "TOC \\o \"1-3\" \\h \\z \\u");
    }

    private void writeDocxAbstract(XWPFDocument doc, ResearchReport report) {
        for (ResearchReportChapter chapter : chapterRepository.findAllByReportIdOrderByDisplayOrderAsc(report.getId())) {
            if (chapter.getType() != ReportChapterType.PRELIMINARY) {
                continue;
            }
            writeDocxHeading(doc, "Abstract", 1, true);
            for (ResearchReportSection section : sectionRepository.findAllByChapterIdOrderByDisplayOrderAsc(chapter.getId())) {
                if (section.getContent() != null && !section.getContent().isBlank()) {
                    markdownRenderer.renderMarkdown(doc, exportText(section, report.getCitationStyle(), citationNumberMap(report)), section.getHeading());
                }
            }
            return;
        }
    }

    private float writePdfAbstract(ResearchReport report, PdfCursor cursor, float y) throws IOException {
        for (ResearchReportChapter chapter : chapterRepository.findAllByReportIdOrderByDisplayOrderAsc(report.getId())) {
            if (chapter.getType() != ReportChapterType.PRELIMINARY) {
                continue;
            }
            y = ensurePage(cursor, y);
            y = pdfLine(cursor, 14, 60, y - 10, "Abstract");
            for (ResearchReportSection section : sectionRepository.findAllByChapterIdOrderByDisplayOrderAsc(chapter.getId())) {
                for (String paragraph : markdownRenderer.plainLines(exportText(section, report.getCitationStyle(), citationNumberMap(report)), section.getHeading())) {
                    for (String line : wrap(paragraph, 95)) {
                        y = ensurePage(cursor, y);
                        y = pdfLine(cursor, 11, 70, y, line);
                    }
                }
            }
            return y;
        }
        return y;
    }

    private void writeReferences(XWPFDocument doc, ResearchReport report, Map<UUID, Integer> referenceNumbers) {
        Map<UUID, ReferenceEntry> references = citedReferences(report);
        if (references.isEmpty()) return;
        writeDocxHeading(doc, "References", 1, true);
        int number = 1;
        for (ReferenceEntry reference : references.values()) {
            XWPFParagraph p = doc.createParagraph();
            p.setStyle("Bibliography");
            p.setIndentationHanging(360);
            p.setIndentationLeft(360);
            XWPFRun r = p.createRun();
            r.setFontFamily("Times New Roman");
            r.setFontSize(12);
            r.setText(citationFormattingService.format(reference, report.getCitationStyle(), CitationContext.REFERENCE_LIST, referenceNumbers.getOrDefault(reference.getId(), number++)).text());
        }
    }

    private void writeAppendices(XWPFDocument doc, ResearchReport report, Map<UUID, Integer> referenceNumbers) {
        for (ResearchReportChapter chapter : chapterRepository.findAllByReportIdOrderByDisplayOrderAsc(report.getId())) {
            if (chapter.getType() != ReportChapterType.APPENDICES) {
                continue;
            }
            List<ResearchReportSection> sections = sectionRepository.findAllByChapterIdOrderByDisplayOrderAsc(chapter.getId());
            boolean hasAppendixMatrix = "APPENDIX".equalsIgnoreCase(report.getLiteratureMatrixInclusion());
            if (!hasAppendixMatrix && sections.stream().allMatch(section -> section.getContent() == null || section.getContent().isBlank())) {
                return;
            }
            writeDocxHeading(doc, chapter.getTitle(), 1, true);
            if (hasAppendixMatrix) {
                literatureMatrixRepository.findFirstByProjectIdOrderByCreatedAtDesc(report.getProject().getId())
                        .ifPresent(matrix -> {
                            if (matrix.getMarkdownTable() != null && !matrix.getMarkdownTable().isBlank()) {
                                writeDocxHeading(doc, "Appendix: Literature Evidence Assessment Matrix", 2, false);
                                markdownRenderer.renderMarkdown(doc, matrix.getMarkdownTable(), "Appendix: Literature Evidence Assessment Matrix");
                            }
                        });
            }
            for (ResearchReportSection section : sections) {
                writeDocxHeading(doc, section.getHeading(), 2, false);
                markdownRenderer.renderMarkdown(doc, exportText(section, report.getCitationStyle(), referenceNumbers), section.getHeading());
            }
        }
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
                if (pr.getReference() != null && pr.isAvailableForCitation()) {
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

    private String displayCitation(ResearchReportCitation citation, CitationStyle style, Map<UUID, Integer> referenceNumbers) {
        ReferenceEntry reference = citation.getReference();
        if (reference == null && citation.getProjectReference() != null) {
            reference = citation.getProjectReference().getReference();
        }
        if (reference == null) {
            return "";
        }
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

    private void writeTitlePage(XWPFDocument doc, ResearchReport report) {
        paragraph(doc, report.getInstitutionName(), ParagraphAlignment.CENTER, true, 14);
        paragraph(doc, report.getDepartmentName(), ParagraphAlignment.CENTER, false, 12);
        paragraph(doc, report.getTitle(), ParagraphAlignment.CENTER, true, 16);
        paragraph(doc, report.getDegreeProgram(), ParagraphAlignment.CENTER, false, 12);
        paragraph(doc, report.getAuthorName(), ParagraphAlignment.CENTER, false, 12);
        paragraph(doc, report.getSupervisorName(), ParagraphAlignment.CENTER, false, 12);
        paragraph(doc, report.getSubmissionYear() == null ? "" : report.getSubmissionYear().toString(), ParagraphAlignment.CENTER, false, 12);
    }

    private void addPageNumberFooter(XWPFDocument doc) {
        try {
            CTSectPr sectPr = doc.getDocument().getBody().isSetSectPr()
                    ? doc.getDocument().getBody().getSectPr()
                    : doc.getDocument().getBody().addNewSectPr();
            XWPFHeaderFooterPolicy policy = new XWPFHeaderFooterPolicy(doc, sectPr);
            XWPFFooter footer = policy.createFooter(XWPFHeaderFooterPolicy.DEFAULT);
            XWPFParagraph p = footer.createParagraph();
            p.setAlignment(ParagraphAlignment.CENTER);
            addField(p, "PAGE");
        } catch (Exception ignored) {
            // A missing page field is preferable to failing the export.
        }
    }

    private void enableFieldUpdateOnOpen(XWPFDocument doc) {
        try {
            doc.getSettings().setUpdateFields();
        } catch (Exception ignored) {
            // Older POI settings implementations may not support this flag.
        }
    }

    private void addField(XWPFParagraph paragraph, String instruction) {
        CTP ctp = paragraph.getCTP();
        CTR begin = ctp.addNewR();
        begin.addNewFldChar().setFldCharType(STFldCharType.BEGIN);
        CTR instr = ctp.addNewR();
        instr.addNewInstrText().setStringValue(instruction);
        CTR separate = ctp.addNewR();
        separate.addNewFldChar().setFldCharType(STFldCharType.SEPARATE);
        CTR text = ctp.addNewR();
        text.addNewT().setStringValue("");
        CTR end = ctp.addNewR();
        end.addNewFldChar().setFldCharType(STFldCharType.END);
    }

    private String safePdf(String text) { return (text == null ? "" : text).replaceAll("[\\r\\n\\t]", " ").replaceAll("[^\\x20-\\x7E]", "?"); }
    private String filename(ResearchReport report, ReportExportFormat format) {
        return filename(report, format, false);
    }

    private String filename(ResearchReport report, ReportExportFormat format, boolean isDraft) {
        return filename(report, format, isDraft, ReportDocumentCompiler.DocumentCompilationScope.full());
    }

    private String filename(ResearchReport report, ReportExportFormat format, boolean isDraft, ReportDocumentCompiler.DocumentCompilationScope scope) {
        String base = report.getProject().getTitle() == null || report.getProject().getTitle().isBlank()
                ? report.getTitle()
                : report.getProject().getTitle();
        String suffix = "";
        if (scope != null && scope.mode() == ReportDocumentCompiler.CompilationMode.SELECTED) {
            suffix = selectedFilenameSuffix(report, scope.selectedNodeIds());
        }
        String draft = isDraft || (scope != null && scope.mode() == ReportDocumentCompiler.CompilationMode.SELECTED) ? "Draft_" : "";
        return safeFilename(draft + base + (suffix.isBlank() ? "_Project_Report" : suffix)) + "." + format.name().toLowerCase(Locale.ROOT);
    }

    private String selectedFilenameSuffix(ResearchReport report, Set<UUID> selectedNodeIds) {
        if (selectedNodeIds == null || selectedNodeIds.isEmpty()) {
            return "_Selected_Content";
        }
        List<Integer> chapters = chapterRepository.findAllByReportIdOrderByDisplayOrderAsc(report.getId()).stream()
                .filter(chapter -> selectedNodeIds.contains(chapter.getId()))
                .map(ResearchReportChapter::getChapterNumber)
                .filter(java.util.Objects::nonNull)
                .sorted()
                .toList();
        if (chapters.size() == 1) {
            return "_Chapter_" + chapters.get(0);
        }
        if (chapters.size() > 1) {
            return "_Chapters_" + chapters.get(0) + "-" + chapters.get(chapters.size() - 1);
        }
        return "_Selected_Content";
    }

    private String safeFilename(String value) {
        String name = value == null ? "research-report" : value.trim();
        name = name.replaceAll("[^A-Za-z0-9._-]+", "_").replaceAll("_+", "_");
        name = name.replaceAll("^_+|_+$", "");
        return name.isBlank() ? "research-report" : name;
    }

    private ReportDocumentCompiler.DocumentCompilationScope toCompilationScope(ResearchReport report, DocumentExportSelection selection) {
        if (selection == null || selection.selectionMode() == null || !"SELECTED".equalsIgnoreCase(selection.selectionMode())) {
            return ReportDocumentCompiler.DocumentCompilationScope.full();
        }
        Set<UUID> selectedIds = selection.selectedNodeIds() == null ? Set.of() : new LinkedHashSet<>(selection.selectedNodeIds());
        validateSelectedNodeIds(report, selectedIds);
        return ReportDocumentCompiler.DocumentCompilationScope.selected(
                selectedIds,
                Boolean.TRUE.equals(selection.includeCoverPage()),
                Boolean.TRUE.equals(selection.includeFrontMatter()),
                Boolean.TRUE.equals(selection.includeToc()),
                Boolean.TRUE.equals(selection.includeListOfFigures()),
                Boolean.TRUE.equals(selection.includeListOfTables()),
                selection.includeReferences() == null || Boolean.TRUE.equals(selection.includeReferences()),
                Boolean.TRUE.equals(selection.includeAppendices()),
                parseReferenceScope(selection.referenceMode())
        );
    }

    private ReportDocumentCompiler.ReferenceScope parseReferenceScope(String value) {
        if (value == null || value.isBlank()) {
            return ReportDocumentCompiler.ReferenceScope.CITED_IN_SELECTION;
        }
        return switch (value.trim().toUpperCase(Locale.ROOT)) {
            case "ALL", "ALL_PROJECT_REFERENCES" -> ReportDocumentCompiler.ReferenceScope.ALL_PROJECT_REFERENCES;
            case "NONE" -> ReportDocumentCompiler.ReferenceScope.NONE;
            default -> ReportDocumentCompiler.ReferenceScope.CITED_IN_SELECTION;
        };
    }

    private void validateSelectedNodeIds(ResearchReport report, Set<UUID> selectedIds) {
        if (selectedIds == null || selectedIds.isEmpty()) {
            throw new IllegalArgumentException("EXPORT_SELECTION_REQUIRED: select at least one chapter or section.");
        }
        Set<UUID> allowed = new LinkedHashSet<>();
        chapterRepository.findAllByReportIdOrderByDisplayOrderAsc(report.getId()).forEach(chapter -> allowed.add(chapter.getId()));
        sectionRepository.findAllByChapterReportId(report.getId()).forEach(section -> allowed.add(section.getId()));
        for (UUID id : selectedIds) {
            if (!allowed.contains(id)) {
                throw new IllegalArgumentException("EXPORT_SELECTION_INVALID: selected node does not belong to this report.");
            }
        }
    }
    private String defaultStyleJson() { return "{\"fontFamily\":\"Times New Roman\",\"bodyFontSize\":12,\"lineSpacing\":1.5,\"pageSize\":\"A4\"}"; }

    private String storageFailureCode(RuntimeException exception) {
        if (exception instanceof com.researchassistant.common.storage.StorageException storageException) {
            return storageException.getErrorCode();
        }
        return exception.getClass().getSimpleName();
    }

    private void writeDocxListOfFigures(XWPFDocument doc, ResearchReport report) {
        if (evidenceListService == null) return;
        var figures = evidenceListService.generateListOfFigures(report.getId());
        if (figures.isEmpty()) return;

        writeDocxHeading(doc, "List of Figures", 1, true);
        for (var fig : figures) {
            XWPFParagraph p = doc.createParagraph();
            XWPFRun r = p.createRun();
            r.setFontFamily("Times New Roman");
            r.setFontSize(11);
            r.setBold(true);
            r.setText(fig.figureNumber() + ": ");

            XWPFRun rCap = p.createRun();
            rCap.setFontFamily("Times New Roman");
            rCap.setFontSize(11);
            rCap.setText(fig.caption() + " ");

            XWPFRun rPage = p.createRun();
            rPage.setFontFamily("Times New Roman");
            rPage.setFontSize(11);
            rPage.setText(".................................................... " + fig.pageNumber());
        }
    }

    private void writeDocxListOfTables(XWPFDocument doc, ResearchReport report) {
        if (evidenceListService == null) return;
        var tables = evidenceListService.generateListOfTables(report.getId());
        if (tables.isEmpty()) return;

        writeDocxHeading(doc, "List of Tables", 1, true);
        for (var tab : tables) {
            XWPFParagraph p = doc.createParagraph();
            XWPFRun r = p.createRun();
            r.setFontFamily("Times New Roman");
            r.setFontSize(11);
            r.setBold(true);
            r.setText(tab.tableNumber() + ": ");

            XWPFRun rCap = p.createRun();
            rCap.setFontFamily("Times New Roman");
            rCap.setFontSize(11);
            rCap.setText(tab.caption() + " ");

            XWPFRun rPage = p.createRun();
            rPage.setFontFamily("Times New Roman");
            rPage.setFontSize(11);
            rPage.setText(".................................................... " + tab.pageNumber());
        }
    }

    private void writeDocxSectionEvidence(XWPFDocument doc, ResearchReportSection section, Map<UUID, String> labels) {
        if (evidenceRepository == null) return;
        var evidenceList = evidenceRepository.findAllBySectionIdOrderByDisplayOrderAscCreatedAtAsc(section.getId());
        for (var e : evidenceList) {
            String label = labels.getOrDefault(e.getId(), e.getFigureLabel() != null ? e.getFigureLabel() : "Figure");
            String renderedCaption = (e.getCaption() != null && !e.getCaption().isBlank())
                    ? label + ": " + e.getCaption()
                    : label;

            if (e.getStorageKey() != null && objectStorageService != null) {
                try {
                    var storageObj = objectStorageService.open(e.getStorageKey());
                    byte[] imgBytes;
                    try (var is = storageObj.contentStream()) {
                        imgBytes = is.readAllBytes();
                    }
                    if (imgBytes.length > 0) {
                        XWPFParagraph imgPara = doc.createParagraph();
                        imgPara.setAlignment(ParagraphAlignment.CENTER);
                        XWPFRun imgRun = imgPara.createRun();
                        int picType = (e.getMimeType() != null && e.getMimeType().contains("png"))
                                ? XWPFDocument.PICTURE_TYPE_PNG
                                : XWPFDocument.PICTURE_TYPE_JPEG;
                        imgRun.addPicture(new ByteArrayInputStream(imgBytes), picType, e.getOriginalFilename(),
                                400 * 12700, 240 * 12700);

                        XWPFParagraph capPara = doc.createParagraph();
                        capPara.setAlignment(ParagraphAlignment.CENTER);
                        XWPFRun capRun = capPara.createRun();
                        capRun.setFontFamily("Times New Roman");
                        capRun.setFontSize(10);
                        capRun.setItalic(true);
                        capRun.setBold(true);
                        capRun.setText(renderedCaption);
                    }
                } catch (Exception ex) {
                    paragraph(doc, renderedCaption, ParagraphAlignment.CENTER, true, 10);
                }
            } else if (e.getEvidenceType() == com.researchassistant.evidence.entity.EvidenceType.TABLE && e.getMetadataJson() != null) {
                paragraph(doc, renderedCaption, ParagraphAlignment.LEFT, true, 11);
                renderStructuredDocxTable(doc, e.getMetadataJson());
            }
        }
    }

    private void renderStructuredDocxTable(XWPFDocument doc, String metadataJson) {
        try {
            com.fasterxml.jackson.databind.JsonNode root = new com.fasterxml.jackson.databind.ObjectMapper().readTree(metadataJson);
            if (root.has("headers") && root.has("rows")) {
                var headers = root.get("headers");
                var rows = root.get("rows");
                int colCount = headers.size();
                int rowCount = 1 + rows.size();
                XWPFTable table = doc.createTable(rowCount, colCount);
                table.setWidth("100%");
                for (int c = 0; c < colCount; c++) {
                    table.getRow(0).getCell(c).setText(headers.get(c).asText());
                }
                for (int r = 0; r < rows.size(); r++) {
                    var rowNode = rows.get(r);
                    for (int c = 0; c < colCount; c++) {
                        String cellVal = c < rowNode.size() ? rowNode.get(c).asText() : "";
                        table.getRow(r + 1).getCell(c).setText(cellVal);
                    }
                }
            }
        } catch (Exception ignored) {}
    }

    private float writePdfListOfFigures(ResearchReport report, PdfCursor cursor, float y) throws IOException {
        if (evidenceListService == null) return y;
        var figures = evidenceListService.generateListOfFigures(report.getId());
        if (figures.isEmpty()) return y;

        y = ensurePage(cursor, y);
        y = pdfLine(cursor, 14, 60, y - 10, "List of Figures");
        for (var fig : figures) {
            y = ensurePage(cursor, y);
            String entry = fig.figureNumber() + ": " + fig.caption() + " .................... " + fig.pageNumber();
            y = pdfLine(cursor, 10, 70, y, entry);
        }
        return y;
    }

    private float writePdfListOfTables(ResearchReport report, PdfCursor cursor, float y) throws IOException {
        if (evidenceListService == null) return y;
        var tables = evidenceListService.generateListOfTables(report.getId());
        if (tables.isEmpty()) return y;

        y = ensurePage(cursor, y);
        y = pdfLine(cursor, 14, 60, y - 10, "List of Tables");
        for (var tab : tables) {
            y = ensurePage(cursor, y);
            String entry = tab.tableNumber() + ": " + tab.caption() + " .................... " + tab.pageNumber();
            y = pdfLine(cursor, 10, 70, y, entry);
        }
        return y;
    }

    private float writePdfSectionEvidence(PDDocument doc, PdfCursor cursor, float y, ResearchReportSection section, Map<UUID, String> labels) throws IOException {
        if (evidenceRepository == null) return y;
        var evidenceList = evidenceRepository.findAllBySectionIdOrderByDisplayOrderAscCreatedAtAsc(section.getId());
        for (var e : evidenceList) {
            String label = labels.getOrDefault(e.getId(), e.getFigureLabel() != null ? e.getFigureLabel() : "Figure");
            String renderedCaption = (e.getCaption() != null && !e.getCaption().isBlank())
                    ? label + ": " + e.getCaption()
                    : label;

            if (e.getStorageKey() != null && objectStorageService != null) {
                try {
                    var storageObj = objectStorageService.open(e.getStorageKey());
                    byte[] imgBytes;
                    try (var is = storageObj.contentStream()) {
                        imgBytes = is.readAllBytes();
                    }
                    if (imgBytes.length > 0) {
                        org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject pdImg =
                                org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject.createFromByteArray(doc, imgBytes, e.getOriginalFilename());
                        float width = 380;
                        float height = Math.min(220, ((float) pdImg.getHeight() / pdImg.getWidth()) * width);
                        if (y - height - 30 < 60) {
                            cursor.newPage();
                            y = 790;
                        }
                        cursor.contentStream.drawImage(pdImg, (612 - width) / 2, y - height, width, height);
                        y -= (height + 14);
                        y = pdfLine(cursor, 10, (612 - width) / 2, y, renderedCaption);
                        y -= 10;
                    }
                } catch (Exception ex) {
                    y = ensurePage(cursor, y);
                    y = pdfLine(cursor, 10, 70, y, renderedCaption);
                }
            } else {
                y = ensurePage(cursor, y);
                y = pdfLine(cursor, 10, 70, y, renderedCaption);
            }
        }
        return y;
    }

    private static final class PdfCursor implements Closeable {
        private final PDDocument document;
        private final PDType1Font font;
        private PDPageContentStream contentStream;
        private float y = 790;

        private PdfCursor(PDDocument document, PDType1Font font) throws IOException {
            this.document = document;
            this.font = font;
            newPage();
        }

        private void newPage() throws IOException {
            if (contentStream != null) {
                contentStream.close();
            }
            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);
            contentStream = new PDPageContentStream(document, page);
            y = 790;
        }

        @Override
        public void close() throws IOException {
            if (contentStream != null) {
                contentStream.close();
            }
        }
    }
}
