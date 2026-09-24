package com.researchassistant.reference.dto;

import com.researchassistant.analysis.entity.CitationStyle;
import com.researchassistant.common.enums.ContentOrigin;
import com.researchassistant.reference.entity.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public final class ReferenceDtos {
    private ReferenceDtos() {}
    public enum CitationContext { IN_TEXT_NARRATIVE, IN_TEXT_PARENTHETICAL, NUMERIC, REFERENCE_LIST }
    public record AuthorRequest(String familyName, String givenName, String literalName, String orcid, AuthorRole role) {}
    public record CreateReferenceRequest(@NotNull ReferenceType type, @NotBlank String title, String containerTitle,
            Integer publicationYear, String volume, String issue, String pages, String publisher, String publisherPlace,
            String edition, String institution, String conferenceName, String abstractText, String languageCode,
            String doi, String url, String isbn, String issn, String pmid, String arxivId,
            ReferenceMetadataStatus metadataStatus, ContentOrigin origin, List<AuthorRequest> authors) {}
    public record UpdateReferenceRequest(String title, String containerTitle, Integer publicationYear, String volume,
            String issue, String pages, String publisher, String publisherPlace, String doi, String url,
            ReferenceMetadataStatus metadataStatus, Boolean availableForCitation, Boolean availableForResearchAi,
            List<AuthorRequest> authors, String citationKey) {}
    public record UpdateReferenceUsageScopeRequest(Boolean availableForResearchAi, Boolean availableForCitation) {}
    public record ReferenceResponse(UUID id, UUID projectReferenceId, String citationKey, ReferenceType type, String title,
            String containerTitle, Integer year, String volume, String issue, String pages, String publisher,
            String doi, String url, ReferenceMetadataStatus metadataStatus, String metadataSource,
            Double metadataConfidence, boolean availableForCitation, boolean availableForResearchAi,
            List<AuthorResponse> authors, OffsetDateTime updatedAt) {}
    public record AuthorResponse(String familyName, String givenName, String literalName, AuthorRole role, int displayOrder) {}
    public record DuplicateCheckRequest(String doi, String title, Integer publicationYear, String firstAuthor) {}
    public record DuplicateCandidate(UUID referenceId, String reason, String title, Integer year) {}
    public record DuplicateCheckResponse(List<DuplicateCandidate> exactDuplicates, List<DuplicateCandidate> possibleDuplicates) {}
    public record FormattedCitation(String text, List<String> warnings, boolean metadataComplete) {}
    public record FormatCitationRequest(@NotNull UUID referenceId, @NotNull CitationStyle style, @NotNull CitationContext context, Integer citationNumber) {}
    public record ImportRequest(@NotNull ReferenceImportFormat format, @NotBlank String originalFilename, @NotBlank String content) {}
    public record ImportJobResponse(UUID id, ReferenceImportFormat format, ReferenceImportStatus status, int detectedEntries, int importedEntries, int duplicateEntries) {}
    public record ImportPreviewItem(UUID id, int ordinal, String title, Integer year, String doi, ReferenceImportClassification classification, String warnings) {}
    public record ConfirmImportRequest(boolean importAsNew, boolean skipExactDuplicates) {}
}
