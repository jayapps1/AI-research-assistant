package com.researchassistant.reference.service;

import com.researchassistant.analysis.entity.CitationStyle;
import com.researchassistant.cache.AppCacheNames;
import com.researchassistant.reference.dto.ReferenceDtos.CitationContext;
import com.researchassistant.reference.dto.ReferenceDtos.FormattedCitation;
import com.researchassistant.reference.entity.*;
import com.researchassistant.reference.repository.ReferenceAuthorRepository;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;

@Service
public class CitationFormattingService {
    private final ReferenceAuthorRepository authorRepository;

    public CitationFormattingService(ReferenceAuthorRepository authorRepository) {
        this.authorRepository = authorRepository;
    }

    @Cacheable(cacheNames = AppCacheNames.FORMATTED_CITATIONS,
            key = "#reference.id + ':' + #reference.updatedAt + ':' + #style + ':' + #context + ':' + (#citationNumber == null ? 'none' : #citationNumber)")
    public FormattedCitation format(ReferenceEntry reference, CitationStyle style, CitationContext context, Integer citationNumber) {
        List<ReferenceAuthor> authors = authorRepository.findAllByReferenceIdAndRoleOrderByDisplayOrderAsc(reference.getId(), AuthorRole.AUTHOR);
        List<String> warnings = warnings(reference, authors);
        String text;
        boolean incomplete = !warnings.isEmpty()
                || reference.getMetadataStatus() == ReferenceMetadataStatus.INCOMPLETE
                || looksLikeFilenameTitle(reference.getTitle());
        if (style == CitationStyle.NUMERIC_APA) {
            if (context == CitationContext.REFERENCE_LIST) {
                if (incomplete) {
                    text = "[" + (citationNumber == null ? "?" : citationNumber) + "] REFERENCE_METADATA_INCOMPLETE.";
                } else {
                    text = "[" + (citationNumber == null ? "?" : citationNumber) + "] " + authorYearReference(reference, authors, style);
                }
            } else {
                text = "[" + (citationNumber == null ? "?" : citationNumber) + "]";
            }
        } else if (context == CitationContext.NUMERIC || style == CitationStyle.IEEE || style == CitationStyle.VANCOUVER) {
            text = context == CitationContext.REFERENCE_LIST ? numericReference(reference, authors, citationNumber, style) : "[" + (citationNumber == null ? "?" : citationNumber) + "]";
        } else if (context == CitationContext.REFERENCE_LIST) {
            text = authorYearReference(reference, authors, style);
        } else if (incomplete) {
            text = "REFERENCE_METADATA_INCOMPLETE";
        } else {
            String author = shortAuthor(authors, style, context);
            String year = reference.getPublicationYear() == null ? "n.d." : reference.getPublicationYear().toString();
            if (style == CitationStyle.MLA_9) {
                text = context == CitationContext.IN_TEXT_NARRATIVE ? author : "(" + author + ")";
            } else {
                text = context == CitationContext.IN_TEXT_NARRATIVE ? author + " (" + year + ")" : "(" + author + ", " + year + ")";
            }
        }
        return new FormattedCitation(text, warnings, warnings.isEmpty());
    }

    private String authorYearReference(ReferenceEntry ref, List<ReferenceAuthor> authors, CitationStyle style) {
        String a = referenceListAuthors(authors, style);
        String y = ref.getPublicationYear() == null ? "n.d." : ref.getPublicationYear().toString();
        StringBuilder b = new StringBuilder();
        b.append(a).append(" (").append(y).append("). ").append(ref.getTitle()).append(".");
        if (ref.getContainerTitle() != null) b.append(" ").append(ref.getContainerTitle()).append(".");
        if (ref.getVolume() != null) b.append(" ").append(ref.getVolume());
        if (ref.getIssue() != null) b.append("(").append(ref.getIssue()).append(")");
        if (ref.getPages() != null) b.append(", ").append(ref.getPages());
        if (ref.getPublisher() != null && ref.getType() == ReferenceType.BOOK) b.append(" ").append(ref.getPublisher()).append(".");
        if (ref.getDoi() != null) b.append(" https://doi.org/").append(ref.getNormalizedDoi() == null ? ref.getDoi() : ref.getNormalizedDoi());
        else if (ref.getUrl() != null) b.append(" ").append(ref.getUrl());
        return b.toString();
    }

    private String numericReference(ReferenceEntry ref, List<ReferenceAuthor> authors, Integer number, CitationStyle style) {
        if (ref.getMetadataStatus() == ReferenceMetadataStatus.INCOMPLETE || looksLikeFilenameTitle(ref.getTitle())) {
            return "[" + (number == null ? "?" : number) + "] REFERENCE_METADATA_INCOMPLETE.";
        }
        StringJoiner joiner = new StringJoiner(", ");
        for (ReferenceAuthor author : authors) joiner.add(displayName(author));
        String prefix = "[" + (number == null ? "?" : number) + "] ";
        return prefix + (authors.isEmpty() ? ref.getTitle() : joiner + ", \"" + ref.getTitle() + "\"")
                + (ref.getContainerTitle() == null ? "" : ", " + ref.getContainerTitle())
                + (ref.getPublicationYear() == null ? "" : ", " + ref.getPublicationYear()) + ".";
    }

    private String shortAuthor(List<ReferenceAuthor> authors, CitationStyle style, CitationContext context) {
        if (authors.isEmpty()) return "Unknown author";
        if (authors.size() == 1) return displayShort(authors.getFirst());
        if (authors.size() == 2) {
            String separator = context == CitationContext.IN_TEXT_NARRATIVE
                    || style == CitationStyle.HARVARD
                    || style == CitationStyle.CHICAGO_AUTHOR_DATE
                    ? " and "
                    : " & ";
            return displayShort(authors.get(0)) + separator + displayShort(authors.get(1));
        }
        return displayShort(authors.getFirst()) + " et al.";
    }

    private String referenceListAuthors(List<ReferenceAuthor> authors, CitationStyle style) {
        if (authors.isEmpty()) return "Unknown author";
        StringJoiner joiner = new StringJoiner(", ");
        for (ReferenceAuthor author : authors) joiner.add(displayName(author));
        return joiner.toString();
    }

    private String displayShort(ReferenceAuthor author) {
        if (author.getLiteralName() != null) return author.getLiteralName();
        return author.getFamilyName() == null ? "Unknown author" : author.getFamilyName();
    }

    private String displayName(ReferenceAuthor author) {
        if (author.getLiteralName() != null) return author.getLiteralName();
        if (author.getFamilyName() == null) return author.getGivenName() == null ? "Unknown author" : author.getGivenName();
        return author.getGivenName() == null ? author.getFamilyName() : author.getFamilyName() + ", " + author.getGivenName();
    }

    private List<String> warnings(ReferenceEntry ref, List<ReferenceAuthor> authors) {
        List<String> warnings = new ArrayList<>();
        if (authors.isEmpty()) warnings.add("Missing author.");
        if (ref.getPublicationYear() == null) warnings.add("Missing publication year.");
        if (ref.getTitle() == null || ref.getTitle().isBlank()) warnings.add("Missing title.");
        if (looksLikeFilenameTitle(ref.getTitle())) warnings.add("Reference title appears to be a filename or internal placeholder.");
        if (ref.getType() == ReferenceType.JOURNAL_ARTICLE && ref.getContainerTitle() == null) warnings.add("Missing journal/container title.");
        return warnings;
    }

    private boolean looksLikeFilenameTitle(String title) {
        if (title == null || title.isBlank()) return false;
        String value = title.trim();
        if ("REFERENCE_METADATA_INCOMPLETE".equalsIgnoreCase(value)) return true;
        if (value.matches("(?i).+\\.(pdf|docx?|txt)$")) return true;
        if (value.contains(" ")) return false;
        return value.matches("[A-Za-z0-9._-]{8,}") && (value.contains("-") || value.contains("_") || value.matches(".*\\d{3,}.*"));
    }
}
