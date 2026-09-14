package com.researchassistant.rag.citation;

import com.researchassistant.rag.entity.RagQueryEvidence;
import com.researchassistant.rag.generation.GeneratedAnswerDraft;
import com.researchassistant.rag.generation.GeneratedCitation;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashSet;
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
        Set<Integer> seen = new HashSet<>();
        for (GeneratedCitation citation : draft.citations()) {
            if (!byOrdinal.containsKey(citation.evidenceOrdinal())) {
                errors.add("Citation references evidence that was not supplied.");
            }
            if (!seen.add(citation.evidenceOrdinal())) {
                errors.add("Duplicate evidence citation reference.");
            }
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
        return new CitationVerificationResult(errors.isEmpty(), errors);
    }
}
