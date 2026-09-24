package com.researchassistant.document.metadata;

import com.researchassistant.reference.entity.ReferenceMetadataStatus;

import java.util.ArrayList;
import java.util.List;

public record BibliographicMetadata(
        String title,
        String authors,
        Integer publicationYear,
        String journal,
        String conference,
        String publisher,
        String volume,
        String issue,
        String pages,
        String doi,
        String isbn,
        String issn,
        String url,
        String sourceType,
        String keywords,
        String source,
        double confidence
) {
    public BibliographicMetadata merge(BibliographicMetadata higherPriority) {
        if (higherPriority == null) {
            return this;
        }
        return new BibliographicMetadata(
                first(higherPriority.title, title),
                first(higherPriority.authors, authors),
                higherPriority.publicationYear != null ? higherPriority.publicationYear : publicationYear,
                first(higherPriority.journal, journal),
                first(higherPriority.conference, conference),
                first(higherPriority.publisher, publisher),
                first(higherPriority.volume, volume),
                first(higherPriority.issue, issue),
                first(higherPriority.pages, pages),
                first(higherPriority.doi, doi),
                first(higherPriority.isbn, isbn),
                first(higherPriority.issn, issn),
                first(higherPriority.url, url),
                first(higherPriority.sourceType, sourceType),
                first(higherPriority.keywords, keywords),
                combineSources(source, higherPriority.source),
                Math.max(confidence, higherPriority.confidence)
        );
    }

    public ReferenceMetadataStatus status() {
        boolean hasTitle = present(title) && !"REFERENCE_METADATA_INCOMPLETE".equalsIgnoreCase(title.trim());
        boolean hasAuthors = present(authors);
        boolean hasYear = publicationYear != null;
        boolean hasVenueOrIdentifier = present(journal) || present(conference) || present(publisher) || present(doi) || present(isbn);
        if (hasTitle && hasAuthors && hasYear && hasVenueOrIdentifier) {
            return ReferenceMetadataStatus.COMPLETE;
        }
        if (hasTitle || hasAuthors || hasYear || hasVenueOrIdentifier) {
            return ReferenceMetadataStatus.PARTIAL;
        }
        return ReferenceMetadataStatus.INCOMPLETE;
    }

    static BibliographicMetadata empty() {
        return new BibliographicMetadata(null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, 0.0d);
    }

    private static String first(String first, String second) {
        return present(first) ? first.trim() : present(second) ? second.trim() : null;
    }

    private static String combineSources(String first, String second) {
        List<String> values = new ArrayList<>();
        if (present(first)) values.add(first.trim());
        if (present(second) && values.stream().noneMatch(v -> v.equalsIgnoreCase(second.trim()))) values.add(second.trim());
        return values.isEmpty() ? null : String.join("+", values);
    }

    private static boolean present(String value) {
        return value != null && !value.isBlank();
    }
}
