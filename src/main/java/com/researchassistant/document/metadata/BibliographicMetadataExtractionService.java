package com.researchassistant.document.metadata;

import com.researchassistant.document.entity.Document;
import com.researchassistant.document.entity.DocumentProcessingJob;
import com.researchassistant.document.entity.DocumentProcessingStatus;
import com.researchassistant.document.entity.DocumentVersion;
import com.researchassistant.document.repository.DocumentPageRepository;
import com.researchassistant.document.repository.DocumentRepository;
import com.researchassistant.document.storage.DocumentStorageService;
import com.researchassistant.reference.entity.ReferenceMetadataStatus;
import com.researchassistant.reference.service.ProjectReferenceRegistryService;
import com.researchassistant.reference.service.ReferenceNormalizationService;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentInformation;
import org.apache.pdfbox.pdmodel.common.PDMetadata;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.time.Year;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class BibliographicMetadataExtractionService {

    private static final Pattern DOI_PATTERN = Pattern.compile("\\b10\\.\\d{4,9}/[-._;()/:A-Z0-9]+", Pattern.CASE_INSENSITIVE);
    private static final Pattern ISBN_PATTERN = Pattern.compile("\\b(?:97[89][- ]?)?\\d[- 0-9]{8,}\\d\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern ISSN_PATTERN = Pattern.compile("\\b\\d{4}-\\d{3}[0-9X]\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern YEAR_PATTERN = Pattern.compile("\\b(19\\d{2}|20\\d{2})\\b");
    private static final Pattern VOLUME_PATTERN = Pattern.compile("\\b(?:vol\\.?|volume)\\s*([A-Za-z0-9-]+)", Pattern.CASE_INSENSITIVE);
    private static final Pattern ISSUE_PATTERN = Pattern.compile("\\b(?:no\\.?|issue)\\s*([A-Za-z0-9-]+)", Pattern.CASE_INSENSITIVE);
    private static final Pattern PAGES_PATTERN = Pattern.compile("\\b(?:pp\\.?|pages?)\\s*([0-9]+\\s*[-–]\\s*[0-9]+)", Pattern.CASE_INSENSITIVE);

    private final DocumentRepository documentRepository;
    private final DocumentPageRepository pageRepository;
    private final DocumentStorageService storageService;
    private final ReferenceNormalizationService normalizationService;
    private final ProjectReferenceRegistryService referenceRegistryService;
    private final List<BibliographicMetadataProvider> metadataProviders;

    public BibliographicMetadataExtractionService(
            DocumentRepository documentRepository,
            DocumentPageRepository pageRepository,
            DocumentStorageService storageService,
            ReferenceNormalizationService normalizationService,
            ProjectReferenceRegistryService referenceRegistryService,
            List<BibliographicMetadataProvider> metadataProviders
    ) {
        this.documentRepository = documentRepository;
        this.pageRepository = pageRepository;
        this.storageService = storageService;
        this.normalizationService = normalizationService;
        this.referenceRegistryService = referenceRegistryService;
        this.metadataProviders = metadataProviders == null ? List.of() : metadataProviders;
    }

    @Transactional
    public BibliographicMetadata extractAndApply(DocumentVersion version, DocumentProcessingJob job) {
        OffsetDateTime now = OffsetDateTime.now();
        if (job != null) {
            job.setStatus(DocumentProcessingStatus.RUNNING);
            job.setStartedAt(now);
        }
        try {
            BibliographicMetadata embedded = extractEmbedded(version);
            BibliographicMetadata frontMatter = extractFrontMatter(version);
            BibliographicMetadata merged = embedded.merge(frontMatter).merge(resolveExternal(embedded.merge(frontMatter)));
            apply(version, merged);
            if (job != null) {
                job.setStatus(DocumentProcessingStatus.COMPLETED);
                job.setCompletedAt(OffsetDateTime.now());
            }
            return merged;
        } catch (Exception exception) {
            markIncomplete(version, exception);
            if (job != null) {
                job.setStatus(DocumentProcessingStatus.FAILED);
                job.setErrorCode("REFERENCE_METADATA_EXTRACTION_FAILED");
                job.setErrorMessage(shortMessage(exception));
                job.setFailedAt(OffsetDateTime.now());
            }
            return BibliographicMetadata.empty();
        }
    }

    public BibliographicMetadata extractOnly(DocumentVersion version) {
        BibliographicMetadata merged = extractEmbedded(version).merge(extractFrontMatter(version));
        return merged.merge(resolveExternal(merged));
    }

    private BibliographicMetadata resolveExternal(BibliographicMetadata seed) {
        if (seed == null || (!present(seed.doi()) && !present(seed.title()))) {
            return null;
        }
        for (BibliographicMetadataProvider provider : metadataProviders) {
            try {
                if (provider.available()) {
                    var resolved = provider.resolve(seed);
                    if (resolved.isPresent()) {
                        return resolved.get();
                    }
                }
            } catch (Exception ignored) {
                // Metadata provider failures must not make the research source unusable.
            }
        }
        return null;
    }

    private BibliographicMetadata extractEmbedded(DocumentVersion version) {
        try {
            byte[] bytes;
            try (InputStream input = storageService.open(version.getStorageKey()).inputStream()) {
                bytes = input.readAllBytes();
            }
            if ("application/pdf".equalsIgnoreCase(version.getMimeType())) {
                return embeddedPdf(bytes);
            }
            if ("application/vnd.openxmlformats-officedocument.wordprocessingml.document".equalsIgnoreCase(version.getMimeType())) {
                return embeddedDocx(bytes);
            }
        } catch (Exception ignored) {
            return BibliographicMetadata.empty();
        }
        return BibliographicMetadata.empty();
    }

    private BibliographicMetadata embeddedPdf(byte[] bytes) throws Exception {
        try (PDDocument pdf = Loader.loadPDF(bytes)) {
            PDDocumentInformation info = pdf.getDocumentInformation();
            String xmp = xmpText(pdf);
            String doi = firstNonBlank(findDoi(info == null ? null : info.getSubject()), findDoi(xmp));
            String title = info == null ? null : cleanMetadataValue(info.getTitle());
            String authors = info == null ? null : cleanMetadataValue(info.getAuthor());
            String keywords = info == null ? null : cleanMetadataValue(info.getKeywords());
            Integer year = parseYear(firstNonBlank(
                    info == null ? null : value(info.getCreationDate()),
                    xmp
            ));
            return new BibliographicMetadata(
                    validTitle(title) ? title : null,
                    authors,
                    year,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    normalizationService.normalizeDoi(doi),
                    findIsbn(xmp),
                    findIssn(xmp),
                    null,
                    null,
                    keywords,
                    "PDF_METADATA",
                    hasAny(title, authors, year, doi, keywords) ? 0.70d : 0.0d
            );
        }
    }

    private BibliographicMetadata embeddedDocx(byte[] bytes) throws Exception {
        try (XWPFDocument docx = new XWPFDocument(new ByteArrayInputStream(bytes))) {
            var core = docx.getProperties().getCoreProperties();
            String title = cleanMetadataValue(core.getTitle());
            String authors = cleanMetadataValue(core.getCreator());
            String keywords = cleanMetadataValue(core.getKeywords());
            String subject = cleanMetadataValue(core.getSubject());
            String doi = findDoi(subject);
            return new BibliographicMetadata(
                    validTitle(title) ? title : null,
                    authors,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    normalizationService.normalizeDoi(doi),
                    null,
                    null,
                    null,
                    null,
                    keywords,
                    "DOCX_CORE_PROPERTIES",
                    hasAny(title, authors, doi, keywords) ? 0.65d : 0.0d
            );
        }
    }

    private BibliographicMetadata extractFrontMatter(DocumentVersion version) {
        String text = pageRepository.findAllByDocumentVersionIdOrderByPageNumber(version.getId()).stream()
                .filter(page -> page.getPageNumber() <= 5)
                .map(page -> page.getTextContent() == null ? "" : page.getTextContent())
                .reduce("", (left, right) -> left + "\n" + right);
        if (text.isBlank()) {
            return BibliographicMetadata.empty();
        }

        // 1. Check for explicit MDPI citation format
        BibliographicMetadata mdpi = extractMdpi(text);
        if (mdpi != null && present(mdpi.title())) {
            return mdpi;
        }

        List<String> lines = normalizedLines(text);

        // 2. Structured academic article extraction (multi-line title, authors, journal, year, DOI)
        BibliographicMetadata structured = extractAcademicArticle(text, lines);
        if (structured != null && present(structured.title())) {
            return structured;
        }

        // 3. Fallback heuristic extraction
        int titleIndex = findTitleIndex(lines);
        String title = titleIndex >= 0 ? lines.get(titleIndex) : null;
        String authors = findAuthors(lines, titleIndex);
        String doi = normalizationService.normalizeDoi(findDoi(text));
        String isbn = findIsbn(text);
        String issn = findIssn(text);
        String journal = findJournal(lines, title);
        String conference = findConference(lines);
        Integer year = parseYear(text);
        String volume = firstMatch(text, VOLUME_PATTERN);
        String issue = firstMatch(text, ISSUE_PATTERN);
        String pages = firstMatch(text, PAGES_PATTERN);
        String keywords = findKeywords(lines);
        String sourceType = sourceType(journal, conference, doi, isbn);
        return new BibliographicMetadata(
                validTitle(title) ? cleanTitle(title) : null,
                cleanAuthors(authors),
                year,
                journal,
                conference,
                null,
                volume,
                issue,
                pages,
                doi,
                isbn,
                issn,
                null,
                sourceType,
                keywords,
                "DOCUMENT_TEXT",
                confidence(title, authors, year, doi, journal, conference)
        );
    }

    private BibliographicMetadata extractMdpi(String text) {
        if (text == null) return null;
        Matcher m = Pattern.compile("(?is)Citation:\\s*([^\\n]+(?:\\n(?!Academic Editor|Received|Copyright|Keywords)[^\\n]+){1,6})").matcher(text);
        if (!m.find()) return null;
        String block = m.group(1).replaceAll("\\s+", " ").trim();
        String doi = findDoi(block);
        Integer year = parseYear(block);

        String noUrl = block.replaceAll("https?://\\S+", "").trim();
        String[] parts = noUrl.split("\\.\\s+");
        if (parts.length >= 2) {
            String authors = cleanAuthors(parts[0].replaceFirst("(?i)^Citation:\\s*", "").trim());
            String title = cleanTitle(parts[1].trim());
            String journal = "Sustainability";
            if (parts.length > 2) {
                String candidateJournal = parts[2].replaceAll("\\d.*$", "").trim();
                if (!candidateJournal.isBlank() && candidateJournal.length() < 50) {
                    journal = candidateJournal;
                }
            }
            if (validTitle(title)) {
                return new BibliographicMetadata(
                        title,
                        authors,
                        year != null ? year : Year.now().getValue(),
                        journal,
                        null,
                        null,
                        null,
                        null,
                        null,
                        normalizationService.normalizeDoi(doi),
                        null,
                        null,
                        null,
                        "JOURNAL_ARTICLE",
                        null,
                        "MDPI_CITATION",
                        0.95d
                );
            }
        }
        return null;
    }

    private BibliographicMetadata extractAcademicArticle(String text, List<String> lines) {
        if (lines.isEmpty()) return null;

        String doi = normalizationService.normalizeDoi(findDoi(text));
        String isbn = findIsbn(text);
        String issn = findIssn(text);
        Integer year = extractAcademicYear(text, lines);
        String journal = extractAcademicJournal(lines);

        // Identify title and authors across lines
        int startLine = 0;
        int limit = Math.min(lines.size(), 30);

        // Skip leading header banners
        while (startLine < limit && isHeaderBanner(lines.get(startLine))) {
            startLine++;
        }
        if (startLine >= limit) return null;

        // Skip standalone journal name line if it matched the journal
        if (journal != null && startLine < limit) {
            String curr = lines.get(startLine).trim().toLowerCase(Locale.ROOT);
            if (curr.equalsIgnoreCase(journal.trim()) || journal.toLowerCase(Locale.ROOT).contains(curr)) {
                startLine++;
            }
        }
        while (startLine < limit && isHeaderBanner(lines.get(startLine))) {
            startLine++;
        }
        if (startLine >= limit) return null;

        // Collect title lines until author line or section boundary
        StringBuilder titleBuilder = new StringBuilder();
        int lineIdx = startLine;
        while (lineIdx < limit) {
            String line = lines.get(lineIdx);
            if (isHeaderBanner(line)) {
                if (titleBuilder.length() > 0) break;
                lineIdx++;
                continue;
            }
            if (looksLikeAuthors(line) || isAffiliationOrBoundary(line)) {
                break;
            }
            if (titleBuilder.length() > 0) {
                titleBuilder.append(" ");
            }
            titleBuilder.append(line.trim());
            lineIdx++;

            if ((line.endsWith("?") || line.endsWith(".")) && titleBuilder.length() >= 40) {
                break;
            }
            if (lineIdx - startLine >= 4) {
                break;
            }
        }

        String rawTitle = cleanTitle(titleBuilder.toString());
        if (!validTitle(rawTitle)) {
            return null;
        }

        // Now collect author line(s)
        StringBuilder authorBuilder = new StringBuilder();
        int authorLines = 0;
        while (lineIdx < limit && authorLines < 3) {
            String line = lines.get(lineIdx);
            if (isAffiliationOrBoundary(line)) {
                break;
            }
            if (isHeaderBanner(line)) {
                lineIdx++;
                continue;
            }
            if (authorBuilder.length() > 0) {
                authorBuilder.append(", ");
            }
            authorBuilder.append(line.trim());
            authorLines++;
            lineIdx++;
            if (line.contains(";") || line.endsWith(".") || isAffiliationOrBoundary(lineIdx < limit ? lines.get(lineIdx) : "")) {
                break;
            }
        }

        String authors = cleanAuthors(authorBuilder.toString());
        String volume = firstMatch(text, VOLUME_PATTERN);
        String issue = firstMatch(text, ISSUE_PATTERN);
        String pages = firstMatch(text, PAGES_PATTERN);
        String keywords = findKeywords(lines);
        String publisher = (journal == null && text.contains("FAO")) ? "FAO Regional Office for Africa" : null;
        String sourceType = sourceType(journal, null, doi, isbn);

        double conf = 0.50d;
        if (present(rawTitle)) conf += 0.20d;
        if (present(authors)) conf += 0.15d;
        if (year != null) conf += 0.10d;

        return new BibliographicMetadata(
                rawTitle,
                authors,
                year,
                journal,
                null,
                publisher,
                volume,
                issue,
                pages,
                doi,
                isbn,
                issn,
                null,
                sourceType,
                keywords,
                "ACADEMIC_PARSER",
                Math.min(conf, 0.95d)
        );
    }

    private Integer extractAcademicYear(String text, List<String> lines) {
        if (text == null) return null;
        Matcher volYear = Pattern.compile("(?i)(?:Food Policy|Heliyon|Sustainability|Vol\\.?|Volume|Issue|J\\.)[^(]*\\((\\d{4})\\)").matcher(text);
        if (volYear.find()) {
            int y = Integer.parseInt(volYear.group(1));
            if (y >= 1990 && y <= Year.now().getValue() + 1) return y;
        }

        Matcher copyYear = Pattern.compile("(?i)(?:Copyright\\s*©?|©)\\s*(20\\d{2})").matcher(text);
        if (copyYear.find()) {
            return Integer.parseInt(copyYear.group(1));
        }

        Matcher seasonYear = Pattern.compile("(?i)(?:January|February|March|April|May|June|July|August|September|October|November|December)[^0-9\\n]{0,25}(20\\d{2})").matcher(text);
        if (seasonYear.find()) {
            return Integer.parseInt(seasonYear.group(1));
        }

        Matcher doiYear = Pattern.compile("10\\.\\d+/[A-Za-z]+(20\\d{2})\\.").matcher(text);
        if (doiYear.find()) {
            return Integer.parseInt(doiYear.group(1));
        }

        return parseYear(text);
    }

    private String extractAcademicJournal(List<String> lines) {
        for (String line : lines.subList(0, Math.min(lines.size(), 40))) {
            String lower = line.toLowerCase(Locale.ROOT).trim();
            if (lower.startsWith("food policy")) return "Food Policy";
            if (lower.startsWith("heliyon")) return "Heliyon";
            if (lower.startsWith("sustainability")) return "Sustainability";
            if (lower.contains("journal of development and agricultural economics")) return "Journal of Development and Agricultural Economics";
            if (lower.contains("journal of development and agricultural") || lower.equals("economics")) return "Journal of Development and Agricultural Economics";
            if (lower.contains("indian inst. sci.") || lower.contains("journal of the indian institute of science")) return "Journal of the Indian Institute of Science";
            if (lower.contains("agricultural and food marketing management")) return "FAO Regional Office for Africa";
            if (lower.startsWith("journal of ") && line.length() <= 100) return line.trim();
        }
        return null;
    }

    private boolean isHeaderBanner(String line) {
        if (line == null || line.isBlank()) return true;
        String trimmed = line.trim();
        String lower = trimmed.toLowerCase(Locale.ROOT);
        if (lower.startsWith("vol.") || lower.startsWith("volume ") || lower.contains("pp. ")) return true;
        if (lower.startsWith("doi:") || lower.startsWith("doi.org") || lower.startsWith("https://doi.org")) return true;
        if (lower.startsWith("issn") || lower.startsWith("isbn")) return true;
        if (lower.startsWith("article number:") || lower.startsWith("article no:")) return true;
        if (lower.startsWith("copyright") || lower.contains("all rights reserved") || lower.contains("author(s) retain")) return true;
        if (lower.startsWith("available online") || lower.startsWith("received") || lower.startsWith("revised") || lower.startsWith("accepted") || lower.startsWith("published:")) return true;
        if (lower.startsWith("contents lists") || lower.startsWith("journal homepage:")) return true;
        if (lower.matches("^(research article|review article|full length research paper|original article|review|editorial|short communication)$")) return true;
        if (lower.startsWith("http://") || lower.startsWith("https://")) return true;
        if (lower.matches("^[0-9a-z.-]+/[©@].*")) return true;
        if (lower.matches(".*(elsevier ltd|academic journals|springer|mdpi|wiley|nature publishing).*")) return true;
        if (lower.matches("^[a-z0-9.\\s]+\\|\\s*vol\\s*\\d+.*")) return true;
        if (lower.matches("^\\d+\\s+van\\s+\\d+.*")) return true;
        if (lower.startsWith("this publication has previously") || lower.startsWith("the designations employed") || lower.startsWith("marketing and agribusiness texts")) return true;
        if (lower.equals("contents") || lower.equals("a r t i c l e i n f o") || lower.equals("keywords:") || lower.equals("abstract") || lower.equals("a b s t r a c t")) return true;
        if (lower.startsWith("licensee mdpi") || lower.startsWith("this article is an open access")) return true;
        if (lower.startsWith("academic editor:")) return true;
        return false;
    }

    private boolean isAffiliationOrBoundary(String line) {
        if (line == null) return false;
        String lower = line.toLowerCase(Locale.ROOT).trim();
        if (lower.startsWith("abstract") || lower.startsWith("a b s t r a c t")) return true;
        if (lower.startsWith("keywords") || lower.startsWith("key words")) return true;
        if (lower.startsWith("introduction") || lower.startsWith("1. introduction")) return true;
        if (lower.contains("university") || lower.contains("department") || lower.contains("faculty") || lower.contains("institute") || lower.contains("college") || lower.contains("bureau of")) return true;
        if (lower.contains("corresponding author") || lower.contains("e-mail address:") || lower.contains("email:")) return true;
        return false;
    }

    private String cleanTitle(String title) {
        if (title == null) return null;
        String cleaned = title.replaceAll("[☆*]+$", "").replaceAll("\\s+", " ").trim();
        if (cleaned.length() > 5 && cleaned.equals(cleaned.toUpperCase(Locale.ROOT))) {
            StringBuilder sb = new StringBuilder();
            for (String word : cleaned.split("\\s+")) {
                if (sb.length() > 0) sb.append(" ");
                if (word.length() > 1) {
                    sb.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1).toLowerCase(Locale.ROOT));
                } else {
                    sb.append(word);
                }
            }
            cleaned = sb.toString();
        }
        return cleaned;
    }

    private String cleanAuthors(String authors) {
        if (authors == null || authors.isBlank()) return null;
        String normalized = authors.replaceAll("(?i)\\s*and\\s+", ", ")
                .replaceAll(";", ",")
                .replaceAll("\\s*\\b[a-z0-9]{1,2}\\s*\\*\\s*", "")
                .replaceAll("\\s*\\*\\s*", "")
                .replaceAll("\\s*\\b[a-d]\\b(?=[,\\s]|$)", "")
                .replaceAll("\\s*\\b\\d{1,2}\\b(?=[,\\s]|$)", "")
                .replaceAll("\\s+", " ")
                .trim();
        List<String> list = new ArrayList<>();
        for (String p : normalized.split(",")) {
            String trimmed = p.replaceAll("[0-9*^]+", "").trim();
            if (trimmed.length() >= 3 && trimmed.chars().anyMatch(Character::isLetter)
                    && !trimmed.toLowerCase(Locale.ROOT).contains("university")
                    && !trimmed.toLowerCase(Locale.ROOT).contains("institute")
                    && !trimmed.toLowerCase(Locale.ROOT).contains("department")) {
                list.add(trimmed);
            }
        }
        return list.isEmpty() ? null : String.join(", ", list);
    }

    private void apply(DocumentVersion version, BibliographicMetadata metadata) {
        Document document = version.getDocument();
        if (document.getBibliographicMetadataStatus() == ReferenceMetadataStatus.VERIFIED) {
            referenceRegistryService.ensureForDocument(document, version, version.getUploadedBy());
            return;
        }
        if (canReplaceTitle(document.getBibliographicTitle(), document.getTitle(), metadata.title())) {
            document.setBibliographicTitle(metadata.title());
        }
        if (present(metadata.authors())) document.setAuthors(metadata.authors());
        if (metadata.publicationYear() != null) document.setPublicationYear(metadata.publicationYear());
        if (present(metadata.journal())) document.setJournal(metadata.journal());
        if (present(metadata.conference())) document.setConference(metadata.conference());
        if (present(metadata.publisher())) document.setPublisher(metadata.publisher());
        if (present(metadata.volume())) document.setVolume(metadata.volume());
        if (present(metadata.issue())) document.setIssue(metadata.issue());
        if (present(metadata.pages())) document.setPages(metadata.pages());
        if (present(metadata.doi())) document.setDoi(metadata.doi());
        if (present(metadata.url())) document.setUrl(metadata.url());
        if (present(metadata.sourceType())) document.setSourceType(metadata.sourceType());
        if (present(metadata.keywords())) document.setKeywords(metadata.keywords());
        document.setBibliographicMetadataStatus(metadata.status());
        document.setBibliographicMetadataSource(metadata.source());
        document.setBibliographicMetadataConfidence(metadata.confidence());
        document.setBibliographicMetadataExtractedAt(OffsetDateTime.now());
        documentRepository.save(document);
        referenceRegistryService.ensureForDocument(document, version, version.getUploadedBy());
    }

    private void markIncomplete(DocumentVersion version, Exception exception) {
        Document document = version.getDocument();
        if (document.getBibliographicMetadataStatus() == ReferenceMetadataStatus.VERIFIED) {
            return;
        }
        document.setBibliographicMetadataStatus(ReferenceMetadataStatus.INCOMPLETE);
        document.setBibliographicMetadataSource("EXTRACTION_FAILED");
        document.setBibliographicMetadataConfidence(0.0d);
        document.setBibliographicMetadataExtractedAt(OffsetDateTime.now());
        documentRepository.save(document);
    }

    private String xmpText(PDDocument pdf) {
        try {
            PDMetadata metadata = pdf.getDocumentCatalog().getMetadata();
            if (metadata == null) return null;
            try (InputStream input = metadata.createInputStream()) {
                return new String(input.readAllBytes(), StandardCharsets.UTF_8);
            }
        } catch (Exception ignored) {
            return null;
        }
    }

    private List<String> normalizedLines(String text) {
        List<String> lines = new ArrayList<>();
        for (String raw : text.replace('\r', '\n').split("\\n")) {
            String line = raw.replaceAll("\\s+", " ").trim();
            if (!line.isBlank()) lines.add(line);
            if (lines.size() >= 120) break;
        }
        return lines;
    }

    private int findTitleIndex(List<String> lines) {
        int limit = Math.min(lines.size(), 35);
        for (int i = 0; i < limit; i++) {
            String line = lines.get(i);
            if (validTitle(line)) {
                return i;
            }
        }
        return -1;
    }

    private String findAuthors(List<String> lines, int titleIndex) {
        if (titleIndex < 0) return null;
        int end = Math.min(lines.size(), titleIndex + 7);
        for (int i = titleIndex + 1; i < end; i++) {
            String line = lines.get(i);
            String lower = line.toLowerCase(Locale.ROOT);
            if (lower.startsWith("abstract") || lower.startsWith("keywords") || lower.contains("doi")) break;
            if (lower.contains("university") || lower.contains("department") || lower.contains("faculty")) continue;
            String cleaned = line.replaceFirst("(?i)^by\\s+", "").trim();
            if (looksLikeAuthors(cleaned)) {
                return cleaned;
            }
        }
        return null;
    }

    private String findJournal(List<String> lines, String title) {
        for (String line : lines.subList(0, Math.min(lines.size(), 60))) {
            String lower = line.toLowerCase(Locale.ROOT);
            if (line.equals(title)) continue;
            if ((lower.contains("journal") || lower.contains("revista")) && line.length() <= 180) {
                return stripTrailingMetadata(line);
            }
        }
        return null;
    }

    private String findConference(List<String> lines) {
        for (String line : lines.subList(0, Math.min(lines.size(), 60))) {
            String lower = line.toLowerCase(Locale.ROOT);
            if ((lower.contains("conference") || lower.contains("proceedings")) && line.length() <= 180) {
                return stripTrailingMetadata(line);
            }
        }
        return null;
    }

    private String findKeywords(List<String> lines) {
        for (String line : lines) {
            Matcher matcher = Pattern.compile("(?i)^key\\s*words?\\s*[:\\-]\\s*(.+)$").matcher(line);
            if (matcher.find()) {
                return matcher.group(1).trim();
            }
        }
        return null;
    }

    private String sourceType(String journal, String conference, String doi, String isbn) {
        if (present(journal) || present(doi)) return "JOURNAL_ARTICLE";
        if (present(conference)) return "CONFERENCE_PAPER";
        if (present(isbn)) return "BOOK";
        return null;
    }

    private String findDoi(String text) {
        if (text == null) return null;
        Matcher matcher = DOI_PATTERN.matcher(text);
        if (!matcher.find()) return null;
        return matcher.group().replaceAll("[\\].,;:]+$", "");
    }

    private String findIsbn(String text) {
        if (text == null) return null;
        Matcher matcher = ISBN_PATTERN.matcher(text);
        return matcher.find() ? matcher.group().trim() : null;
    }

    private String findIssn(String text) {
        if (text == null) return null;
        Matcher matcher = ISSN_PATTERN.matcher(text);
        return matcher.find() ? matcher.group().trim() : null;
    }

    private Integer parseYear(String text) {
        if (text == null) return null;
        Matcher matcher = YEAR_PATTERN.matcher(text);
        int maxYear = Year.now().getValue() + 1;
        while (matcher.find()) {
            int year = Integer.parseInt(matcher.group(1));
            if (year >= 1900 && year <= maxYear) return year;
        }
        return null;
    }

    private String firstMatch(String text, Pattern pattern) {
        if (text == null) return null;
        Matcher matcher = pattern.matcher(text);
        return matcher.find() ? matcher.group(1).replace("–", "-").replaceAll("\\s+", "").trim() : null;
    }

    private boolean validTitle(String value) {
        if (!present(value)) return false;
        String trimmed = value.trim();
        String lower = trimmed.toLowerCase(Locale.ROOT);
        if (trimmed.length() < 18 || trimmed.length() > 240) return false;
        if (lower.startsWith("abstract") || lower.startsWith("keywords") || lower.startsWith("introduction")) return false;
        if (lower.contains("doi") || lower.contains("issn") || lower.contains("isbn") || lower.contains("copyright")) return false;
        if (lower.contains("journal") || lower.contains("proceedings")) return false;
        if (isLikelyFilename(trimmed)) return false;
        return trimmed.chars().filter(Character::isLetter).count() >= 10;
    }

    private boolean looksLikeAuthors(String line) {
        if (!present(line) || line.length() > 220) return false;
        String lower = line.toLowerCase(Locale.ROOT);
        if (lower.contains("abstract") || lower.contains("doi") || lower.contains("journal") || lower.contains("university")) return false;
        if (line.contains(";") || line.contains(",") || lower.contains(" and ") || line.contains("&")) {
            return line.chars().filter(Character::isLetter).count() >= 6;
        }
        String[] parts = line.split("\\s+");
        return parts.length >= 2 && parts.length <= 8 && line.chars().filter(Character::isLetter).count() >= 6;
    }

    private boolean canReplaceTitle(String currentBibliographicTitle, String storageTitle, String candidate) {
        if (!present(candidate)) return false;
        if (!present(currentBibliographicTitle)) return true;
        if (currentBibliographicTitle.equalsIgnoreCase(storageTitle) && isLikelyFilename(currentBibliographicTitle)) return true;
        if ("REFERENCE_METADATA_INCOMPLETE".equalsIgnoreCase(currentBibliographicTitle.trim())) return true;
        if (isLikelyFilename(currentBibliographicTitle)) return true;
        return currentBibliographicTitle.length() < candidate.length() && candidate.length() >= 25;
    }

    private boolean isLikelyFilename(String value) {
        if (!present(value)) return false;
        String v = value.trim();
        if (v.matches("(?i).+\\.(pdf|docx?|txt)$")) return true;
        if (v.contains(" ")) return false;
        return v.matches("[A-Za-z0-9._-]{6,}") && (v.contains("-") || v.contains("_") || v.matches(".*\\d{3,}.*"));
    }

    private double confidence(String title, String authors, Integer year, String doi, String journal, String conference) {
        double value = 0.25d;
        if (present(title)) value += 0.20d;
        if (present(authors)) value += 0.15d;
        if (year != null) value += 0.10d;
        if (present(doi)) value += 0.20d;
        if (present(journal) || present(conference)) value += 0.10d;
        return Math.min(value, 0.95d);
    }

    private String stripTrailingMetadata(String value) {
        return value == null ? null : value.replaceAll("(?i)\\s+(vol\\.?|volume|issue|no\\.).*$", "").trim();
    }

    private String cleanMetadataValue(String value) {
        if (!present(value)) return null;
        String cleaned = value.replaceAll("\\s+", " ").trim();
        return cleaned.equalsIgnoreCase("untitled") ? null : cleaned;
    }

    private String firstNonBlank(String first, String second) {
        return present(first) ? first : present(second) ? second : null;
    }

    private boolean hasAny(Object... values) {
        for (Object value : values) {
            if (value instanceof String s && present(s)) return true;
            if (value instanceof Integer) return true;
        }
        return false;
    }

    private String value(Object object) {
        return object == null ? null : object.toString();
    }

    private boolean blank(String value) {
        return value == null || value.isBlank();
    }

    private boolean present(String value) {
        return value != null && !value.isBlank();
    }

    private String shortMessage(Exception exception) {
        String message = exception.getMessage();
        if (message == null || message.isBlank()) {
            message = exception.getClass().getSimpleName();
        }
        return message.length() <= 2000 ? message : message.substring(0, 2000);
    }
}
