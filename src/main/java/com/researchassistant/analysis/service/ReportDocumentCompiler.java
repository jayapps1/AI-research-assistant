package com.researchassistant.analysis.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.researchassistant.analysis.entity.*;
import com.researchassistant.analysis.repository.*;
import com.researchassistant.literature.repository.LiteratureMatrixRepository;
import com.researchassistant.reference.dto.ReferenceDtos.CitationContext;
import com.researchassistant.reference.entity.ProjectReference;
import com.researchassistant.reference.entity.ProjectReferenceStatus;
import com.researchassistant.reference.entity.ReferenceEntry;
import com.researchassistant.reference.repository.ProjectReferenceRepository;
import com.researchassistant.reference.service.CitationFormattingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class ReportDocumentCompiler {
    private static final Logger log = LoggerFactory.getLogger(ReportDocumentCompiler.class);

    private final ResearchReportChapterRepository chapterRepository;
    private final ResearchReportSectionRepository sectionRepository;
    private final ResearchReportCitationRepository citationRepository;
    private final ProjectReferenceRepository projectReferenceRepository;
    private final LiteratureMatrixRepository literatureMatrixRepository;
    private final ReportRichTextService richTextService;
    private final CitationFormattingService citationFormattingService;
    private final TitlePageRenderer titlePageRenderer;
    private final DocumentStructureExtractors structureExtractors;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public ReportDocumentCompiler(ResearchReportChapterRepository chapterRepository,
            ResearchReportSectionRepository sectionRepository,
            ResearchReportCitationRepository citationRepository,
            ProjectReferenceRepository projectReferenceRepository,
            LiteratureMatrixRepository literatureMatrixRepository,
            ReportRichTextService richTextService,
            CitationFormattingService citationFormattingService,
            TitlePageRenderer titlePageRenderer,
            DocumentStructureExtractors structureExtractors) {
        this.chapterRepository = chapterRepository;
        this.sectionRepository = sectionRepository;
        this.citationRepository = citationRepository;
        this.projectReferenceRepository = projectReferenceRepository;
        this.literatureMatrixRepository = literatureMatrixRepository;
        this.richTextService = richTextService;
        this.citationFormattingService = citationFormattingService;
        this.titlePageRenderer = titlePageRenderer;
        this.structureExtractors = structureExtractors;
    }

    public CompiledAcademicDocument compile(ResearchReport report) {
        List<ResearchReportChapter> chapters = chapterRepository.findAllByReportIdOrderByDisplayOrderAsc(report.getId());
        List<ResearchReportSection> allSections = sectionRepository.findAllByChapterReportId(report.getId()).stream()
                .sorted(Comparator
                        .comparing((ResearchReportSection section) -> section.getChapter().getDisplayOrder())
                        .thenComparingInt(ResearchReportSection::getDisplayOrder))
                .toList();
        Map<UUID, Integer> referenceNumbers = citationNumberMap(report);
        Map<String, Object> sectionRevisions = new LinkedHashMap<>();
        List<ReportRichTextService.DocumentPart> parts = new ArrayList<>();

        parts.add(markdownPart("Title Page", 1, titlePageRenderer.renderMarkdown(titlePageRenderer.resolveData(report))));

        appendUserFrontMatter(report, chapters, parts, sectionRevisions, referenceNumbers);
        String tocMarkdown = renderTableOfContents(chapters, report);
        int tocEntries = countTocEntries(chapters);
        parts.add(markdownPart("Table of Contents", 1, tocMarkdown));

        List<DocumentStructureExtractors.FigureEntry> figures = structureExtractors.extractFigures(allSections);
        List<DocumentStructureExtractors.TableEntry> tables = structureExtractors.extractTables(allSections);
        parts.add(markdownPart("List of Figures", 1, structureExtractors.renderListOfFiguresMarkdown(figures)));
        parts.add(markdownPart("List of Tables", 1, structureExtractors.renderListOfTablesMarkdown(tables)));

        int compiledSections = appendBody(report, chapters, parts, sectionRevisions, referenceNumbers);
        int referenceCount = appendReferences(report, parts);
        compiledSections += appendAppendices(report, chapters, parts, sectionRevisions, referenceNumbers);

        String contentJson = richTextService.assembleDocumentJson(parts);
        String plainText = richTextService.plainTextFromDocumentJson(contentJson);
        String markdown = richTextService.documentJsonToMarkdown(contentJson);
        int nonEmptySections = (int) allSections.stream().filter(this::hasContent).count();

        CompiledAcademicDocument compiled = new CompiledAcademicDocument(
                report.getId(),
                report.getProject().getId(),
                report.getTitle(),
                report.getCitationStyle(),
                contentJson,
                plainText,
                markdown,
                allSections.size(),
                nonEmptySections,
                compiledSections,
                tocEntries,
                figures.size(),
                tables.size(),
                referenceCount,
                writeJson(sectionRevisions),
                writeJson(referenceNumbers),
                report.getTemplate() == null ? "{}" : nullToEmpty(report.getTemplate().getConfigurationJson())
        );
        log.info(
                "Compiled final report document: reportId={} sourceSectionCount={} compiledSectionCount={} nonEmptySectionCount={} tocEntryCount={} figureCount={} tableCount={} referenceCount={} compiledContentCharacterCount={}",
                compiled.reportId(),
                compiled.sourceSectionCount(),
                compiled.compiledSectionCount(),
                compiled.nonEmptySectionCount(),
                compiled.tocEntryCount(),
                compiled.figureCount(),
                compiled.tableCount(),
                compiled.referenceCount(),
                compiled.contentJson() == null ? 0 : compiled.contentJson().length()
        );
        return compiled;
    }

    private void appendUserFrontMatter(ResearchReport report, List<ResearchReportChapter> chapters,
            List<ReportRichTextService.DocumentPart> parts, Map<String, Object> sectionRevisions,
            Map<UUID, Integer> referenceNumbers) {
        for (ResearchReportChapter chapter : chapters) {
            if (chapter.getType() != ReportChapterType.PRELIMINARY) {
                continue;
            }
            for (ResearchReportSection section : sectionRepository.findAllByChapterIdOrderByDisplayOrderAsc(chapter.getId())) {
                SectionSemanticPurpose purpose = section.resolveSemanticPurpose();
                if (purpose == SectionSemanticPurpose.TITLE_PAGE
                        || purpose == SectionSemanticPurpose.TABLE_OF_CONTENTS
                        || purpose == SectionSemanticPurpose.LIST_OF_FIGURES
                        || purpose == SectionSemanticPurpose.LIST_OF_TABLES) {
                    continue;
                }
                appendSectionPart(section, 1, parts, sectionRevisions, report.getCitationStyle(), referenceNumbers);
            }
        }
    }

    private int appendBody(ResearchReport report, List<ResearchReportChapter> chapters,
            List<ReportRichTextService.DocumentPart> parts, Map<String, Object> sectionRevisions,
            Map<UUID, Integer> referenceNumbers) {
        int count = 0;
        boolean coursework = isCourseworkReport(report);
        for (ResearchReportChapter chapter : chapters) {
            if (chapter.getType() == ReportChapterType.PRELIMINARY
                    || chapter.getType() == ReportChapterType.REFERENCES
                    || chapter.getType() == ReportChapterType.APPENDICES) {
                continue;
            }
            if (!coursework) {
                parts.add(new ReportRichTextService.DocumentPart(chapter.getTitle(), 1, null, null));
            }
            for (ResearchReportSection section : sectionRepository.findAllByChapterIdAndParentSectionIsNullOrderByDisplayOrderAsc(chapter.getId())) {
                count += appendSectionTree(section, coursework ? 1 : 2, parts, sectionRevisions, report.getCitationStyle(), referenceNumbers);
            }
            if (chapter.getType() == ReportChapterType.LITERATURE_REVIEW && "CHAPTER_TWO".equalsIgnoreCase(report.getLiteratureMatrixInclusion())) {
                literatureMatrixRepository.findFirstByProjectIdOrderByCreatedAtDesc(report.getProject().getId())
                        .ifPresent(matrix -> {
                            if (matrix.getMarkdownTable() != null && !matrix.getMarkdownTable().isBlank()) {
                                parts.add(markdownPart("Literature Evidence Assessment Matrix", 2, matrix.getMarkdownTable()));
                            }
                        });
            }
        }
        return count;
    }

    private int appendAppendices(ResearchReport report, List<ResearchReportChapter> chapters,
            List<ReportRichTextService.DocumentPart> parts, Map<String, Object> sectionRevisions,
            Map<UUID, Integer> referenceNumbers) {
        int count = 0;
        for (ResearchReportChapter chapter : chapters) {
            if (chapter.getType() != ReportChapterType.APPENDICES) {
                continue;
            }
            parts.add(new ReportRichTextService.DocumentPart(chapter.getTitle(), 1, null, null));
            if ("APPENDIX".equalsIgnoreCase(report.getLiteratureMatrixInclusion())) {
                literatureMatrixRepository.findFirstByProjectIdOrderByCreatedAtDesc(report.getProject().getId())
                        .ifPresent(matrix -> {
                            if (matrix.getMarkdownTable() != null && !matrix.getMarkdownTable().isBlank()) {
                                parts.add(markdownPart("Appendix: Literature Evidence Assessment Matrix", 2, matrix.getMarkdownTable()));
                            }
                        });
            }
            for (ResearchReportSection section : sectionRepository.findAllByChapterIdAndParentSectionIsNullOrderByDisplayOrderAsc(chapter.getId())) {
                count += appendSectionTree(section, 2, parts, sectionRevisions, report.getCitationStyle(), referenceNumbers);
            }
        }
        return count;
    }

    private int appendSectionTree(ResearchReportSection section, int headingLevel,
            List<ReportRichTextService.DocumentPart> parts, Map<String, Object> sectionRevisions,
            CitationStyle citationStyle, Map<UUID, Integer> referenceNumbers) {
        appendSectionPart(section, Math.min(headingLevel, 4), parts, sectionRevisions, citationStyle, referenceNumbers);
        int count = 1;
        for (ResearchReportSection child : sectionRepository.findAllByParentSectionIdOrderByDisplayOrderAsc(section.getId())) {
            count += appendSectionTree(child, headingLevel + 1, parts, sectionRevisions, citationStyle, referenceNumbers);
        }
        return count;
    }

    private void appendSectionPart(ResearchReportSection section, int level,
            List<ReportRichTextService.DocumentPart> parts, Map<String, Object> sectionRevisions,
            CitationStyle citationStyle, Map<UUID, Integer> referenceNumbers) {
        sectionRevisions.put(section.getId().toString(), section.getRevisionNumber());
        String heading = section.getSectionNumber() != null && !section.getSectionNumber().isBlank()
                ? section.getSectionNumber() + " " + section.getHeading()
                : section.getHeading();
        String contentJson = contentJsonForSection(section, citationStyle, referenceNumbers);
        String plainText = richTextService.plainTextFromDocumentJson(contentJson);
        parts.add(new ReportRichTextService.DocumentPart(heading, level, contentJson, plainText));
    }

    private String contentJsonForSection(ResearchReportSection section, CitationStyle style, Map<UUID, Integer> referenceNumbers) {
        if (section.getContentJson() != null
                && !section.getContentJson().isBlank()
                && richTextService.isValidDocumentJson(section.getContentJson())
                && !richTextService.plainTextFromDocumentJson(section.getContentJson()).isBlank()) {
            return section.getContentJson();
        }
        String renderedMarkdown = exportText(section, style, referenceNumbers);
        return richTextService.markdownToDocumentJson(renderedMarkdown);
    }

    private int appendReferences(ResearchReport report, List<ReportRichTextService.DocumentPart> parts) {
        Map<UUID, ReferenceEntry> references = citedReferences(report);
        StringBuilder markdown = new StringBuilder();
        int number = 1;
        for (ReferenceEntry reference : references.values()) {
            String text = citationFormattingService.format(reference, report.getCitationStyle(), CitationContext.REFERENCE_LIST, number++).text();
            if (text != null && !text.isBlank()) {
                markdown.append(text.trim()).append("\n\n");
            }
        }
        if (markdown.isEmpty()) {
            markdown.append("*No cited references have been added to this report.*");
        }
        parts.add(markdownPart("References", 1, markdown.toString().trim()));
        return references.size();
    }

    private String renderTableOfContents(List<ResearchReportChapter> chapters, ResearchReport report) {
        StringBuilder md = new StringBuilder();
        boolean coursework = isCourseworkReport(report);
        for (ResearchReportChapter chapter : chapters) {
            if (chapter.getType() == ReportChapterType.PRELIMINARY
                    || chapter.getType() == ReportChapterType.REFERENCES
                    || chapter.getType() == ReportChapterType.APPENDICES) {
                continue;
            }
            if (!coursework) {
                String prefix = chapter.getChapterNumber() == null ? "" : "Chapter " + chapter.getChapterNumber() + ": ";
                md.append("### ").append(prefix).append(chapter.getTitle()).append("\n");
            }
            for (ResearchReportSection section : sectionRepository.findAllByChapterIdAndParentSectionIsNullOrderByDisplayOrderAsc(chapter.getId())) {
                appendTocLine(md, section, 0);
            }
            md.append("\n");
        }
        return md.toString().trim();
    }

    private void appendTocLine(StringBuilder md, ResearchReportSection section, int depth) {
        md.append("  ".repeat(Math.max(0, depth)));
        if (section.getSectionNumber() != null && !section.getSectionNumber().isBlank()) {
            md.append(section.getSectionNumber()).append(" ");
        }
        md.append(section.getHeading()).append("\n");
        for (ResearchReportSection child : sectionRepository.findAllByParentSectionIdOrderByDisplayOrderAsc(section.getId())) {
            appendTocLine(md, child, depth + 1);
        }
    }

    private int countTocEntries(List<ResearchReportChapter> chapters) {
        int count = 0;
        for (ResearchReportChapter chapter : chapters) {
            if (chapter.getType() == ReportChapterType.PRELIMINARY
                    || chapter.getType() == ReportChapterType.REFERENCES
                    || chapter.getType() == ReportChapterType.APPENDICES) {
                continue;
            }
            count++;
            for (ResearchReportSection section : sectionRepository.findAllByChapterIdAndParentSectionIsNullOrderByDisplayOrderAsc(chapter.getId())) {
                count += countSectionTree(section);
            }
        }
        return count;
    }

    private int countSectionTree(ResearchReportSection section) {
        int count = 1;
        for (ResearchReportSection child : sectionRepository.findAllByParentSectionIdOrderByDisplayOrderAsc(section.getId())) {
            count += countSectionTree(child);
        }
        return count;
    }

    private ReportRichTextService.DocumentPart markdownPart(String heading, int level, String markdown) {
        String json = richTextService.markdownToDocumentJson(markdown);
        return new ReportRichTextService.DocumentPart(heading, level, json, richTextService.plainTextFromDocumentJson(json));
    }

    private boolean hasContent(ResearchReportSection section) {
        if (section.getContentJson() != null
                && richTextService.isValidDocumentJson(section.getContentJson())
                && !richTextService.plainTextFromDocumentJson(section.getContentJson()).isBlank()) {
            return true;
        }
        return section.getContent() != null && !section.getContent().isBlank()
                || section.getPlainText() != null && !section.getPlainText().isBlank();
    }

    private String resolveSectionRawText(ResearchReportSection section) {
        if (section.getContentJson() != null && !section.getContentJson().isBlank() && richTextService.isValidDocumentJson(section.getContentJson())) {
            String md = richTextService.documentJsonToMarkdown(section.getContentJson());
            if (md != null && !md.isBlank()) {
                return md;
            }
        }
        if (section.getContent() != null && !section.getContent().isBlank()) {
            return section.getContent();
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
        return replaceInlineCitationTokens(text, section, style, referenceNumbers)
                .replaceAll("\\[?E\\d+]?\\b", "")
                .replaceAll("(?i)\\[?DOC-\\d{3,}(?:\\s*,\\s*p\\.?\\s*\\d+)?]?", "")
                .replaceAll("[ \\t]{2,}", " ")
                .trim();
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

    private String displayCitation(ResearchReportCitation citation, CitationStyle style, Map<UUID, Integer> referenceNumbers) {
        ReferenceEntry reference = citation.getReference();
        if (reference == null && citation.getProjectReference() != null) {
            reference = citation.getProjectReference().getReference();
        }
        return reference == null ? "" : displayCitation(reference, style, referenceNumbers);
    }

    private String displayCitation(ReferenceEntry reference, CitationStyle style, Map<UUID, Integer> referenceNumbers) {
        Integer number = referenceNumbers.get(reference.getId());
        CitationContext context = style == CitationStyle.IEEE || style == CitationStyle.VANCOUVER || style == CitationStyle.NUMERIC_APA
                ? CitationContext.NUMERIC
                : CitationContext.IN_TEXT_PARENTHETICAL;
        var formatted = citationFormattingService.format(reference, style, context, number);
        return formatted.metadataComplete() || context == CitationContext.NUMERIC ? formatted.text() : "";
    }

    private boolean isCourseworkReport(ResearchReport report) {
        return report.getType() == ResearchReportType.COURSEWORK
                || (report.getProject() != null
                && report.getProject().getWorkspaceType() == com.researchassistant.project.entity.AcademicWorkspaceType.COURSEWORK);
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception ignored) {
            return "{}";
        }
    }

    private String nullToEmpty(String value) {
        return value == null || value.isBlank() ? "{}" : value;
    }

    public record CompiledAcademicDocument(
            UUID reportId,
            UUID projectId,
            String title,
            CitationStyle citationStyle,
            String contentJson,
            String plainText,
            String markdown,
            int sourceSectionCount,
            int nonEmptySectionCount,
            int compiledSectionCount,
            int tocEntryCount,
            int figureCount,
            int tableCount,
            int referenceCount,
            String sectionRevisionSnapshotJson,
            String referencesSnapshotJson,
            String templateSnapshotJson
    ) {}
}
