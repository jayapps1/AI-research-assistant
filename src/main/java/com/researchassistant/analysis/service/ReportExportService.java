package com.researchassistant.analysis.service;

import com.researchassistant.analysis.entity.*;
import com.researchassistant.analysis.repository.*;
import com.researchassistant.common.exception.ResourceNotFoundException;
import com.researchassistant.document.storage.*;
import com.researchassistant.identity.entity.User;
import com.researchassistant.project.service.ProjectAuthorizationService;
import com.researchassistant.reference.dto.ReferenceDtos.CitationContext;
import com.researchassistant.reference.entity.ReferenceEntry;
import com.researchassistant.reference.service.CitationFormattingService;
import com.researchassistant.security.audit.SecurityAuditEventType;
import com.researchassistant.security.audit.SecurityAuditService;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.poi.xwpf.usermodel.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.io.*;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class ReportExportService {
    private final ReportExportJobRepository exportJobRepository;
    private final ResearchReportRepository reportRepository;
    private final ResearchReportChapterRepository chapterRepository;
    private final ResearchReportSectionRepository sectionRepository;
    private final ResearchReportCitationRepository citationRepository;
    private final CitationFormattingService citationFormattingService;
    private final DocumentStorageService storageService;
    private final ProjectAuthorizationService authorizationService;
    private final SecurityAuditService auditService;

    public ReportExportService(ReportExportJobRepository exportJobRepository, ResearchReportRepository reportRepository,
            ResearchReportChapterRepository chapterRepository, ResearchReportSectionRepository sectionRepository,
            ResearchReportCitationRepository citationRepository, CitationFormattingService citationFormattingService,
            DocumentStorageService storageService, ProjectAuthorizationService authorizationService, SecurityAuditService auditService) {
        this.exportJobRepository = exportJobRepository;
        this.reportRepository = reportRepository;
        this.chapterRepository = chapterRepository;
        this.sectionRepository = sectionRepository;
        this.citationRepository = citationRepository;
        this.citationFormattingService = citationFormattingService;
        this.storageService = storageService;
        this.authorizationService = authorizationService;
        this.auditService = auditService;
    }

    @Transactional
    public ReportExportJob createExport(UUID reportId, ReportExportFormat format, User user) {
        ResearchReport report = reportRepository.findById(reportId).orElseThrow(() -> new ResourceNotFoundException("Report not found."));
        authorizationService.requireProjectEditor(report.getProject().getId(), user);
        if (format != ReportExportFormat.DOCX && format != ReportExportFormat.PDF) throw new IllegalArgumentException("Only DOCX and PDF exports are implemented.");
        ReportExportJob job = new ReportExportJob();
        job.setReport(report);
        job.setFormat(format);
        job.setStatus(ReportExportStatus.RUNNING);
        job.setFilename(filename(report, format));
        job.setMimeType(format == ReportExportFormat.DOCX ? "application/vnd.openxmlformats-officedocument.wordprocessingml.document" : "application/pdf");
        job.setReportRevisionNumber(report.getRevisionNumber());
        job.setCitationStyle(report.getCitationStyle());
        job.setTemplateId(report.getTemplate() == null ? null : report.getTemplate().getId());
        job.setStyleConfigurationSnapshot(defaultStyleJson());
        job.setRequestedBy(user);
        job.setStartedAt(OffsetDateTime.now());
        exportJobRepository.save(job);
        auditService.record(user.getId(), SecurityAuditEventType.REPORT_EXPORT_REQUESTED);
        try {
            byte[] bytes = format == ReportExportFormat.DOCX ? renderDocx(report) : renderPdf(report);
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
            job.setErrorMessage("Report export failed.");
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
        try (XWPFDocument doc = new XWPFDocument(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            paragraph(doc, report.getInstitutionName(), ParagraphAlignment.CENTER, true, 14);
            paragraph(doc, report.getTitle(), ParagraphAlignment.CENTER, true, 16);
            paragraph(doc, report.getAuthorName(), ParagraphAlignment.CENTER, false, 12);
            paragraph(doc, report.getSupervisorName(), ParagraphAlignment.CENTER, false, 12);
            paragraph(doc, report.getSubmissionYear() == null ? "" : report.getSubmissionYear().toString(), ParagraphAlignment.CENTER, false, 12);
            for (ResearchReportChapter chapter : chapterRepository.findAllByReportIdOrderByDisplayOrderAsc(report.getId())) {
                XWPFParagraph cp = doc.createParagraph();
                cp.setPageBreak(true);
                XWPFRun cr = cp.createRun();
                cr.setBold(true); cr.setFontFamily("Times New Roman"); cr.setFontSize(14); cr.setText(chapter.getTitle());
                for (ResearchReportSection section : sectionRepository.findAllByChapterIdOrderByDisplayOrderAsc(chapter.getId())) {
                    paragraph(doc, section.getHeading(), ParagraphAlignment.LEFT, true, 13);
                    paragraph(doc, section.getContent() == null ? "" : section.getContent(), ParagraphAlignment.BOTH, false, 12);
                }
            }
            writeReferences(doc, report);
            doc.write(out);
            return out.toByteArray();
        }
    }

    private byte[] renderPdf(ResearchReport report) throws IOException {
        try (PDDocument doc = new PDDocument(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            PDType1Font font = new PDType1Font(Standard14Fonts.FontName.TIMES_ROMAN);
            PdfCursor cursor = new PdfCursor(doc, font);
            float y = pdfLine(cursor, 16, 60, cursor.y, report.getTitle());
            for (ResearchReportChapter chapter : chapterRepository.findAllByReportIdOrderByDisplayOrderAsc(report.getId())) {
                y = ensurePage(cursor, y);
                y = pdfLine(cursor, 14, 60, y - 10, chapter.getTitle());
                for (ResearchReportSection section : sectionRepository.findAllByChapterIdOrderByDisplayOrderAsc(chapter.getId())) {
                    y = ensurePage(cursor, y);
                    y = pdfLine(cursor, 12, 70, y, section.getHeading());
                    for (String line : wrap(section.getContent() == null ? "" : section.getContent(), 95)) {
                        y = ensurePage(cursor, y);
                        y = pdfLine(cursor, 11, 70, y, line);
                    }
                }
            }
            y = ensurePage(cursor, y);
            y = pdfLine(cursor, 14, 60, y - 10, "References");
            int number = 1;
            for (ReferenceEntry reference : citedReferences(report).values()) {
                String text = citationFormattingService.format(reference, report.getCitationStyle(), CitationContext.REFERENCE_LIST, number++).text();
                for (String line : wrap(text, 95)) {
                    y = ensurePage(cursor, y);
                    y = pdfLine(cursor, 11, 70, y, line);
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
    private void writeReferences(XWPFDocument doc, ResearchReport report) {
        Map<UUID, ReferenceEntry> references = citedReferences(report);
        if (references.isEmpty()) return;
        XWPFParagraph pageBreak = doc.createParagraph();
        pageBreak.setPageBreak(true);
        paragraph(doc, "References", ParagraphAlignment.LEFT, true, 14);
        int number = 1;
        for (ReferenceEntry reference : references.values()) {
            paragraph(doc, citationFormattingService.format(reference, report.getCitationStyle(), CitationContext.REFERENCE_LIST, number++).text(), ParagraphAlignment.LEFT, false, 12);
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
        return references;
    }

    private String safePdf(String text) { return (text == null ? "" : text).replaceAll("[\\r\\n\\t]", " ").replaceAll("[^\\x20-\\x7E]", "?"); }
    private String filename(ResearchReport report, ReportExportFormat format) { return "research-report-" + report.getProject().getId() + "-" + DateTimeFormatter.ofPattern("yyyyMMddHHmmss").format(java.time.LocalDateTime.now()) + "." + format.name().toLowerCase(); }
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
