package com.researchassistant.conversation.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.researchassistant.common.enums.ContentOrigin;
import com.researchassistant.conversation.dto.SavedConversationSourceResponse;
import com.researchassistant.conversation.entity.ConversationSource;
import com.researchassistant.conversation.entity.ConversationSourceType;
import com.researchassistant.conversation.repository.ConversationSourceRepository;
import com.researchassistant.identity.entity.User;
import com.researchassistant.project.entity.ResearchProject;
import com.researchassistant.project.service.ProjectAuthorizationService;
import com.researchassistant.reference.entity.AuthorRole;
import com.researchassistant.reference.entity.ProjectReference;
import com.researchassistant.reference.entity.ProjectReferenceStatus;
import com.researchassistant.reference.entity.ReferenceAuthor;
import com.researchassistant.reference.entity.ReferenceEntry;
import com.researchassistant.reference.entity.ReferenceMetadataStatus;
import com.researchassistant.reference.entity.ReferenceType;
import com.researchassistant.reference.repository.ProjectReferenceRepository;
import com.researchassistant.reference.repository.ReferenceAuthorRepository;
import com.researchassistant.reference.repository.ReferenceEntryRepository;
import com.researchassistant.reference.service.ReferenceNormalizationService;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Service
public class WebSourceProjectSaveService {

    private final ConversationSourceRepository sourceRepository;
    private final ReferenceEntryRepository entryRepository;
    private final ReferenceAuthorRepository authorRepository;
    private final ProjectReferenceRepository projectReferenceRepository;
    private final ProjectAuthorizationService authorizationService;
    private final ReferenceNormalizationService normalizationService;
    private final ObjectMapper objectMapper;

    public WebSourceProjectSaveService(
            ConversationSourceRepository sourceRepository,
            ReferenceEntryRepository entryRepository,
            ReferenceAuthorRepository authorRepository,
            ProjectReferenceRepository projectReferenceRepository,
            ProjectAuthorizationService authorizationService,
            ReferenceNormalizationService normalizationService,
            ObjectMapper objectMapper
    ) {
        this.sourceRepository = sourceRepository;
        this.entryRepository = entryRepository;
        this.authorRepository = authorRepository;
        this.projectReferenceRepository = projectReferenceRepository;
        this.authorizationService = authorizationService;
        this.normalizationService = normalizationService;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public SavedConversationSourceResponse saveToProject(UUID sourceId, UUID projectId, User user) {
        ConversationSource source = sourceRepository.findByIdAndConversationUserId(sourceId, user.getId())
                .orElseThrow(() -> new com.researchassistant.conversation.exception.ConversationAccessDeniedException("Conversation source not found."));
        if (source.getSourceType() != ConversationSourceType.WEB_PAGE
                && source.getSourceType() != ConversationSourceType.ACADEMIC_SOURCE) {
            throw new IllegalArgumentException("Only web sources can be saved to a project.");
        }
        ResearchProject project = authorizationService.requireProjectEditor(projectId, user).project();

        SourceMetadata metadata = metadata(source);
        String normalizedDoi = normalizationService.normalizeDoi(metadata.doi());
        ReferenceEntry existing = findExistingReference(source, normalizedDoi, metadata).orElse(null);
        boolean reusedReference = existing != null;
        ReferenceEntry reference = reusedReference
                ? updateIncompleteReference(existing, source, normalizedDoi, metadata)
                : createReference(source, normalizedDoi, metadata, user);

        ProjectReference existingProjectReference = projectReferenceRepository
                .findByProjectIdAndReferenceId(project.getId(), reference.getId())
                .orElse(null);
        boolean reusedProjectReference = existingProjectReference != null;
        ProjectReference projectReference = reusedProjectReference
                ? existingProjectReference
                : createProjectReference(project, reference, authors(source), user);
        if (projectReference.getStatus() != ProjectReferenceStatus.ACTIVE) {
            projectReference.setStatus(ProjectReferenceStatus.ACTIVE);
        }

        source.setProjectReference(projectReference);
        sourceRepository.save(source);

        return new SavedConversationSourceResponse(
                source.getId(),
                reference.getId(),
                projectReference.getId(),
                projectReference.getCitationKey(),
                reusedReference,
                reusedProjectReference
        );
    }

    private java.util.Optional<ReferenceEntry> findExistingReference(
            ConversationSource source,
            String normalizedDoi,
            SourceMetadata metadata
    ) {
        if (normalizedDoi != null) {
            var byDoi = entryRepository.findFirstByNormalizedDoiIgnoreCase(normalizedDoi);
            if (byDoi.isPresent()) {
                return byDoi;
            }
        }
        if (source.getUrl() != null && !source.getUrl().isBlank()) {
            var byUrl = entryRepository.findFirstByUrlIgnoreCase(source.getUrl().trim());
            if (byUrl.isPresent()) {
                return byUrl;
            }
        }
        String normalizedTitle = normalizationService.normalizeTitle(source.getTitle());
        if (normalizedTitle == null) {
            return java.util.Optional.empty();
        }
        return entryRepository.findAllByTitleContainingIgnoreCase(source.getTitle()).stream()
                .filter(entry -> normalizedTitle.equals(normalizationService.normalizeTitle(entry.getTitle())))
                .filter(entry -> metadata.publicationYear() == null || metadata.publicationYear().equals(entry.getPublicationYear()))
                .findFirst();
    }

    private ReferenceEntry createReference(
            ConversationSource source,
            String normalizedDoi,
            SourceMetadata metadata,
            User user
    ) {
        ReferenceEntry entry = new ReferenceEntry();
        entry.setType(source.getSourceType() == ConversationSourceType.ACADEMIC_SOURCE
                ? ReferenceType.JOURNAL_ARTICLE
                : ReferenceType.WEB_PAGE);
        entry.setTitle(source.getTitle());
        entry.setPublicationYear(metadata.publicationYear());
        entry.setPublicationMonth(metadata.publicationMonth());
        entry.setPublicationDay(metadata.publicationDay());
        entry.setPublisher(metadata.publisher());
        entry.setDoi(metadata.doi());
        entry.setNormalizedDoi(normalizedDoi);
        entry.setUrl(blankToNull(source.getUrl()));
        entry.setMetadataStatus(metadataStatus(source, metadata));
        entry.setMetadataSource(source.getProvider());
        entry.setMetadataReviewStatus(metadataStatus(source, metadata).name());
        entry.setOrigin(ContentOrigin.IMPORTED);
        entry.setAccessedAt(OffsetDateTime.now());
        entry.setRetrievedAt(source.getRetrievedAt());
        entry.setCreatedBy(user);
        entry = entryRepository.save(entry);
        saveAuthors(entry, authors(source));
        return entry;
    }

    private ReferenceEntry updateIncompleteReference(
            ReferenceEntry entry,
            ConversationSource source,
            String normalizedDoi,
            SourceMetadata metadata
    ) {
        if (entry.getMetadataStatus() != ReferenceMetadataStatus.VERIFIED) {
            if (isBlank(entry.getTitle())) {
                entry.setTitle(source.getTitle());
            }
            if (entry.getPublicationYear() == null) {
                entry.setPublicationYear(metadata.publicationYear());
            }
            if (isBlank(entry.getPublisher())) {
                entry.setPublisher(metadata.publisher());
            }
            if (isBlank(entry.getDoi())) {
                entry.setDoi(metadata.doi());
                entry.setNormalizedDoi(normalizedDoi);
            }
            if (isBlank(entry.getUrl())) {
                entry.setUrl(blankToNull(source.getUrl()));
            }
            if (entry.getRetrievedAt() == null) {
                entry.setRetrievedAt(source.getRetrievedAt());
            }
            if (entry.getAccessedAt() == null) {
                entry.setAccessedAt(OffsetDateTime.now());
            }
        }
        return entryRepository.save(entry);
    }

    private ProjectReference createProjectReference(
            ResearchProject project,
            ReferenceEntry reference,
            List<String> authors,
            User user
    ) {
        ProjectReference projectReference = new ProjectReference();
        projectReference.setProject(project);
        projectReference.setReference(reference);
        projectReference.setCitationKey(uniqueCitationKey(project.getId(), reference, authors));
        projectReference.setAddedBy(user);
        return projectReferenceRepository.save(projectReference);
    }

    private void saveAuthors(ReferenceEntry entry, List<String> authors) {
        int displayOrder = 1;
        for (String authorName : authors) {
            if (authorName == null || authorName.isBlank()) {
                continue;
            }
            ReferenceAuthor author = new ReferenceAuthor();
            author.setReference(entry);
            author.setLiteralName(authorName.trim());
            author.setDisplayOrder(displayOrder++);
            author.setRole(AuthorRole.AUTHOR);
            authorRepository.save(author);
        }
    }

    private ReferenceMetadataStatus metadataStatus(ConversationSource source, SourceMetadata metadata) {
        boolean hasCore = !authors(source).isEmpty()
                && metadata.publicationYear() != null
                && source.getTitle() != null
                && !source.getTitle().isBlank();
        if (hasCore && (metadata.doi() != null || metadata.publisher() != null || source.getUrl() != null)) {
            return ReferenceMetadataStatus.COMPLETE;
        }
        return source.getTitle() == null || source.getTitle().isBlank()
                ? ReferenceMetadataStatus.INCOMPLETE
                : ReferenceMetadataStatus.PARTIAL;
    }

    private SourceMetadata metadata(ConversationSource source) {
        Map<String, Object> values = readMetadata(source.getMetadata());
        String doi = stringValue(values.get("doi"));
        String publisher = firstNonBlank(
                stringValue(values.get("publisher")),
                stringValue(values.get("siteName")),
                source.getProvider()
        );
        OffsetDateTime publishedAt = source.getPublishedAt();
        return new SourceMetadata(
                doi,
                publisher,
                publishedAt == null ? null : publishedAt.getYear(),
                publishedAt == null ? null : publishedAt.getMonthValue(),
                publishedAt == null ? null : publishedAt.getDayOfMonth()
        );
    }

    private Map<String, Object> readMetadata(String json) {
        if (json == null || json.isBlank()) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {});
        } catch (Exception ignored) {
            return Map.of();
        }
    }

    private List<String> authors(ConversationSource source) {
        if (source.getAuthorsJson() == null || source.getAuthorsJson().isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(source.getAuthorsJson(), new TypeReference<List<String>>() {});
        } catch (Exception ignored) {
            return List.of();
        }
    }

    private String uniqueCitationKey(UUID projectId, ReferenceEntry reference, List<String> authors) {
        String stem = "ref";
        if (authors != null && !authors.isEmpty() && authors.getFirst() != null && !authors.getFirst().isBlank()) {
            String first = authors.getFirst();
            String[] parts = first.contains(",")
                    ? first.split(",", 2)
                    : first.trim().split("\\s+");
            stem = first.contains(",") ? parts[0] : parts[parts.length - 1];
        }
        stem = stem.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "");
        if (stem.isBlank()) {
            stem = "ref";
        }
        String base = stem + (reference.getPublicationYear() == null ? "nd" : reference.getPublicationYear());
        String key = base;
        int suffix = 2;
        while (projectReferenceRepository.existsByProjectIdAndCitationKey(projectId, key)) {
            key = base + suffix++;
        }
        return key;
    }

    private String stringValue(Object value) {
        if (value == null) {
            return null;
        }
        String text = String.valueOf(value).trim();
        return text.isBlank() ? null : text;
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return null;
    }

    private String blankToNull(String value) {
        return isBlank(value) ? null : value.trim();
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private record SourceMetadata(
            String doi,
            String publisher,
            Integer publicationYear,
            Integer publicationMonth,
            Integer publicationDay
    ) {
    }
}
