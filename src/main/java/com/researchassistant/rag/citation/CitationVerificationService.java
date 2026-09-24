package com.researchassistant.rag.citation;

import com.researchassistant.rag.entity.RagQueryEvidence;
import com.researchassistant.rag.generation.GeneratedAnswerDraft;
import com.researchassistant.rag.generation.GeneratedCitation;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class CitationVerificationService {

    private static final Pattern DOC_REFERENCE =
            Pattern.compile("\\bDOC-\\d{3,}\\b");
    private final CitationMarkerParser markerParser;

    public CitationVerificationService() {
        this(new CitationMarkerParser());
    }

    public CitationVerificationService(CitationMarkerParser markerParser) {
        this.markerParser = markerParser;
    }

    public CitationVerificationResult verify(
            GeneratedAnswerDraft draft,
            List<RagQueryEvidence> evidence
    ) {
        List<String> errors = new ArrayList<>();
        Map<Integer, RagQueryEvidence> byOrdinal = evidence.stream()
                .collect(Collectors.toMap(
                        RagQueryEvidence::getEvidenceOrdinal,
                        Function.identity()
                ));
        CitationMarkerParser.ParsedCitationMarkers parsedMarkers =
                markerParser.parse(draft.answerText());
        Set<Integer> citedOrdinals = new LinkedHashSet<>(parsedMarkers.evidenceOrdinals());
        for (GeneratedCitation citation : draft.citations()) {
            citedOrdinals.add(citation.evidenceOrdinal());
        }

        List<Integer> missingOrdinals = citedOrdinals.stream()
                .filter(ordinal -> !byOrdinal.containsKey(ordinal))
                .toList();
        List<String> invalidMarkers = missingOrdinals.stream()
                .map(ordinal -> "[E" + ordinal + "]")
                .distinct()
                .toList();
        if (!missingOrdinals.isEmpty()) {
            errors.add("Citation references evidence that was not supplied.");
        }
        for (GeneratedCitation citation : draft.citations()) {
            if (citation.evidenceOrdinal() <= 0) {
                errors.add("Citation references an invalid evidence identifier.");
            }
        }
        if (citedOrdinals.isEmpty()) {
            errors.add("Generated answer did not cite any supplied evidence.");
        }
        Matcher matcher = DOC_REFERENCE.matcher(draft.answerText() == null ? "" : draft.answerText());
        while (matcher.find()) {
            String code = matcher.group();
            boolean present = evidence.stream()
                    .anyMatch(item -> item.getDocumentCode().equals(code));
            if (!present) {
                errors.add("Answer contains a fabricated document reference.");
                break;
            }
        }
        if (draft.answerText() == null || draft.answerText().isBlank()) {
            errors.add("Generated answer is empty.");
        }
        List<Integer> verifiedOrdinals = citedOrdinals.stream()
                .filter(byOrdinal::containsKey)
                .toList();
        return new CitationVerificationResult(
                errors.isEmpty(),
                errors,
                verifiedOrdinals,
                parsedMarkers.markers(),
                invalidMarkers,
                missingOrdinals
        );
    }
}
