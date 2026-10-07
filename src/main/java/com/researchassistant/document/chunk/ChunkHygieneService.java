package com.researchassistant.document.chunk;

import com.researchassistant.document.entity.ChunkSemanticType;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Classifies document chunks and sanitizes retrieval artifacts such as journal headers,
 * "Available online...", copyright notices, DOIs, and bibliographic metadata.
 */
@Service
public class ChunkHygieneService {

    private static final Pattern FOOTER_HEADER_SNIPPET = Pattern.compile(
            "(?i)(?:available\\s+online\\s+at|journal\\s+homepage|contents\\s+lists\\s+available\\s+at|" +
            "all\\s+rights\\s+reserved|elsevier\\s+(?:b\\.v\\.|ltd)|springer\\s+nature|wiley\\s+periodicals|" +
            "ieee\\s+transactions|published\\s+by\\s+elsevier|https?:\\/\\/doi\\.org\\/10\\.\\d{4,9}\\/|" +
            "issn[\\s:]+\\d{4}-\\d{3}[\\dxX]|\\bpage\\s+\\d+\\s+of\\s+\\d+\\b|^\\s*page\\s+\\d+\\s*$)"
    );

    private static final Pattern METADATA_SNIPPET = Pattern.compile(
            "(?i)(?:received\\s+\\d{1,2}\\s+[a-z]+\\s+\\d{4}|accepted\\s+\\d{1,2}\\s+[a-z]+\\s+\\d{4}|" +
            "revised\\s+\\d{1,2}\\s+[a-z]+\\s+\\d{4}|article\\s+history|corresponding\\s+author|" +
            "\\bkeywords\\s*:[^\\n]{10,})"
    );

    private static final Pattern REFERENCES_HEADING = Pattern.compile(
            "(?i)^\\s*(?:#{1,4}\\s*)?(?:references|bibliography|works\\s+cited|literature\\s+cited)\\s*$"
    );

    private static final Pattern REFERENCE_ENTRY_LINE = Pattern.compile(
            "^\\s*(?:\\[\\d+\\]|\\d+\\.|[A-Z][a-zA-Z\\s.-]+,\\s*\\(?(?:19|20)\\d{2}\\)?)"
    );

    private static final Pattern TABLE_CAPTION = Pattern.compile(
            "(?i)^\\s*(?:table|tbl\\.)\\s+\\d+[:.]"
    );

    private static final Pattern FIGURE_CAPTION = Pattern.compile(
            "(?i)^\\s*(?:figure|fig\\.)\\s+\\d+[:.]"
    );

    private static final List<Pattern> JUNK_LINE_PATTERNS = List.of(
            Pattern.compile("(?i)^.*available\\s+online\\s+at\\s+www\\.sciencedirect\\.com.*$"),
            Pattern.compile("(?i)^.*available\\s+online\\s+at.*$"),
            Pattern.compile("(?i)^.*journal\\s+homepage:?.*$"),
            Pattern.compile("(?i)^.*contents\\s+lists\\s+available\\s+at.*$"),
            Pattern.compile("(?i)^.*(?:elsevier|springer|wiley|ieee).*all\\s+rights\\s+reserved.*$"),
            Pattern.compile("(?i)^.*copyright\\s*©?.*$"),
            Pattern.compile("(?i)^.*©\\s*\\d{4}.*$"),
            Pattern.compile("(?i)^.*https?:\\/\\/doi\\.org\\/10\\.\\d{4,9}\\/[-._;()/:A-Za-z0-9]+.*$"),
            Pattern.compile("(?i)^.*doi:\\s*10\\.\\d{4,9}\\/[-._;()/:A-Za-z0-9]+.*$"),
            Pattern.compile("(?i)^.*issn[\\s:]+\\d{4}-\\d{3}[\\dxX].*$"),
            Pattern.compile("(?i)^.*\\d{4}-\\d{3}[\\dxX]\\/\\s*©.*$"),
            Pattern.compile("(?i)^.*received\\s+\\d{1,2}\\s+[a-z]+.*accepted\\s+\\d{1,2}\\s+[a-z]+.*$"),
            Pattern.compile("(?i)^.*article\\s+history:?.*$")
    );

    /**
     * Classifies a chunk into a semantic category.
     */
    public ChunkSemanticType classify(String text, Integer pageNumber, Integer chunkNumber) {
        if (text == null || text.isBlank()) {
            return ChunkSemanticType.BODY;
        }

        String trimmed = text.trim();
        String[] lines = trimmed.split("\n");

        // 1. References section check
        if (REFERENCES_HEADING.matcher(lines[0].trim()).matches()) {
            return ChunkSemanticType.REFERENCES;
        }
        int referenceLineCount = 0;
        for (String line : lines) {
            if (REFERENCE_ENTRY_LINE.matcher(line.trim()).find()) {
                referenceLineCount++;
            }
        }
        if (lines.length >= 3 && (double) referenceLineCount / lines.length >= 0.5) {
            return ChunkSemanticType.REFERENCES;
        }

        // 2. Table check
        if (TABLE_CAPTION.matcher(lines[0].trim()).find()) {
            return ChunkSemanticType.TABLE;
        }
        int tablePipeLines = 0;
        for (String line : lines) {
            if (line.contains("|") && line.indexOf('|') != line.lastIndexOf('|')) {
                tablePipeLines++;
            }
        }
        if (tablePipeLines >= 2) {
            return ChunkSemanticType.TABLE;
        }

        // 3. Figure caption check
        if (lines.length <= 4 && FIGURE_CAPTION.matcher(lines[0].trim()).find()) {
            return ChunkSemanticType.FIGURE_CAPTION;
        }

        // 4. Footer / Running header check
        if (trimmed.length() <= 300 && FOOTER_HEADER_SNIPPET.matcher(trimmed).find()) {
            return ChunkSemanticType.FOOTER_HEADER;
        }

        // 5. Metadata / Front matter check
        if (trimmed.length() <= 400 && METADATA_SNIPPET.matcher(trimmed).find()) {
            return ChunkSemanticType.METADATA;
        }

        if (pageNumber != null && pageNumber == 1) {
            if (METADATA_SNIPPET.matcher(trimmed).find() || (chunkNumber != null && chunkNumber <= 2 && trimmed.contains("@") && trimmed.contains("University"))) {
                return ChunkSemanticType.FRONT_MATTER;
            }
        }

        return ChunkSemanticType.BODY;
    }

    /**
     * Determines whether the chunk is suitable for substantive evidence generation.
     * Excludes footers, headers, raw metadata, and bibliography lists.
     */
    public boolean isSubstantiveEvidence(ChunkSemanticType type) {
        if (type == null) {
            return true;
        }
        return type != ChunkSemanticType.FOOTER_HEADER
                && type != ChunkSemanticType.METADATA
                && type != ChunkSemanticType.REFERENCES;
    }

    /**
     * Sanitizes chunk text by stripping junk publication header lines, DOIs, ISSNs,
     * copyright notices, and "Available online..." fragments.
     */
    public String sanitizeEvidenceText(String text) {
        if (text == null || text.isBlank()) {
            return "";
        }

        String[] rawLines = text.split("\r?\n");
        List<String> cleanLines = new ArrayList<>();

        for (String line : rawLines) {
            String trimmedLine = line.trim();
            if (trimmedLine.isEmpty()) {
                cleanLines.add("");
                continue;
            }

            boolean isJunk = false;
            for (Pattern pattern : JUNK_LINE_PATTERNS) {
                if (pattern.matcher(trimmedLine).matches()) {
                    isJunk = true;
                    break;
                }
            }

            if (!isJunk) {
                cleanLines.add(line);
            }
        }

        String cleaned = String.join("\n", cleanLines).trim();
        // Remove 3+ consecutive blank lines
        cleaned = cleaned.replaceAll("\n{3,}", "\n\n");
        return cleaned.isBlank() ? text.trim() : cleaned;
    }
}
