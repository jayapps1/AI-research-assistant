package com.researchassistant.template.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.researchassistant.analysis.entity.ReportChapterType;
import com.researchassistant.analysis.entity.SectionGenerationPolicy;
import com.researchassistant.analysis.entity.SectionRequirementLevel;
import com.researchassistant.analysis.entity.SectionSemanticPurpose;
import com.researchassistant.document.entity.DocumentVersion;
import com.researchassistant.document.extraction.DocxTextExtractor;
import com.researchassistant.document.extraction.ExtractionResult;
import com.researchassistant.document.extraction.PdfBoxTextExtractor;
import com.researchassistant.template.dto.AcademicTemplateDtos.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.researchassistant.template.dto.AcademicTemplateDtos.NOT_SPECIFIED;

@Service
public class AcademicTemplateExtractionService {

    private static final Logger log = LoggerFactory.getLogger(AcademicTemplateExtractionService.class);

    private final PdfBoxTextExtractor pdfExtractor;
    private final DocxTextExtractor docxExtractor;
    private final ObjectMapper objectMapper;

    private static final Pattern INSTITUTION_PATTERN = Pattern.compile(
            "(?i)\\b([A-Za-z &]{2,60}(?:University|Polytechnic|College|Institute|Technical\\s+University))\\b"
    );

    private static final Pattern DEPARTMENT_SPECIFIC_PATTERN = Pattern.compile(
            "(?i)\\b(Department\\s+of\\s+[A-Za-z &]{3,50})\\b"
    );

    private static final Pattern FACULTY_PATTERN = Pattern.compile(
            "(?i)\\b((?:Faculty|School|Division)\\s+of\\s+[A-Za-z &]{3,50})\\b"
    );

    private static final Pattern PROGRAMME_PATTERN = Pattern.compile(
            "(?i)\\b(?:programme|program|degree|diploma|course)\\s*:\\s*([A-Za-z0-9\\s.()-]{3,40})\\b|" +
            "\\b((?:B\\.Sc|BSc|B\\.Eng|HND|Diploma|M\\.Sc|MSc|Ph\\.?D)\\s+(?:in\\s+)?[A-Za-z\\s&]{3,35})\\b"
    );

    private static final Pattern DOC_TYPE_PATTERN = Pattern.compile(
            "(?i)\\b(final\\s+year\\s+project\\s+report|project\\s+report|capstone\\s+project|master'?s?\\s+thesis|" +
            "doctoral\\s+dissertation|phd\\s+thesis|coursework\\s+report|technical\\s+report|dissertation|thesis)\\b"
    );

    private static final Pattern CITATION_STYLE_PATTERN = Pattern.compile(
            "(?i)\\b(apa\\s*(?:7(?:th)?|6(?:th)?)?|ieee|harvard|chicago|vancouver|mla)\\b"
    );

    private static final Pattern FONT_PATTERN = Pattern.compile(
            "(?i)\\b(times\\s+new\\s+roman|arial|calibri|georgia|cambria)\\b"
    );

    private static final Pattern FONT_SIZE_PATTERN = Pattern.compile(
            "(?i)\\b(\\d{1,2})\\s*(?:pt|point)\\s*(?:font)?\\b"
    );

    private static final Pattern SPACING_PATTERN = Pattern.compile(
            "(?i)\\b(1\\.5|double|2\\.0|single|1\\.0)\\s*(?:line\\s*)?spacing\\b"
    );

    private static final Pattern MARGIN_PATTERN = Pattern.compile(
            "(?i)(?:margin[s]?|left|right|top|bottom)[^.\\n]{0,30}\\b(\\d(?:\\.\\d)?)\\s*(?:inch(?:es)?|in|\"|cm)\\b"
    );

    private static final Pattern CHAPTER_HEADER_PATTERN = Pattern.compile(
            "(?i)^\\s*(?:chapter\\s+(?:one|two|three|four|five|six|seven|eight|nine|ten|\\d+)|ch\\.?\\s*\\d+)[\\s:.-]*(.*)$"
    );

    private static final Pattern SECTION_HEADER_PATTERN = Pattern.compile(
            "^\\s*(\\d+\\.\\d+(?:\\.\\d+)?)\\s+([^\\n\\r]{3,100})$"
    );

    public AcademicTemplateExtractionService(
            PdfBoxTextExtractor pdfExtractor,
            DocxTextExtractor docxExtractor,
            ObjectMapper objectMapper
    ) {
        this.pdfExtractor = pdfExtractor;
        this.docxExtractor = docxExtractor;
        this.objectMapper = objectMapper;
    }

    /**
     * Extracts template structure from an input stream (PDF or DOCX).
     */
    public ExtractedAcademicTemplate extractFromStream(InputStream inputStream, String fileName, String mimeType) {
        String fullText = extractText(inputStream, fileName, mimeType);
        return extractFromText(fullText, fileName);
    }

    /**
     * Analyzes plain text and extracts institution, department, front matter, chapters,
     * sections, formatting rules, and marks uncertain items.
     */
    public ExtractedAcademicTemplate extractFromText(String text, String fileName) {
        if (text == null || text.isBlank()) {
            return fallbackTemplate(fileName);
        }

        List<UncertainItemDto> uncertainItems = new ArrayList<>();

        // 1. Detect Metadata
        String institution = detectInstitution(text);
        if (institution.equals(NOT_SPECIFIED)) {
            uncertainItems.add(new UncertainItemDto("METADATA", "Institution", "Institution name was not explicitly stated in guideline", 0.4));
        }

        String department = detectDepartment(text);
        if (department.equals(NOT_SPECIFIED)) {
            uncertainItems.add(new UncertainItemDto("METADATA", "Department", "Department name was not explicitly stated in guideline", 0.4));
        }

        String programme = detectProgramme(text);
        String documentType = detectDocumentType(text);
        String citationStyle = detectCitationStyle(text);
        if (citationStyle.equals(NOT_SPECIFIED)) {
            uncertainItems.add(new UncertainItemDto("CITATION", "Citation Style", "Referencing format not explicitly specified (defaults to APA_7)", 0.5));
        }

        // 2. Extract Formatting Rules
        TemplateFormattingRulesDto formatting = detectFormattingRules(text);

        // 3. Extract Front Matter
        List<TemplateSectionDefinitionDto> frontMatter = detectFrontMatter(text);

        // 4. Extract Chapters and Sections
        List<TemplateChapterDefinitionDto> chapters = detectChaptersAndSections(text, uncertainItems);
        if (chapters.isEmpty()) {
            chapters = buildStandardAcademicChapters(documentType);
            uncertainItems.add(new UncertainItemDto("STRUCTURE", "Chapter Hierarchy", "Guideline text lacked explicit chapter markers; synthesized standard academic structure", 0.6));
        }

        // 5. Extract Appendices
        List<TemplateSectionDefinitionDto> appendices = detectAppendices(text);

        return new ExtractedAcademicTemplate(
                institution,
                department,
                programme,
                documentType,
                citationStyle,
                frontMatter,
                chapters,
                appendices,
                formatting,
                uncertainItems
        );
    }

    private String extractText(InputStream inputStream, String fileName, String mimeType) {
        try {
            DocumentVersion dummyVersion = new DocumentVersion();
            dummyVersion.setOriginalFilename(fileName);
            dummyVersion.setMimeType(mimeType != null ? mimeType : guessMimeType(fileName));

            ExtractionResult result;
            if (pdfExtractor.supports(dummyVersion)) {
                result = pdfExtractor.extract(dummyVersion, inputStream);
            } else if (docxExtractor.supports(dummyVersion)) {
                result = docxExtractor.extract(dummyVersion, inputStream);
            } else {
                byte[] bytes = inputStream.readAllBytes();
                return new String(bytes, java.nio.charset.StandardCharsets.UTF_8);
            }

            StringBuilder sb = new StringBuilder();
            if (result != null && result.pages() != null) {
                for (var page : result.pages()) {
                    sb.append(page.text()).append("\n");
                }
            }
            return sb.toString();
        } catch (Exception e) {
            log.warn("Failed to extract text from document stream {}: {}", fileName, e.getMessage());
            return "";
        }
    }

    private String guessMimeType(String fileName) {
        if (fileName == null) return "application/pdf";
        String lower = fileName.toLowerCase(Locale.ROOT);
        if (lower.endsWith(".docx")) return "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
        if (lower.endsWith(".doc")) return "application/msword";
        if (lower.endsWith(".txt")) return "text/plain";
        return "application/pdf";
    }

    private String detectInstitution(String text) {
        Matcher m = INSTITUTION_PATTERN.matcher(text);
        if (m.find()) {
            return m.group(1).trim();
        }
        return NOT_SPECIFIED;
    }

    private String detectDepartment(String text) {
        Matcher deptMatcher = DEPARTMENT_SPECIFIC_PATTERN.matcher(text);
        if (deptMatcher.find()) {
            return deptMatcher.group(1).trim();
        }
        Matcher facultyMatcher = FACULTY_PATTERN.matcher(text);
        if (facultyMatcher.find()) {
            return facultyMatcher.group(1).trim();
        }
        return NOT_SPECIFIED;
    }

    private String detectProgramme(String text) {
        Matcher m = PROGRAMME_PATTERN.matcher(text);
        if (m.find()) {
            String g1 = m.group(1);
            String g2 = m.group(2);
            return (g1 != null ? g1 : g2).trim();
        }
        return NOT_SPECIFIED;
    }

    private String detectDocumentType(String text) {
        Matcher m = DOC_TYPE_PATTERN.matcher(text);
        if (m.find()) {
            String matched = m.group(1).trim();
            return Character.toUpperCase(matched.charAt(0)) + matched.substring(1);
        }
        return "Project Report";
    }

    private String detectCitationStyle(String text) {
        Matcher m = CITATION_STYLE_PATTERN.matcher(text);
        if (m.find()) {
            String style = m.group(1).toUpperCase(Locale.ROOT);
            if (style.startsWith("APA")) return "APA_7";
            if (style.contains("IEEE")) return "IEEE";
            if (style.contains("HARVARD")) return "HARVARD";
            if (style.contains("CHICAGO")) return "CHICAGO";
            if (style.contains("MLA")) return "MLA";
            if (style.contains("VANCOUVER")) return "VANCOUVER";
            return style;
        }
        return NOT_SPECIFIED;
    }

    private TemplateFormattingRulesDto detectFormattingRules(String text) {
        String font = NOT_SPECIFIED;
        Matcher fontMatcher = FONT_PATTERN.matcher(text);
        if (fontMatcher.find()) {
            String f = fontMatcher.group(1).trim();
            font = Character.toUpperCase(f.charAt(0)) + f.substring(1);
        }

        String bodySize = NOT_SPECIFIED;
        String h1Size = NOT_SPECIFIED;
        String h2Size = NOT_SPECIFIED;
        Matcher sizeMatcher = FONT_SIZE_PATTERN.matcher(text);
        List<String> detectedSizes = new ArrayList<>();
        while (sizeMatcher.find() && detectedSizes.size() < 4) {
            detectedSizes.add(sizeMatcher.group(1) + " pt");
        }
        if (!detectedSizes.isEmpty()) {
            bodySize = detectedSizes.get(0);
            if (detectedSizes.size() > 1) {
                h1Size = detectedSizes.get(1);
            }
        }

        String spacing = NOT_SPECIFIED;
        Matcher spacingMatcher = SPACING_PATTERN.matcher(text);
        if (spacingMatcher.find()) {
            spacing = spacingMatcher.group(1).trim();
        }

        TemplateMarginsDto margins = TemplateMarginsDto.defaultUnspecified();
        Matcher marginMatcher = MARGIN_PATTERN.matcher(text);
        if (marginMatcher.find()) {
            String val = marginMatcher.group(1) + " inches";
            margins = new TemplateMarginsDto(val, "1.0 inch", "1.0 inch", "1.0 inch");
        }

        String numberingStyle = text.contains("1.1") || text.contains("1.1.1") ? "Decimal (1.1, 1.1.1)" : NOT_SPECIFIED;
        String pageNumbering = text.toLowerCase(Locale.ROOT).contains("roman") ? "Roman for preliminary, Arabic for body" : NOT_SPECIFIED;
        String chapterBreak = text.toLowerCase(Locale.ROOT).contains("new page") || text.toLowerCase(Locale.ROOT).contains("fresh page")
                ? "Each chapter starts on a new page" : NOT_SPECIFIED;

        return new TemplateFormattingRulesDto(
                font,
                bodySize,
                h1Size,
                h2Size,
                spacing,
                margins,
                numberingStyle,
                pageNumbering,
                chapterBreak
        );
    }

    private List<TemplateSectionDefinitionDto> detectFrontMatter(String text) {
        List<TemplateSectionDefinitionDto> frontMatter = new ArrayList<>();
        String lower = text.toLowerCase(Locale.ROOT);

        frontMatter.add(new TemplateSectionDefinitionDto(
                null, "Title Page", SectionRequirementLevel.REQUIRED,
                SectionSemanticPurpose.TITLE_PAGE, SectionGenerationPolicy.DETERMINISTIC,
                "Official Title Page following institutional layout", List.of()
        ));

        if (lower.contains("declaration")) {
            frontMatter.add(new TemplateSectionDefinitionDto(
                    null, "Declaration", SectionRequirementLevel.REQUIRED,
                    SectionSemanticPurpose.DECLARATION, SectionGenerationPolicy.DETERMINISTIC,
                    "Student declaration of original work", List.of()
            ));
        }
        if (lower.contains("certification") || lower.contains("approval page") || lower.contains("supervisors approval")) {
            frontMatter.add(new TemplateSectionDefinitionDto(
                    null, "Certification", SectionRequirementLevel.REQUIRED,
                    SectionSemanticPurpose.CERTIFICATION, SectionGenerationPolicy.DETERMINISTIC,
                    "Supervisor and departmental certification", List.of()
            ));
        }
        if (lower.contains("dedication")) {
            frontMatter.add(new TemplateSectionDefinitionDto(
                    null, "Dedication", SectionRequirementLevel.OPTIONAL,
                    SectionSemanticPurpose.DEDICATION, SectionGenerationPolicy.DETERMINISTIC,
                    "Personal dedication", List.of()
            ));
        }
        if (lower.contains("acknowledgement") || lower.contains("acknowledgment")) {
            frontMatter.add(new TemplateSectionDefinitionDto(
                    null, "Acknowledgements", SectionRequirementLevel.RECOMMENDED,
                    SectionSemanticPurpose.ACKNOWLEDGEMENTS, SectionGenerationPolicy.DETERMINISTIC,
                    "Institutional and academic acknowledgements", List.of()
            ));
        }
        if (lower.contains("abstract") || lower.contains("executive summary")) {
            frontMatter.add(new TemplateSectionDefinitionDto(
                    null, "Abstract", SectionRequirementLevel.REQUIRED,
                    SectionSemanticPurpose.ABSTRACT, SectionGenerationPolicy.PROJECT_DERIVED_AI,
                    "Summary of research/project objectives, methodology, findings and conclusion", List.of()
            ));
        }

        frontMatter.add(new TemplateSectionDefinitionDto(
                null, "Table of Contents", SectionRequirementLevel.REQUIRED,
                SectionSemanticPurpose.TABLE_OF_CONTENTS, SectionGenerationPolicy.DETERMINISTIC,
                "Dynamic Table of Contents auto-generated from report headings", List.of()
        ));
        frontMatter.add(new TemplateSectionDefinitionDto(
                null, "List of Tables", SectionRequirementLevel.RECOMMENDED,
                SectionSemanticPurpose.LIST_OF_TABLES, SectionGenerationPolicy.DETERMINISTIC,
                "List of tables with page references", List.of()
        ));
        frontMatter.add(new TemplateSectionDefinitionDto(
                null, "List of Figures", SectionRequirementLevel.RECOMMENDED,
                SectionSemanticPurpose.LIST_OF_FIGURES, SectionGenerationPolicy.DETERMINISTIC,
                "List of figures and diagrams with page references", List.of()
        ));

        return frontMatter;
    }

    private List<TemplateChapterDefinitionDto> detectChaptersAndSections(String text, List<UncertainItemDto> uncertainItems) {
        List<TemplateChapterDefinitionDto> chapters = new ArrayList<>();
        String[] lines = text.split("\r?\n");

        TemplateChapterDefinitionDto currentChapter = null;
        List<TemplateSectionDefinitionDto> currentSections = new ArrayList<>();
        int currentChapterNum = 0;

        for (String rawLine : lines) {
            String line = rawLine.trim();
            if (line.isEmpty()) continue;

            Matcher chMatcher = CHAPTER_HEADER_PATTERN.matcher(line);
            if (chMatcher.find()) {
                if (currentChapter != null) {
                    chapters.add(new TemplateChapterDefinitionDto(
                            currentChapter.chapterNumber(),
                            currentChapter.title(),
                            currentChapter.type(),
                            true,
                            List.copyOf(currentSections)
                    ));
                    currentSections.clear();
                }

                currentChapterNum++;
                String titleSuffix = chMatcher.group(1).trim();
                String chapterTitle = resolveChapterTitle(currentChapterNum, titleSuffix);
                ReportChapterType chType = resolveChapterType(currentChapterNum, chapterTitle);

                currentChapter = new TemplateChapterDefinitionDto(
                        currentChapterNum,
                        chapterTitle,
                        chType,
                        true,
                        List.of()
                );
                continue;
            }

            Matcher secMatcher = SECTION_HEADER_PATTERN.matcher(line);
            if (secMatcher.find()) {
                String secNum = secMatcher.group(1).trim();
                String heading = secMatcher.group(2).trim();

                // If no chapter encountered yet, initialize Chapter 1
                if (currentChapter == null) {
                    currentChapterNum = 1;
                    currentChapter = new TemplateChapterDefinitionDto(
                            1, "CHAPTER ONE — INTRODUCTION", ReportChapterType.INTRODUCTION, true, List.of()
                    );
                }

                SectionSemanticPurpose purpose = resolveSectionSemanticPurpose(heading);
                SectionGenerationPolicy policy = purpose.defaultPolicy();
                SectionRequirementLevel reqLevel = isSectionOptional(heading)
                        ? SectionRequirementLevel.OPTIONAL
                        : (isSectionRecommended(heading) ? SectionRequirementLevel.RECOMMENDED : SectionRequirementLevel.REQUIRED);

                TemplateSectionDefinitionDto secDto = new TemplateSectionDefinitionDto(
                        secNum,
                        heading,
                        reqLevel,
                        purpose,
                        policy,
                        "Institutional guideline requirement for " + heading,
                        List.of()
                );
                currentSections.add(secDto);
            }
        }

        if (currentChapter != null) {
            chapters.add(new TemplateChapterDefinitionDto(
                    currentChapter.chapterNumber(),
                    currentChapter.title(),
                    currentChapter.type(),
                    true,
                    List.copyOf(currentSections)
            ));
        }

        return chapters;
    }

    private List<TemplateSectionDefinitionDto> detectAppendices(String text) {
        List<TemplateSectionDefinitionDto> appendices = new ArrayList<>();
        if (text.toLowerCase(Locale.ROOT).contains("appendix") || text.toLowerCase(Locale.ROOT).contains("appendices")) {
            appendices.add(new TemplateSectionDefinitionDto(
                    "Appendix A", "Source Code / System Artifacts", SectionRequirementLevel.OPTIONAL,
                    SectionSemanticPurpose.APPENDIX, SectionGenerationPolicy.DETERMINISTIC,
                    "System source code, architecture diagrams, or raw research instruments", List.of()
            ));
            appendices.add(new TemplateSectionDefinitionDto(
                    "Appendix B", "Questionnaires / Interview Guides", SectionRequirementLevel.OPTIONAL,
                    SectionSemanticPurpose.APPENDIX, SectionGenerationPolicy.DETERMINISTIC,
                    "Survey instruments, pilot results, or ethics approval letters", List.of()
            ));
        }
        return appendices;
    }

    private String resolveChapterTitle(int num, String suffix) {
        String numWords = switch (num) {
            case 1 -> "ONE";
            case 2 -> "TWO";
            case 3 -> "THREE";
            case 4 -> "FOUR";
            case 5 -> "FIVE";
            case 6 -> "SIX";
            default -> String.valueOf(num);
        };
        if (suffix != null && !suffix.isBlank()) {
            return "CHAPTER " + numWords + " — " + suffix.toUpperCase(Locale.ROOT);
        }
        return switch (num) {
            case 1 -> "CHAPTER ONE — INTRODUCTION";
            case 2 -> "CHAPTER TWO — LITERATURE REVIEW";
            case 3 -> "CHAPTER THREE — METHODOLOGY / SYSTEM DESIGN";
            case 4 -> "CHAPTER FOUR — RESULTS AND DISCUSSION";
            case 5 -> "CHAPTER FIVE — CONCLUSION AND RECOMMENDATIONS";
            default -> "CHAPTER " + numWords;
        };
    }

    private ReportChapterType resolveChapterType(int num, String title) {
        String lower = title.toLowerCase(Locale.ROOT);
        if (lower.contains("introduction")) return ReportChapterType.INTRODUCTION;
        if (lower.contains("literature")) return ReportChapterType.LITERATURE_REVIEW;
        if (lower.contains("method") || lower.contains("design") || lower.contains("software") || lower.contains("architecture")) return ReportChapterType.METHODOLOGY;
        if (lower.contains("result") || lower.contains("discussion") || lower.contains("finding") || lower.contains("evaluation")) return ReportChapterType.RESULTS;
        if (lower.contains("conclusion") || lower.contains("recommendation")) return ReportChapterType.CONCLUSION_RECOMMENDATIONS;
        return ReportChapterType.CUSTOM;
    }

    private SectionSemanticPurpose resolveSectionSemanticPurpose(String heading) {
        String lower = heading.toLowerCase(Locale.ROOT);
        if (lower.contains("background")) return SectionSemanticPurpose.BACKGROUND;
        if (lower.contains("problem") || lower.contains("statement of the problem")) return SectionSemanticPurpose.PROBLEM_STATEMENT;
        if (lower.contains("aim") || lower.contains("objective")) return SectionSemanticPurpose.OBJECTIVES;
        if (lower.contains("question")) return SectionSemanticPurpose.RESEARCH_QUESTIONS;
        if (lower.contains("hypothesis") || lower.contains("hypotheses")) return SectionSemanticPurpose.HYPOTHESES;
        if (lower.contains("significance") || lower.contains("justification")) return SectionSemanticPurpose.SIGNIFICANCE;
        if (lower.contains("scope") || lower.contains("delimitation") || lower.contains("limitation") || lower.contains("boundary")) return SectionSemanticPurpose.SCOPE;
        if (lower.contains("organization") || lower.contains("structure of the study")) return SectionSemanticPurpose.CUSTOM;
        if (lower.contains("literature") || lower.contains("review")) return SectionSemanticPurpose.LITERATURE_REVIEW;
        if (lower.contains("theoretical framework")) return SectionSemanticPurpose.THEORETICAL_FRAMEWORK;
        if (lower.contains("conceptual framework")) return SectionSemanticPurpose.CONCEPTUAL_FRAMEWORK;
        if (lower.contains("research gap")) return SectionSemanticPurpose.RESEARCH_GAP;
        if (lower.contains("related system") || lower.contains("existing system")) return SectionSemanticPurpose.RELATED_SYSTEMS;
        if (lower.contains("requirement") || lower.contains("specification")) return SectionSemanticPurpose.SYSTEM_REQUIREMENTS;
        if (lower.contains("architecture") || lower.contains("system design") || lower.contains("database design") || lower.contains("data design")) return SectionSemanticPurpose.SYSTEM_DESIGN;
        if (lower.contains("implementation") || lower.contains("module")) return SectionSemanticPurpose.IMPLEMENTATION;
        if (lower.contains("testing") || lower.contains("evaluation") || lower.contains("validation")) return SectionSemanticPurpose.TESTING;
        if (lower.contains("finding") || lower.contains("result")) return SectionSemanticPurpose.FINDINGS;
        if (lower.contains("discussion")) return SectionSemanticPurpose.DISCUSSION;
        if (lower.contains("conclusion")) return SectionSemanticPurpose.CONCLUSIONS;
        if (lower.contains("recommendation")) return SectionSemanticPurpose.RECOMMENDATIONS;
        if (lower.contains("methodology") || lower.contains("research design")) return SectionSemanticPurpose.METHODOLOGY;
        return SectionSemanticPurpose.CUSTOM;
    }

    private boolean isSectionOptional(String heading) {
        String lower = heading.toLowerCase(Locale.ROOT);
        return lower.contains("optional") || lower.contains("delimitation") || lower.contains("dedication") || lower.contains("future work");
    }

    private boolean isSectionRecommended(String heading) {
        String lower = heading.toLowerCase(Locale.ROOT);
        return lower.contains("significance") || lower.contains("limitation") || lower.contains("organization") || lower.contains("acknowledg");
    }

    private List<TemplateChapterDefinitionDto> buildStandardAcademicChapters(String docType) {
        boolean isSoftware = docType != null && docType.toLowerCase(Locale.ROOT).contains("project");

        List<TemplateSectionDefinitionDto> ch1Sections = List.of(
                new TemplateSectionDefinitionDto("1.1", "Background of the Study", SectionRequirementLevel.REQUIRED, SectionSemanticPurpose.BACKGROUND, SectionGenerationPolicy.SOURCE_GROUNDED_AI, "Contextual background and domain rationale", List.of()),
                new TemplateSectionDefinitionDto("1.2", "Problem Statement", SectionRequirementLevel.REQUIRED, SectionSemanticPurpose.PROBLEM_STATEMENT, SectionGenerationPolicy.SOURCE_GROUNDED_AI, "Clear statement of the practical or research gap", List.of()),
                new TemplateSectionDefinitionDto("1.3", "Aim and Objectives", SectionRequirementLevel.REQUIRED, SectionSemanticPurpose.OBJECTIVES, SectionGenerationPolicy.PROJECT_DERIVED_AI, "General aim and specific operational objectives", List.of()),
                new TemplateSectionDefinitionDto("1.4", "Research Questions", SectionRequirementLevel.RECOMMENDED, SectionSemanticPurpose.RESEARCH_QUESTIONS, SectionGenerationPolicy.PROJECT_DERIVED_AI, "Core investigation questions aligned with objectives", List.of()),
                new TemplateSectionDefinitionDto("1.5", "Significance of the Study", SectionRequirementLevel.RECOMMENDED, SectionSemanticPurpose.SIGNIFICANCE, SectionGenerationPolicy.PROJECT_DERIVED_AI, "Beneficiaries and academic/practical contributions", List.of()),
                new TemplateSectionDefinitionDto("1.6", "Scope and Delimitations", SectionRequirementLevel.RECOMMENDED, SectionSemanticPurpose.SCOPE, SectionGenerationPolicy.PROJECT_DERIVED_AI, "Boundaries and inclusions/exclusions", List.of()),
                new TemplateSectionDefinitionDto("1.7", "Organization of the Study", SectionRequirementLevel.RECOMMENDED, SectionSemanticPurpose.ORGANIZATION_OF_STUDY, SectionGenerationPolicy.PROJECT_DERIVED_AI, "Overview of subsequent chapters", List.of())
        );

        List<TemplateSectionDefinitionDto> ch2Sections = List.of(
                new TemplateSectionDefinitionDto("2.1", "Theoretical and Conceptual Review", SectionRequirementLevel.REQUIRED, SectionSemanticPurpose.LITERATURE_REVIEW, SectionGenerationPolicy.SOURCE_GROUNDED_AI, "Comprehensive synthesis across scholarly literature", List.of()),
                new TemplateSectionDefinitionDto("2.2", isSoftware ? "Review of Related Systems" : "Empirical Literature Review", SectionRequirementLevel.REQUIRED, isSoftware ? SectionSemanticPurpose.RELATED_SYSTEMS : SectionSemanticPurpose.LITERATURE_REVIEW, SectionGenerationPolicy.SOURCE_GROUNDED_AI, isSoftware ? "Evaluation of existing software platforms" : "Cross-study synthesis of empirical findings", List.of()),
                new TemplateSectionDefinitionDto("2.3", "Research Gaps and Synthesis", SectionRequirementLevel.REQUIRED, SectionSemanticPurpose.RESEARCH_GAP, SectionGenerationPolicy.SOURCE_GROUNDED_AI, "Identified methodological and practical gaps", List.of())
        );

        List<TemplateSectionDefinitionDto> ch3Sections = isSoftware ? List.of(
                new TemplateSectionDefinitionDto("3.1", "System Requirements and Specifications", SectionRequirementLevel.REQUIRED, SectionSemanticPurpose.SYSTEM_REQUIREMENTS, SectionGenerationPolicy.PROJECT_DERIVED_AI, "Functional and non-functional requirements", List.of()),
                new TemplateSectionDefinitionDto("3.2", "System Architecture and Design", SectionRequirementLevel.REQUIRED, SectionSemanticPurpose.SYSTEM_DESIGN, SectionGenerationPolicy.PROJECT_DERIVED_AI, "Architectural components, database design, and data flow", List.of()),
                new TemplateSectionDefinitionDto("3.3", "Implementation Strategy", SectionRequirementLevel.RECOMMENDED, SectionSemanticPurpose.IMPLEMENTATION, SectionGenerationPolicy.PROJECT_DERIVED_AI, "Technology stack, frameworks, and modules", List.of())
        ) : List.of(
                new TemplateSectionDefinitionDto("3.1", "Research Design", SectionRequirementLevel.REQUIRED, SectionSemanticPurpose.METHODOLOGY, SectionGenerationPolicy.PROJECT_DERIVED_AI, "Overall methodological framework and strategy", List.of()),
                new TemplateSectionDefinitionDto("3.2", "Population and Sampling", SectionRequirementLevel.REQUIRED, SectionSemanticPurpose.POPULATION_SAMPLING, SectionGenerationPolicy.PROJECT_DERIVED_AI, "Target population and sampling technique", List.of()),
                new TemplateSectionDefinitionDto("3.3", "Data Collection and Analysis Procedures", SectionRequirementLevel.REQUIRED, SectionSemanticPurpose.DATA_COLLECTION_METHOD, SectionGenerationPolicy.PROJECT_DERIVED_AI, "Data collection instruments and statistical methods", List.of())
        );

        List<TemplateSectionDefinitionDto> ch4Sections = isSoftware ? List.of(
                new TemplateSectionDefinitionDto("4.1", "System Testing and Evaluation", SectionRequirementLevel.REQUIRED, SectionSemanticPurpose.TESTING, SectionGenerationPolicy.PROJECT_EVIDENCE_REQUIRED, "Unit, integration, and user acceptance test results", List.of()),
                new TemplateSectionDefinitionDto("4.2", "Results and Discussion", SectionRequirementLevel.REQUIRED, SectionSemanticPurpose.FINDINGS, SectionGenerationPolicy.PROJECT_EVIDENCE_REQUIRED, "System verification outcomes and comparison with objectives", List.of())
        ) : List.of(
                new TemplateSectionDefinitionDto("4.1", "Data Analysis and Findings", SectionRequirementLevel.REQUIRED, SectionSemanticPurpose.FINDINGS, SectionGenerationPolicy.PROJECT_EVIDENCE_REQUIRED, "Empirical findings grounded in study data", List.of()),
                new TemplateSectionDefinitionDto("4.2", "Discussion of Findings", SectionRequirementLevel.REQUIRED, SectionSemanticPurpose.DISCUSSION, SectionGenerationPolicy.SOURCE_GROUNDED_AI, "Interpretation of results in light of literature", List.of())
        );

        List<TemplateSectionDefinitionDto> ch5Sections = List.of(
                new TemplateSectionDefinitionDto("5.1", "Summary of Findings", SectionRequirementLevel.REQUIRED, SectionSemanticPurpose.CONCLUSIONS, SectionGenerationPolicy.PROJECT_DERIVED_AI, "Executive summary of major achievements", List.of()),
                new TemplateSectionDefinitionDto("5.2", "Conclusions", SectionRequirementLevel.REQUIRED, SectionSemanticPurpose.CONCLUSIONS, SectionGenerationPolicy.PROJECT_DERIVED_AI, "Scholarly conclusions answering research questions", List.of()),
                new TemplateSectionDefinitionDto("5.3", "Recommendations", SectionRequirementLevel.REQUIRED, SectionSemanticPurpose.RECOMMENDATIONS, SectionGenerationPolicy.PROJECT_DERIVED_AI, "Actionable recommendations and practical implications", List.of())
        );

        return List.of(
                new TemplateChapterDefinitionDto(1, "CHAPTER ONE — INTRODUCTION", ReportChapterType.INTRODUCTION, true, ch1Sections),
                new TemplateChapterDefinitionDto(2, "CHAPTER TWO — LITERATURE REVIEW", ReportChapterType.LITERATURE_REVIEW, true, ch2Sections),
                new TemplateChapterDefinitionDto(3, isSoftware ? "CHAPTER THREE — SYSTEM DESIGN" : "CHAPTER THREE — METHODOLOGY", ReportChapterType.METHODOLOGY, true, ch3Sections),
                new TemplateChapterDefinitionDto(4, "CHAPTER FOUR — RESULTS AND DISCUSSIONS", ReportChapterType.RESULTS, true, ch4Sections),
                new TemplateChapterDefinitionDto(5, "CHAPTER FIVE — CONCLUSION AND RECOMMENDATIONS", ReportChapterType.CONCLUSION_RECOMMENDATIONS, true, ch5Sections)
        );
    }

    private ExtractedAcademicTemplate fallbackTemplate(String fileName) {
        return new ExtractedAcademicTemplate(
                NOT_SPECIFIED,
                NOT_SPECIFIED,
                NOT_SPECIFIED,
                "Project Report",
                "APA_7",
                List.of(),
                buildStandardAcademicChapters("Project Report"),
                List.of(),
                TemplateFormattingRulesDto.defaultUnspecified(),
                List.of(new UncertainItemDto("FILE", fileName, "Could not extract plain text from file", 0.1))
        );
    }
}
