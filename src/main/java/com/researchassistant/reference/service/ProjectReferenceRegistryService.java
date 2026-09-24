package com.researchassistant.reference.service;

import com.researchassistant.common.enums.ContentOrigin;
import com.researchassistant.document.entity.Document;
import com.researchassistant.document.entity.DocumentVersion;
import com.researchassistant.identity.entity.User;
import com.researchassistant.project.entity.ResearchProject;
import com.researchassistant.reference.entity.*;
import com.researchassistant.reference.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
public class ProjectReferenceRegistryService {

    private final ReferenceEntryRepository entryRepository;
    private final ReferenceAuthorRepository authorRepository;
    private final ProjectReferenceRepository projectReferenceRepository;
    private final ReferenceSourceLinkRepository sourceLinkRepository;
    private final ReferenceNormalizationService normalizationService;

    public ProjectReferenceRegistryService(
            ReferenceEntryRepository entryRepository,
            ReferenceAuthorRepository authorRepository,
            ProjectReferenceRepository projectReferenceRepository,
            ReferenceSourceLinkRepository sourceLinkRepository,
            ReferenceNormalizationService normalizationService
    ) {
        this.entryRepository = entryRepository;
        this.authorRepository = authorRepository;
        this.projectReferenceRepository = projectReferenceRepository;
        this.sourceLinkRepository = sourceLinkRepository;
        this.normalizationService = normalizationService;
    }

    @Transactional
    public ProjectReference ensureForDocument(Document document, DocumentVersion version, User user) {
        ResearchProject project = document.getProject();
        ProjectReference existingProjectReference = sourceLinkRepository.findFirstByDocumentId(document.getId())
                .flatMap(link -> projectReferenceRepository.findByProjectIdAndReferenceId(project.getId(), link.getReference().getId()))
                .orElse(null);
        if (existingProjectReference != null) {
            syncReferenceFromDocument(existingProjectReference.getReference(), document);
            String currentKey = existingProjectReference.getCitationKey();
            if (currentKey == null
                    || currentKey.startsWith("refnd")
                    || currentKey.matches("ref\\d*")
                    || !currentKey.matches("^[a-z]+(19|20)\\d{2}.*")) {
                existingProjectReference.setCitationKey(uniqueCitationKey(project.getId(), existingProjectReference.getReference(), document.getAuthors(), existingProjectReference.getId()));
                projectReferenceRepository.save(existingProjectReference);
            }
            return existingProjectReference;
        }

        String normalizedDoi = normalizationService.normalizeDoi(document.getDoi());
        ReferenceEntry reference = normalizedDoi == null
                ? null
                : entryRepository.findFirstByNormalizedDoiIgnoreCase(normalizedDoi).orElse(null);
        if (reference == null) {
            reference = buildReference(document, normalizedDoi, user);
            reference = entryRepository.save(reference);
            saveAuthors(reference, document.getAuthors());
        } else {
            syncReferenceFromDocument(reference, document);
        }

        ReferenceEntry managedReference = reference;
        ProjectReference projectReference = projectReferenceRepository
                .findByProjectIdAndReferenceId(project.getId(), managedReference.getId())
                .orElseGet(() -> {
                    ProjectReference pr = new ProjectReference();
                    pr.setProject(project);
                    pr.setReference(managedReference);
                    pr.setCitationKey(uniqueCitationKey(project.getId(), managedReference, document.getAuthors(), null));
                    pr.setAddedBy(user);
                    return projectReferenceRepository.save(pr);
                });

        if (!sourceLinkRepository.existsByDocumentId(document.getId())) {
            ReferenceSourceLink link = new ReferenceSourceLink();
            link.setReference(managedReference);
            link.setDocument(document);
            link.setDocumentVersion(version);
            link.setType(ReferenceSourceLinkType.FULL_TEXT);
            sourceLinkRepository.save(link);
        }
        return projectReference;
    }

    private ReferenceEntry buildReference(Document document, String normalizedDoi, User user) {
        ReferenceEntry entry = new ReferenceEntry();
        entry.setType(resolveType(document.getSourceType()));
        entry.setTitle(firstNonBlank(document.getBibliographicTitle(), null, null));
        if (isBlank(entry.getTitle())) {
            entry.setTitle("REFERENCE_METADATA_INCOMPLETE");
        }
        entry.setContainerTitle(firstNonBlank(document.getJournal(), document.getConference(), null));
        entry.setPublicationYear(document.getPublicationYear());
        entry.setVolume(blankToNull(document.getVolume()));
        entry.setIssue(blankToNull(document.getIssue()));
        entry.setPages(blankToNull(document.getPages()));
        entry.setPublisher(blankToNull(document.getPublisher()));
        entry.setDoi(blankToNull(document.getDoi()));
        entry.setNormalizedDoi(normalizedDoi);
        entry.setUrl(blankToNull(document.getUrl()));
        entry.setMetadataStatus(metadataStatus(document));
        entry.setMetadataSource(blankToNull(document.getBibliographicMetadataSource()));
        entry.setMetadataConfidence(document.getBibliographicMetadataConfidence());
        entry.setMetadataReviewStatus(metadataStatus(document).name());
        entry.setOrigin(ContentOrigin.IMPORTED);
        entry.setCreatedBy(user);
        return entry;
    }

    private void syncReferenceFromDocument(ReferenceEntry reference, Document document) {
        if (reference.getMetadataStatus() == ReferenceMetadataStatus.VERIFIED) {
            return;
        }
        if (canReplaceTitle(reference.getTitle(), document.getBibliographicTitle())) {
            reference.setTitle(document.getBibliographicTitle().trim());
        }
        if (isBlank(reference.getContainerTitle()) || "Source details pending".equalsIgnoreCase(reference.getContainerTitle())) {
            reference.setContainerTitle(firstNonBlank(document.getJournal(), document.getConference(), document.getPublisher()));
        }
        if (document.getPublicationYear() != null) {
            reference.setPublicationYear(document.getPublicationYear());
        }
        if (isBlank(reference.getVolume())) reference.setVolume(blankToNull(document.getVolume()));
        if (isBlank(reference.getIssue())) reference.setIssue(blankToNull(document.getIssue()));
        if (isBlank(reference.getPages())) reference.setPages(blankToNull(document.getPages()));
        if (isBlank(reference.getPublisher()) && !isBlank(document.getPublisher())) reference.setPublisher(document.getPublisher().trim());
        if (isBlank(reference.getDoi()) && !isBlank(document.getDoi())) reference.setDoi(blankToNull(document.getDoi()));
        if (isBlank(reference.getNormalizedDoi()) && !isBlank(document.getDoi())) reference.setNormalizedDoi(normalizationService.normalizeDoi(document.getDoi()));
        if (isBlank(reference.getUrl())) reference.setUrl(blankToNull(document.getUrl()));
        reference.setType(resolveType(document.getSourceType()));
        reference.setMetadataStatus(metadataStatus(document));
        reference.setMetadataSource(blankToNull(document.getBibliographicMetadataSource()));
        reference.setMetadataConfidence(document.getBibliographicMetadataConfidence());
        reference.setMetadataReviewStatus(metadataStatus(document).name());
        entryRepository.save(reference);

        if (!isBlank(document.getAuthors()) && reference.getMetadataStatus() != ReferenceMetadataStatus.VERIFIED) {
            List<ReferenceAuthor> existing = authorRepository.findAllByReferenceIdAndRoleOrderByDisplayOrderAsc(reference.getId(), AuthorRole.AUTHOR);
            authorRepository.deleteAll(existing);
            authorRepository.flush();
            saveAuthors(reference, document.getAuthors());
        }
    }

    private void saveAuthors(ReferenceEntry reference, String authors) {
        if (authors == null || authors.isBlank()) {
            return;
        }
        int order = 1;
        for (String name : splitAuthors(authors)) {
            ReferenceAuthor author = new ReferenceAuthor();
            author.setReference(reference);
            author.setLiteralName(name);
            author.setDisplayOrder(order++);
            author.setRole(AuthorRole.AUTHOR);

            String familyName = null;
            String givenName = null;
            if (name.contains(",")) {
                String[] parts = name.split(",", 2);
                familyName = parts[0].trim();
                givenName = parts[1].trim();
            } else {
                String[] parts = name.trim().split("\\s+");
                if (parts.length > 1) {
                    familyName = parts[parts.length - 1];
                    givenName = name.substring(0, name.lastIndexOf(familyName)).trim();
                } else if (parts.length == 1) {
                    familyName = parts[0];
                }
            }
            author.setFamilyName(familyName);
            author.setGivenName(givenName);
            authorRepository.save(author);
        }
    }

    private java.util.List<String> splitAuthors(String authors) {
        String normalized = authors.replace(" and ", ";").replace("&", ";");
        String[] parts = normalized.contains(";") ? normalized.split(";") : normalized.split(",");
        return Arrays.stream(parts)
                .map(String::trim)
                .filter(part -> !part.isBlank())
                .toList();
    }

    private ReferenceType resolveType(String sourceType) {
        if (sourceType == null || sourceType.isBlank()) {
            return ReferenceType.OTHER;
        }
        String value = sourceType.trim().toUpperCase(Locale.ROOT).replace('-', '_').replace(' ', '_');
        if (value.contains("JOURNAL") || value.contains("ARTICLE")) {
            return ReferenceType.JOURNAL_ARTICLE;
        }
        if (value.contains("BOOK")) {
            return ReferenceType.BOOK;
        }
        if (value.contains("CONFERENCE")) {
            return ReferenceType.CONFERENCE_PAPER;
        }
        if (value.contains("THESIS")) {
            return ReferenceType.THESIS;
        }
        if (value.contains("REPORT")) {
            return ReferenceType.REPORT;
        }
        return ReferenceType.OTHER;
    }

    private ReferenceMetadataStatus metadataStatus(Document document) {
        if (document.getBibliographicMetadataStatus() == ReferenceMetadataStatus.VERIFIED) {
            return ReferenceMetadataStatus.VERIFIED;
        }
        boolean hasCore = !isBlank(document.getAuthors())
                && document.getPublicationYear() != null
                && !isBlank(document.getBibliographicTitle());
        boolean hasAny = !isBlank(document.getAuthors())
                || document.getPublicationYear() != null
                || !isBlank(document.getBibliographicTitle())
                || !isBlank(document.getJournal())
                || !isBlank(document.getConference())
                || !isBlank(document.getDoi())
                || !isBlank(document.getPublisher());
        if (hasCore && (!isBlank(document.getJournal()) || !isBlank(document.getConference()) || !isBlank(document.getDoi()) || !isBlank(document.getPublisher()))) {
            return ReferenceMetadataStatus.COMPLETE;
        }
        return hasAny ? ReferenceMetadataStatus.PARTIAL : ReferenceMetadataStatus.INCOMPLETE;
    }

    private String uniqueCitationKey(UUID projectId, ReferenceEntry reference, String authors, UUID existingProjectReferenceId) {
        String stem = "ref";
        if (!isBlank(authors)) {
            stem = splitAuthors(authors).stream().findFirst().orElse("ref");
            if (stem.contains(",")) {
                stem = stem.split(",")[0].trim();
            } else {
                String[] parts = stem.trim().split("\\s+");
                if (parts.length > 0) {
                    stem = parts[parts.length - 1]; // last name
                }
            }
        }
        stem = stem.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "");
        if (stem.isBlank()) {
            stem = "ref";
        }
        String yearStr = reference.getPublicationYear() == null ? "nd" : String.valueOf(reference.getPublicationYear());
        String base = stem + yearStr;
        String key = base;
        int suffix = 2;
        while (existingProjectReferenceId == null
                ? projectReferenceRepository.existsByProjectIdAndCitationKey(projectId, key)
                : projectReferenceRepository.existsByProjectIdAndCitationKeyAndIdNot(projectId, key, existingProjectReferenceId)) {
            key = base + suffix++;
        }
        return key;
    }

    private String firstNonBlank(String first, String second, String third) {
        if (!isBlank(first)) return first.trim();
        if (!isBlank(second)) return second.trim();
        if (!isBlank(third)) return third.trim();
        return null;
    }

    private String blankToNull(String value) {
        return isBlank(value) ? null : value.trim();
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private boolean canReplaceTitle(String current, String candidate) {
        if (isBlank(candidate)) return false;
        if (isBlank(current)) return true;
        if ("REFERENCE_METADATA_INCOMPLETE".equalsIgnoreCase(current.trim())) return true;
        if (looksLikeFilename(current)) return true;
        return current.length() < candidate.length() && candidate.length() >= 25;
    }

    private boolean looksLikeFilename(String value) {
        if (isBlank(value)) return false;
        String v = value.trim();
        if (v.matches("(?i).+\\.(pdf|docx?|txt)$")) return true;
        if (v.contains(" ")) return false;
        return v.matches("[A-Za-z0-9._-]{6,}") && (v.contains("-") || v.contains("_") || v.matches(".*\\d{3,}.*"));
    }
}
