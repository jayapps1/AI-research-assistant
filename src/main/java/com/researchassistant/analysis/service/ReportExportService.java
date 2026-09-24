package com.researchassistant.analysis.service;

import com.researchassistant.analysis.dto.AnalysisDtos.ReportValidationResponse;
import com.researchassistant.analysis.entity.*;
import com.researchassistant.analysis.exception.ReportValidationException;
import com.researchassistant.analysis.repository.*;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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
    private final DocumentStorageService storageService;
    private final ProjectAuthorizationService authorizationService;
    private final SecurityAuditService auditService;
    private final QuotaService quotaService;
    private final ReportDocumentVersionRepository documentVersionRepository;
    private final LiteratureMatrixRepository literatureMatrixRepository;
    private final ProjectReferenceRepository projectReferenceRepository;
    private final org.springframework.beans.factory.ObjectProvider<AnalysisWorkflowService> workflowServiceProvider;

    public ReportExportService(ReportExportJobRepository exportJobRepository, ResearchReportRepository reportRepository,
            ResearchReportChapterRepository chapterRepository, ResearchReportSectionRepository sectionRepository,
            ResearchReportCitationRepository citationRepository, CitationFormattingService citationFormattingService,
            ReportMarkdownRenderer markdownRenderer,
            DocumentStorageService storageService, ProjectAuthorizationService authorizationService, SecurityAuditService auditService,
            QuotaService quotaService, ReportDocumentVersionRepository documentVersionRepository,
            LiteratureMatrixRepository literatureMatrixRepository, ProjectReferenceRepository projectReferenceRepository,
            org.springframework.beans.factory.ObjectProvider<AnalysisWorkflowService> workflowServiceProvider) {
        this.exportJobRepository = exportJobRepository;
        this.reportRepository = reportRepository;
        this.chapterRepository = chapterRepository;
        this.sectionRepository = sectionRepository;
        this.citationRepository = citationRepository;
        this.citationFormattingService = citationFormattingService;
        this.markdownRenderer = markdownRenderer;
        this.storageService = storageService;
        this.authorizationService = authorizationService;
        this.auditService = auditService;
        this.quotaService = quotaService;
        this.documentVersionRepository = documentVersionRepository;
        this.literatureMatrixRepository = literatureMatrixRepository;
        this.projectReferenceRepository = projectReferenceRepository;
        this.workflowServiceProvider = workflowServiceProvider;
    }

    @Transactional
    public ReportExportJob createExport(UUID reportId, ReportExportFormat format, User user) {
        return createExport(reportId, format, false, user);
    }

    @Transactional
    public ReportExportJob createExport(UUID reportId, ReportExportFormat format, boolean isDraft, User user) {
        ResearchReport report = reportRepository.findById(reportId).orElseThrow(() -> new ResourceNotFoundException("Report not found."));
        authorizationService.requireProjectEditor(report.getProject().getId(), user);
        if (format != ReportExportFormat.DOCX && format != ReportExportFormat.PDF) throw new IllegalArgumentException("Only DOCX and PDF exports are implemented.");

        if (!isDraft) {
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
        job.setFilename(filename(report, format, isDraft));
        job.setMimeType(format == ReportExportFormat.DOCX ? "application/vnd.openxmlformats-officedocument.wordprocessingml.document" : "application/pdf");
        job.setReportRevisionNumber(report.getRevisionNumber());
        job.setCitationStyle(report.getCitationStyle());
        job.setTemplateId(report.getTemplate() == null ? null : report.getTemplate().getId());
        job.setStyleConfigurationSnapshot(defaultStyleJson());
        job.setRequestedBy(user);
        job.setStartedAt(OffsetDateTime.now());

        documentVersionRepository.findFirstByReportIdOrderByVersionNumberDesc(reportId)
                .ifPresent(job::setFinalDocumentVersion);

        exportJobRepository.save(job);
        auditService.record(user.getId(), SecurityAuditEventType.REPORT_EXPORT_REQUESTED);
        try {
            byte[] bytes = format == ReportExportFormat.DOCX ? renderDocx(report, isDraft) : renderPdf(report, isDraft);
            String storageKey = "exports/" + report.getProject().getId() + "/" + job.getId() + "/" + job.getFilename();
            StoredDocumentObject stored = storageService.store(storageKey, new ByteArrayInputStream(bytes));
            job.setStorageKey(stored.storageKey());
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

    private byte[] renderDocx(ResearchReport report, boolean isDraft) throws IOException {
        try (XWPFDocument doc = new XWPFDocument(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
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
            for (ResearchReportChapter chapter : chapterRepository.findAllByReportIdOrderByDisplayOrderAsc(report.getId())) {
                if (chapter.getType() == ReportChapterType.PRELIMINARY || chapter.getType() == ReportChapterType.REFERENCES || chapter.getType() == ReportChapterType.APPENDICES) {
                    continue;
                }
                writeDocxHeading(doc, chapter.getTitle(), 1, true);
                List<ResearchReportSection> rootSections = sectionRepository.findAllByChapterIdAndParentSectionIsNullOrderByDisplayOrderAsc(chapter.getId());
                for (ResearchReportSection section : rootSections) {
                    String heading = section.getSectionNumber() != null ? section.getSectionNumber() + " " + section.getHeading() : section.getHeading();
                    writeDocxHeading(doc, heading, 2, false);
                    markdownRenderer.renderMarkdown(doc, exportText(section, report.getCitationStyle(), referenceNumbers), section.getHeading());

                    List<ResearchReportSection> subsections = sectionRepository.findAllByParentSectionIdOrderByDisplayOrderAsc(section.getId());
                    for (ResearchReportSection sub : subsections) {
                        String subHeading = sub.getSectionNumber() != null ? sub.getSectionNumber() + " " + sub.getHeading() : sub.getHeading();
                        writeDocxHeading(doc, subHeading, 3, false);
                        markdownRenderer.renderMarkdown(doc, exportText(sub, report.getCitationStyle(), referenceNumbers), sub.getHeading());

                        List<ResearchReportSection> subSubs = sectionRepository.findAllByParentSectionIdOrderByDisplayOrderAsc(sub.getId());
                        for (ResearchReportSection subSub : subSubs) {
                            String subSubHeading = subSub.getSectionNumber() != null ? subSub.getSectionNumber() + " " + subSub.getHeading() : subSub.getHeading();
                            writeDocxHeading(doc, subSubHeading, 4, false);
                            markdownRenderer.renderMarkdown(doc, exportText(subSub, report.getCitationStyle(), referenceNumbers), subSub.getHeading());
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
            for (ResearchReportChapter chapter : chapterRepository.findAllByReportIdOrderByDisplayOrderAsc(report.getId())) {
                if (chapter.getType() == ReportChapterType.PRELIMINARY || chapter.getType() == ReportChapterType.REFERENCES || chapter.getType() == ReportChapterType.APPENDICES) {
                    continue;
                }
                y = ensurePage(cursor, y);
                y = pdfLine(cursor, 14, 60, y - 10, chapter.getTitle());
                List<ResearchReportSection> rootSections = sectionRepository.findAllByChapterIdAndParentSectionIsNullOrderByDisplayOrderAsc(chapter.getId());
                for (ResearchReportSection section : rootSections) {
                    y = ensurePage(cursor, y);
                    String heading = section.getSectionNumber() != null ? section.getSectionNumber() + " " + section.getHeading() : section.getHeading();
                    y = pdfLine(cursor, 12, 70, y, heading);
                    for (String paragraph : markdownRenderer.plainLines(exportText(section, report.getCitationStyle(), referenceNumbers), section.getHeading())) {
                        for (String line : wrap(paragraph, 95)) {
                            y = ensurePage(cursor, y);
                            y = pdfLine(cursor, 11, 70, y, line);
                        }
                    }

                    List<ResearchReportSection> subsections = sectionRepository.findAllByParentSectionIdOrderByDisplayOrderAsc(section.getId());
                    for (ResearchReportSection sub : subsections) {
                        y = ensurePage(cursor, y);
                        String subHeading = sub.getSectionNumber() != null ? sub.getSectionNumber() + " " + sub.getHeading() : sub.getHeading();
                        y = pdfLine(cursor, 11, 75, y, subHeading);
                        for (String paragraph : markdownRenderer.plainLines(exportText(sub, report.getCitationStyle(), referenceNumbers), sub.getHeading())) {
                            for (String line : wrap(paragraph, 93)) {
                                y = ensurePage(cursor, y);
                                y = pdfLine(cursor, 10, 75, y, line);
                            }
                        }

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

    private String exportText(ResearchReportSection section, CitationStyle style, Map<UUID, Integer> referenceNumbers) {
        String text = section.getContent() == null ? "" : section.getContent();
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
        text = text.replaceAll("\\[?E\\d+]?\\b", "");
        text = text.replaceAll("(?i)\\[?DOC-\\d{3,}(?:\\s*,\\s*p\\.?\\s*\\d+)?]?", "");
        return text.replaceAll("[ \\t]{2,}", " ").trim();
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
        return (isDraft ? "draft-" : "") + "research-report-" + report.getProject().getId() + "-" + DateTimeFormatter.ofPattern("yyyyMMddHHmmss").format(java.time.LocalDateTime.now()) + "." + format.name().toLowerCase();
    }
    private String defaultStyleJson() { return "{\"fontFamily\":\"Times New Roman\",\"bodyFontSize\":12,\"lineSpacing\":1.5,\"pageSize\":\"A4\"}"; }

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
