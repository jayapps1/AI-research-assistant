package com.researchassistant.rag.citation;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class CitationMarkerParser {

    private static final Pattern EVIDENCE_ID =
            Pattern.compile("E(\\d+)", Pattern.CASE_INSENSITIVE);
    private static final Pattern ANY_BRACKETED_EVIDENCE_MARKER =
            Pattern.compile("\\[[^\\]]*\\bE\\d+[^\\]]*]", Pattern.CASE_INSENSITIVE);

    public ParsedCitationMarkers parse(String text) {
        if (text == null || text.isBlank()) {
            return new ParsedCitationMarkers(List.of(), List.of());
        }
        Matcher bracketMatcher = ANY_BRACKETED_EVIDENCE_MARKER.matcher(text);
        Set<Integer> ordinals = new LinkedHashSet<>();
        List<String> markers = new ArrayList<>();
        while (bracketMatcher.find()) {
            String marker = bracketMatcher.group();
            markers.add(marker);
            Matcher idMatcher = EVIDENCE_ID.matcher(marker);
            while (idMatcher.find()) {
                try {
                    ordinals.add(Integer.parseInt(idMatcher.group(1)));
                } catch (NumberFormatException ignored) {
                    // Regex limits the value to digits. Overflow is treated as unparseable.
                }
            }
        }
        return new ParsedCitationMarkers(List.copyOf(ordinals), List.copyOf(markers));
    }

    public record ParsedCitationMarkers(List<Integer> evidenceOrdinals, List<String> markers) {
        public ParsedCitationMarkers {
            evidenceOrdinals = evidenceOrdinals == null ? List.of() : List.copyOf(evidenceOrdinals);
            markers = markers == null ? List.of() : List.copyOf(markers);
        }
    }
}
