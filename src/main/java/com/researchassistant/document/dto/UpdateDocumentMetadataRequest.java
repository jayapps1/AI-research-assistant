package com.researchassistant.document.dto;

import jakarta.validation.constraints.Size;

public record UpdateDocumentMetadataRequest(
        @Size(max = 500) String title,
        @Size(max = 1000) String bibliographicTitle,
        String authors,
        Integer publicationYear,
        @Size(max = 500) String journal,
        @Size(max = 500) String conference,
        @Size(max = 500) String publisher,
        @Size(max = 100) String volume,
        @Size(max = 100) String issue,
        @Size(max = 100) String pages,
        @Size(max = 500) String doi,
        @Size(max = 1000) String url,
        @Size(max = 80) String sourceType,
        String keywords
) {
}
