package com.researchassistant.rag.citation;

import com.researchassistant.analysis.entity.CitationPresentation;
import com.researchassistant.analysis.entity.CitationStyle;
import com.researchassistant.reference.dto.ReferenceDtos.CitationContext;
import com.researchassistant.reference.dto.ReferenceDtos.FormattedCitation;
import com.researchassistant.reference.entity.ReferenceEntry;
import com.researchassistant.reference.repository.ProjectReferenceRepository;
import com.researchassistant.reference.repository.ReferenceSourceLinkRepository;
import com.researchassistant.reference.service.CitationFormattingService;
import com.researchassistant.rag.entity.AnswerCitation;
import com.researchassistant.rag.entity.RagQueryEvidence;

import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
public class UserFacingCitationRenderer {

    private static final Pattern RAW_EVIDENCE_MARKER = Pattern.compile("\\[?E\\d+]?\\b");
    private static final Pattern RAW_DOCUMENT_PROVENANCE = Pattern.compile("\\[?DOC-\\d{3,}(?:\\s*,\\s*p\\.?\\s*\\d+)?]?", Pattern.CASE_INSENSITIVE);

    private final ReferenceSourceLinkRepository sourceLinkRepository;
    private final ProjectReferenceRepository projectReferenceRepository;
    private final CitationFormattingService citationFormattingService;

    public UserFacingCitationRenderer(
            ReferenceSourceLinkRepository sourceLinkRepository,
            ProjectReferenceRepository projectReferenceRepository,
            CitationFormattingService citationFormattingService
    ) {
        this.sourceLinkRepository = sourceLinkRepository;
        this.projectReferenceRepository = projectReferenceRepository;
        this.citationFormattingService = citationFormattingService;
    }

    public String renderAnswer(
            String rawText,
            CitationStyle style,
            CitationPresentation presentation,
            List<AnswerCitation> citations
    ) {
        if (rawText == null || rawText.isBlank() || citations == null || citations.isEmpty()) {
            return stripInternalProvenance(rawText);
        }
        CitationStyle safeStyle = style == null ? CitationStyle.APA_7 : style;
        CitationPresentation safePresentation = presentation == null ? CitationPresentation.PARENTHETICAL : presentation;

        String rendered = rawText;
        Map<UUID, Integer> referenceNumbers = referenceNumbers(citations);
        for (AnswerCitation citation : citations) {
            RagQueryEvidence evidence = citation.getEvidence();
            ReferenceEntry reference = resolveReference(citation);
            Integer citationNumber = reference == null ? citation.getCitationOrdinal() : referenceNumbers.get(reference.getId());
            String replacement = formatCitation(reference, safeStyle, safePresentation, citationNumber);
            rendered = rendered.replaceAll("\\[E" + evidence.getEvidenceOrdinal() + "]", java.util.regex.Matcher.quoteReplacement(replacement));
            rendered = rendered.replaceAll("\\bE" + evidence.getEvidenceOrdinal() + "\\b", java.util.regex.Matcher.quoteReplacement(replacement));
        }
        return stripInternalProvenance(rendered);
    }

    public RenderedCitation renderCitation(AnswerCitation citation, CitationStyle style, CitationPresentation presentation) {
        ReferenceEntry reference = resolveReference(citation);
        FormattedCitation formatted = reference == null
                ? null
                : citationFormattingService.format(
                reference,
                style == null ? CitationStyle.APA_7 : style,
                context(style, presentation),
                citation.getCitationOrdinal()
        );
        if (formatted == null) {
            return new RenderedCitation("REFERENCE_METADATA_INCOMPLETE", false, "Reference metadata incomplete");
        }
        return new RenderedCitation(
                formatted.text(),
                formatted.metadataComplete(),
                formatted.metadataComplete() ? null : "Reference metadata incomplete"
        );
    }

    private String stripInternalProvenance(String text) {
        if (text == null) {
            return null;
        }
        String withoutEvidence = RAW_EVIDENCE_MARKER.matcher(text).replaceAll("");
        return RAW_DOCUMENT_PROVENANCE.matcher(withoutEvidence).replaceAll("");
    }

    private String formatCitation(
            ReferenceEntry reference,
            CitationStyle style,
            CitationPresentation presentation,
            Integer citationNumber
    ) {
        if (reference == null) {
            return "[" + (citationNumber != null ? citationNumber : 1) + "]";
        }
        FormattedCitation formatted = citationFormattingService.format(reference, style, context(style, presentation), citationNumber);
        if (formatted.metadataComplete() || style == CitationStyle.IEEE || style == CitationStyle.VANCOUVER || style == CitationStyle.NUMERIC_APA) {
            return formatted.text();
        }
        return "[" + (citationNumber != null ? citationNumber : 1) + "]";
    }

    private ReferenceEntry resolveReference(AnswerCitation citation) {
        RagQueryEvidence evidence = citation.getEvidence();
        UUID projectId = citation.getAnswer().getQuery().getConversation().getProject().getId();
        return sourceLinkRepository.findFirstByDocumentId(evidence.getDocumentId())
                .flatMap(link -> projectReferenceRepository
                        .findByProjectIdAndReferenceId(projectId, link.getReference().getId())
                        .map(projectReference -> projectReference.getReference())
                        .or(() -> java.util.Optional.of(link.getReference())))
                .orElse(null);
    }

    private Map<UUID, Integer> referenceNumbers(List<AnswerCitation> citations) {
        Map<UUID, Integer> numbers = new LinkedHashMap<>();
        Map<UUID, ReferenceEntry> cache = new HashMap<>();
        int next = 1;
        for (AnswerCitation citation : citations) {
            UUID documentId = citation.getEvidence().getDocumentId();
            ReferenceEntry reference = cache.computeIfAbsent(documentId, ignored -> resolveReference(citation));
            if (reference != null && !numbers.containsKey(reference.getId())) {
                numbers.put(reference.getId(), next++);
            }
        }
        return numbers;
    }

    private CitationContext context(CitationStyle style, CitationPresentation presentation) {
        if (style == CitationStyle.IEEE || style == CitationStyle.VANCOUVER || style == CitationStyle.NUMERIC_APA) {
            return CitationContext.NUMERIC;
        }
        if (presentation == CitationPresentation.NARRATIVE) {
            return CitationContext.IN_TEXT_NARRATIVE;
        }
        return CitationContext.IN_TEXT_PARENTHETICAL;
    }

    public record RenderedCitation(String text, boolean metadataComplete, String warning) {
    }
}
