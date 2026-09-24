package com.researchassistant.reference.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.researchassistant.analysis.entity.CitationStyle;
import com.researchassistant.common.enums.ContentOrigin;
import com.researchassistant.common.exception.ResourceNotFoundException;
import com.researchassistant.identity.entity.User;
import com.researchassistant.project.entity.ResearchProject;
import com.researchassistant.project.service.ProjectAuthorizationService;
import com.researchassistant.reference.dto.ReferenceDtos.*;
import com.researchassistant.reference.entity.*;
import com.researchassistant.reference.repository.*;
import com.researchassistant.security.audit.SecurityAuditEventType;
import com.researchassistant.security.audit.SecurityAuditService;
import com.researchassistant.cache.AppCacheNames;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.domain.*;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.HexFormat;

@Service
public class ReferenceLibraryService {
    private final ReferenceEntryRepository entryRepository;
    private final ReferenceAuthorRepository authorRepository;
    private final ProjectReferenceRepository projectReferenceRepository;
    private final ReferenceImportJobRepository importJobRepository;
    private final ReferenceImportItemRepository importItemRepository;
    private final ProjectAuthorizationService authorizationService;
    private final ReferenceNormalizationService normalizationService;
    private final ReferenceDuplicateDetectionService duplicateDetectionService;
    private final CitationFormattingService citationFormattingService;
    private final ReferenceInteroperabilityService interoperabilityService;
    private final SecurityAuditService auditService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public ReferenceLibraryService(ReferenceEntryRepository entryRepository, ReferenceAuthorRepository authorRepository,
            ProjectReferenceRepository projectReferenceRepository, ReferenceImportJobRepository importJobRepository,
            ReferenceImportItemRepository importItemRepository, ProjectAuthorizationService authorizationService,
            ReferenceNormalizationService normalizationService, ReferenceDuplicateDetectionService duplicateDetectionService,
            CitationFormattingService citationFormattingService, ReferenceInteroperabilityService interoperabilityService,
            SecurityAuditService auditService) {
        this.entryRepository = entryRepository;
        this.authorRepository = authorRepository;
        this.projectReferenceRepository = projectReferenceRepository;
        this.importJobRepository = importJobRepository;
        this.importItemRepository = importItemRepository;
        this.authorizationService = authorizationService;
        this.normalizationService = normalizationService;
        this.duplicateDetectionService = duplicateDetectionService;
        this.citationFormattingService = citationFormattingService;
        this.interoperabilityService = interoperabilityService;
        this.auditService = auditService;
    }

    @Transactional
    @CacheEvict(cacheNames = AppCacheNames.FORMATTED_CITATIONS, allEntries = true)
    public ReferenceResponse create(UUID projectId, User user, CreateReferenceRequest request) {
        ResearchProject project = authorizationService.requireProjectEditor(projectId, user).project();
        ReferenceEntry entry = buildEntry(request, user);
        ReferenceEntry saved = entryRepository.save(entry);
        saveAuthors(saved, request.authors());
        ProjectReference pr = new ProjectReference();
        pr.setProject(project);
        pr.setReference(saved);
        pr.setCitationKey(uniqueCitationKey(projectId, saved, request.authors()));
        pr.setAddedBy(user);
        projectReferenceRepository.save(pr);
        auditService.record(user.getId(), SecurityAuditEventType.REFERENCE_CREATED);
        return response(pr);
    }

    @Transactional(readOnly = true)
    public Page<ReferenceResponse> list(UUID projectId, User user, String author, Integer year, ReferenceType type,
            String title, String citationKey, ReferenceMetadataStatus metadataStatus, Pageable pageable) {
        authorizationService.requireProjectViewer(projectId, user);
        List<ReferenceResponse> filtered = projectReferenceRepository.findAllByProjectIdAndStatusOrderByCitationKeyAsc(projectId, ProjectReferenceStatus.ACTIVE).stream()
                .filter(pr -> citationKey == null || pr.getCitationKey().equalsIgnoreCase(citationKey))
                .filter(pr -> title == null || pr.getReference().getTitle().toLowerCase(Locale.ROOT).contains(title.toLowerCase(Locale.ROOT)))
                .filter(pr -> year == null || year.equals(pr.getReference().getPublicationYear()))
                .filter(pr -> type == null || type == pr.getReference().getType())
                .filter(pr -> metadataStatus == null || metadataStatus == pr.getReference().getMetadataStatus())
                .filter(pr -> author == null || authorRepository.findAllByReferenceIdOrderByDisplayOrderAsc(pr.getReference().getId()).stream().anyMatch(a -> displayAuthor(a).toLowerCase(Locale.ROOT).contains(author.toLowerCase(Locale.ROOT))))
                .map(this::response)
                .toList();
        int start = (int) Math.min(pageable.getOffset(), filtered.size());
        int end = Math.min(start + pageable.getPageSize(), filtered.size());
        return new PageImpl<>(filtered.subList(start, end), pageable, filtered.size());
    }

    @Transactional(readOnly = true)
    public ReferenceResponse get(UUID referenceId, User user) {
        ProjectReference pr = projectReferenceRepository.findById(referenceId)
                .orElseGet(() -> projectReferenceRepository.findAll().stream().filter(p -> p.getReference().getId().equals(referenceId)).findFirst().orElseThrow(() -> new ResourceNotFoundException("Reference not found.")));
        authorizationService.requireProjectViewer(pr.getProject().getId(), user);
        return response(pr);
    }

    @Transactional
    @CacheEvict(cacheNames = AppCacheNames.FORMATTED_CITATIONS, allEntries = true)
    public ReferenceResponse update(UUID referenceId, User user, UpdateReferenceRequest request) {
        ProjectReference pr = loadProjectReference(referenceId);
        authorizationService.requireProjectEditor(pr.getProject().getId(), user);
        ReferenceEntry entry = pr.getReference();
        if (request.title() != null) entry.setTitle(request.title().trim());
        if (request.containerTitle() != null) entry.setContainerTitle(blankToNull(request.containerTitle()));
        if (request.publicationYear() != null) entry.setPublicationYear(request.publicationYear());
        if (request.volume() != null) entry.setVolume(blankToNull(request.volume()));
        if (request.issue() != null) entry.setIssue(blankToNull(request.issue()));
        if (request.pages() != null) entry.setPages(blankToNull(request.pages()));
        if (request.publisher() != null) entry.setPublisher(blankToNull(request.publisher()));
        if (request.publisherPlace() != null) entry.setPublisherPlace(blankToNull(request.publisherPlace()));
        if (request.doi() != null) { entry.setDoi(blankToNull(request.doi())); entry.setNormalizedDoi(normalizationService.normalizeDoi(request.doi())); }
        if (request.url() != null) entry.setUrl(blankToNull(request.url()));
        if (request.metadataStatus() != null) {
            entry.setMetadataStatus(request.metadataStatus());
        } else if (touchesBibliographicMetadata(request)) {
            entry.setMetadataStatus(ReferenceMetadataStatus.VERIFIED);
            entry.setMetadataSource("USER_CONFIRMED");
            entry.setMetadataConfidence(1.0d);
            entry.setMetadataReviewStatus(ReferenceMetadataStatus.VERIFIED.name());
            entry.setVerifiedAt(OffsetDateTime.now());
        }
        if (request.authors() != null) { authorRepository.deleteAll(authorRepository.findAllByReferenceIdOrderByDisplayOrderAsc(entry.getId())); saveAuthors(entry, request.authors()); }
        if (request.availableForCitation() != null) {
            pr.setAvailableForCitation(request.availableForCitation());
        }
        if (request.availableForResearchAi() != null) {
            pr.setAvailableForResearchAi(request.availableForResearchAi());
        }
        if (request.citationKey() != null && !request.citationKey().isBlank()) {
            String cleanKey = request.citationKey().trim().replaceAll("[^A-Za-z0-9_-]", "");
            if (!cleanKey.isEmpty()) {
                pr.setCitationKey(cleanKey);
            }
        }
        projectReferenceRepository.save(pr);
        auditService.record(user.getId(), SecurityAuditEventType.REFERENCE_UPDATED);
        return response(pr);
    }

    @Transactional
    public ReferenceResponse setAvailableForCitation(UUID referenceId, User user, boolean available) {
        ProjectReference pr = loadProjectReference(referenceId);
        authorizationService.requireProjectEditor(pr.getProject().getId(), user);
        pr.setAvailableForCitation(available);
        auditService.record(user.getId(), SecurityAuditEventType.REFERENCE_UPDATED);
        return response(pr);
    }

    @Transactional
    public ReferenceResponse setAvailableForResearchAi(UUID referenceId, User user, boolean available) {
        ProjectReference pr = loadProjectReference(referenceId);
        authorizationService.requireProjectEditor(pr.getProject().getId(), user);
        pr.setAvailableForResearchAi(available);
        auditService.record(user.getId(), SecurityAuditEventType.REFERENCE_UPDATED);
        return response(pr);
    }

    @Transactional
    public ReferenceResponse setUsageScope(UUID referenceId, User user, Boolean availableForResearchAi, Boolean availableForCitation) {
        ProjectReference pr = loadProjectReference(referenceId);
        authorizationService.requireProjectEditor(pr.getProject().getId(), user);
        if (availableForResearchAi != null) {
            pr.setAvailableForResearchAi(availableForResearchAi);
        }
        if (availableForCitation != null) {
            pr.setAvailableForCitation(availableForCitation);
        }
        auditService.record(user.getId(), SecurityAuditEventType.REFERENCE_UPDATED);
        return response(pr);
    }

    @Transactional
    public ReferenceResponse archive(UUID referenceId, User user) {
        ProjectReference pr = loadProjectReference(referenceId);
        authorizationService.requireProjectEditor(pr.getProject().getId(), user);
        pr.setStatus(ProjectReferenceStatus.ARCHIVED);
        auditService.record(user.getId(), SecurityAuditEventType.REFERENCE_UPDATED);
        return response(pr);
    }

    public DuplicateCheckResponse checkDuplicates(UUID projectId, User user, DuplicateCheckRequest request) {
        authorizationService.requireProjectViewer(projectId, user);
        return duplicateDetectionService.check(projectReferences(projectId), request);
    }

    public FormattedCitation format(User user, FormatCitationRequest request) {
        ReferenceEntry entry = entryRepository.findById(request.referenceId()).orElseThrow(() -> new ResourceNotFoundException("Reference not found."));
        ProjectReference projectReference = projectReferenceRepository.findAll().stream()
                .filter(pr -> pr.getReference().getId().equals(entry.getId()))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Reference not found."));
        authorizationService.requireProjectViewer(projectReference.getProject().getId(), user);
        return citationFormattingService.format(entry, request.style(), request.context(), request.citationNumber());
    }

    @Transactional
    public ImportJobResponse importPreview(UUID projectId, User user, ImportRequest request) {
        ResearchProject project = authorizationService.requireProjectEditor(projectId, user).project();
        List<CreateReferenceRequest> parsed = interoperabilityService.parse(request.format(), request.content());
        ReferenceImportJob job = new ReferenceImportJob();
        job.setProject(project);
        job.setFormat(request.format());
        job.setStatus(ReferenceImportStatus.PREVIEW_READY);
        job.setOriginalFilename(safeName(request.originalFilename()));
        job.setFileSizeBytes(request.content().getBytes(StandardCharsets.UTF_8).length);
        job.setChecksumSha256(sha256(request.content().getBytes(StandardCharsets.UTF_8)));
        job.setUploadedBy(user);
        job.setDetectedEntries(parsed.size());
        job.setImportedEntries(0);
        job.setDuplicateEntries(0);
        job.setSkippedEntries(0);
        job.setFailedEntries(0);
        ReferenceImportJob saved = importJobRepository.save(job);
        int ordinal = 1;
        int duplicates = 0;
        for (CreateReferenceRequest item : parsed) {
            DuplicateCheckResponse dup = duplicateDetectionService.check(projectReferences(projectId), new DuplicateCheckRequest(item.doi(), item.title(), item.publicationYear(), firstAuthor(item)));
            ReferenceImportItem preview = new ReferenceImportItem();
            preview.setImportJob(saved);
            preview.setItemOrdinal(ordinal++);
            preview.setParsedJson(writeJson(item));
            preview.setClassification(!dup.exactDuplicates().isEmpty() ? ReferenceImportClassification.EXACT_DUPLICATE : !dup.possibleDuplicates().isEmpty() ? ReferenceImportClassification.POSSIBLE_DUPLICATE : ReferenceImportClassification.NEW);
            if (preview.getClassification() != ReferenceImportClassification.NEW) duplicates++;
            preview.setMetadataWarnings(metadataWarnings(item));
            importItemRepository.save(preview);
        }
        saved.setDuplicateEntries(duplicates);
        return new ImportJobResponse(saved.getId(), saved.getFormat(), saved.getStatus(), parsed.size(), 0, duplicates);
    }

    @Transactional(readOnly = true)
    public List<ImportPreviewItem> preview(UUID importJobId, User user) {
        ReferenceImportJob job = loadJob(importJobId, user, false);
        return importItemRepository.findAllByImportJobIdOrderByItemOrdinalAsc(job.getId()).stream().map(i -> {
            CreateReferenceRequest parsed = readJson(i.getParsedJson());
            return new ImportPreviewItem(i.getId(), i.getItemOrdinal(), parsed.title(), parsed.publicationYear(), parsed.doi(), i.getClassification(), i.getMetadataWarnings());
        }).toList();
    }

    @Transactional
    public ImportJobResponse confirm(UUID importJobId, User user, ConfirmImportRequest request) {
        ReferenceImportJob job = loadJob(importJobId, user, true);
        int imported = 0, skipped = 0, duplicates = 0;
        for (ReferenceImportItem item : importItemRepository.findAllByImportJobIdOrderByItemOrdinalAsc(job.getId())) {
            if (item.getClassification() == ReferenceImportClassification.EXACT_DUPLICATE && request.skipExactDuplicates()) { skipped++; duplicates++; continue; }
            if (item.getClassification() == ReferenceImportClassification.POSSIBLE_DUPLICATE && !request.importAsNew()) { skipped++; duplicates++; continue; }
            create(job.getProject().getId(), user, readJson(item.getParsedJson()));
            imported++;
        }
        job.setStatus(duplicates > 0 ? ReferenceImportStatus.COMPLETED_WITH_WARNINGS : ReferenceImportStatus.COMPLETED);
        job.setImportedEntries(imported);
        job.setSkippedEntries(skipped);
        job.setDuplicateEntries(duplicates);
        job.setCompletedAt(OffsetDateTime.now());
        auditService.record(user.getId(), SecurityAuditEventType.REFERENCE_IMPORTED);
        return new ImportJobResponse(job.getId(), job.getFormat(), job.getStatus(), job.getDetectedEntries() == null ? 0 : job.getDetectedEntries(), imported, duplicates);
    }

    @Transactional(readOnly = true)
    public ExportedReferences export(UUID projectId, User user, ReferenceImportFormat format, List<UUID> selectedIds) {
        authorizationService.requireProjectViewer(projectId, user);
        List<ProjectReference> refs = projectReferenceRepository.findAllByProjectIdAndStatusOrderByCitationKeyAsc(projectId, ProjectReferenceStatus.ACTIVE).stream()
                .filter(pr -> selectedIds == null || selectedIds.isEmpty() || selectedIds.contains(pr.getReference().getId()) || selectedIds.contains(pr.getId()))
                .toList();
        List<ReferenceInteroperabilityService.ReferenceExportView> views = refs.stream().map(pr -> new ReferenceInteroperabilityService.ReferenceExportView(
                pr.getReference().getType(), pr.getReference().getTitle(), pr.getReference().getContainerTitle(), pr.getReference().getPublicationYear(),
                pr.getReference().getVolume(), pr.getReference().getIssue(), pr.getReference().getPages(), pr.getReference().getPublisher(),
                pr.getReference().getDoi(), pr.getReference().getUrl(), pr.getReference().getIsbn(), pr.getReference().getIssn(),
                authorRepository.findAllByReferenceIdOrderByDisplayOrderAsc(pr.getReference().getId()).stream().map(this::displayAuthor).toList())).toList();
        String content = interoperabilityService.export(format, views);
        auditService.record(user.getId(), SecurityAuditEventType.REFERENCE_EXPORT_CREATED);
        String extension = format == ReferenceImportFormat.RIS ? "ris" : format == ReferenceImportFormat.BIBTEX ? "bib" : "xml";
        return new ExportedReferences("research-project-references-" + java.time.LocalDate.now() + "." + extension, MediaType.TEXT_PLAIN_VALUE, content);
    }

    private ReferenceEntry buildEntry(CreateReferenceRequest r, User user) {
        ReferenceEntry entry = new ReferenceEntry();
        entry.setType(r.type() == null ? ReferenceType.OTHER : r.type());
        entry.setTitle(required(r.title()));
        entry.setContainerTitle(blankToNull(r.containerTitle()));
        entry.setPublicationYear(r.publicationYear());
        entry.setVolume(blankToNull(r.volume()));
        entry.setIssue(blankToNull(r.issue()));
        entry.setPages(blankToNull(r.pages()));
        entry.setPublisher(blankToNull(r.publisher()));
        entry.setPublisherPlace(blankToNull(r.publisherPlace()));
        entry.setEdition(blankToNull(r.edition()));
        entry.setInstitution(blankToNull(r.institution()));
        entry.setConferenceName(blankToNull(r.conferenceName()));
        entry.setAbstractText(blankToNull(r.abstractText()));
        entry.setLanguageCode(blankToNull(r.languageCode()));
        entry.setDoi(blankToNull(r.doi()));
        entry.setNormalizedDoi(normalizationService.normalizeDoi(r.doi()));
        entry.setUrl(blankToNull(r.url()));
        entry.setIsbn(blankToNull(r.isbn()));
        entry.setIssn(blankToNull(r.issn()));
        entry.setPmid(blankToNull(r.pmid()));
        entry.setArxivId(blankToNull(r.arxivId()));
        entry.setMetadataStatus(r.metadataStatus() == null ? ReferenceMetadataStatus.PARTIAL : r.metadataStatus());
        entry.setOrigin(r.origin() == null ? ContentOrigin.USER : r.origin());
        entry.setCreatedBy(user);
        return entry;
    }

    private void saveAuthors(ReferenceEntry entry, List<AuthorRequest> authors) {
        int order = 1;
        for (AuthorRequest request : authors == null ? List.<AuthorRequest>of() : authors) {
            ReferenceAuthor author = new ReferenceAuthor();
            author.setReference(entry);
            author.setFamilyName(blankToNull(request.familyName()));
            author.setGivenName(blankToNull(request.givenName()));
            author.setLiteralName(blankToNull(request.literalName()));
            author.setOrcid(blankToNull(request.orcid()));
            author.setRole(request.role() == null ? AuthorRole.AUTHOR : request.role());
            author.setDisplayOrder(order++);
            authorRepository.save(author);
        }
    }

    private ReferenceResponse response(ProjectReference pr) {
        List<AuthorResponse> authors = authorRepository.findAllByReferenceIdOrderByDisplayOrderAsc(pr.getReference().getId()).stream()
                .map(a -> new AuthorResponse(a.getFamilyName(), a.getGivenName(), a.getLiteralName(), a.getRole(), a.getDisplayOrder())).toList();
        ReferenceEntry e = pr.getReference();
        return new ReferenceResponse(e.getId(), pr.getId(), pr.getCitationKey(), e.getType(), e.getTitle(),
                e.getContainerTitle(), e.getPublicationYear(), e.getVolume(), e.getIssue(), e.getPages(),
                e.getPublisher(), e.getDoi(), e.getUrl(), e.getMetadataStatus(), e.getMetadataSource(),
                e.getMetadataConfidence(), pr.isAvailableForCitation(), pr.isAvailableForResearchAi(), authors, e.getUpdatedAt());
    }

    private ProjectReference loadProjectReference(UUID id) {
        return projectReferenceRepository.findById(id).orElseGet(() -> projectReferenceRepository.findAll().stream()
                .filter(pr -> pr.getReference().getId().equals(id)).findFirst().orElseThrow(() -> new ResourceNotFoundException("Reference not found.")));
    }

    private List<ReferenceEntry> projectReferences(UUID projectId) {
        return projectReferenceRepository.findAllByProjectIdAndStatusOrderByCitationKeyAsc(projectId, ProjectReferenceStatus.ACTIVE).stream()
                .map(ProjectReference::getReference)
                .toList();
    }

    private String firstAuthor(CreateReferenceRequest item) {
        if (item.authors() == null || item.authors().isEmpty()) return null;
        AuthorRequest first = item.authors().getFirst();
        if (first.literalName() != null && !first.literalName().isBlank()) return first.literalName();
        return ((first.givenName() == null ? "" : first.givenName() + " ") + (first.familyName() == null ? "" : first.familyName())).trim();
    }

    private ReferenceImportJob loadJob(UUID id, User user, boolean edit) {
        ReferenceImportJob job = importJobRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Reference import not found."));
        if (edit) authorizationService.requireProjectEditor(job.getProject().getId(), user); else authorizationService.requireProjectViewer(job.getProject().getId(), user);
        return job;
    }

    private String uniqueCitationKey(UUID projectId, ReferenceEntry entry, List<AuthorRequest> authors) {
        String stem = "ref";
        if (authors != null && !authors.isEmpty()) {
            AuthorRequest first = authors.getFirst();
            stem = first.familyName() != null ? first.familyName() : first.literalName() != null ? first.literalName() : "ref";
        }
        stem = stem.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "");
        String base = stem + (entry.getPublicationYear() == null ? "nd" : entry.getPublicationYear());
        String key = base;
        int suffix = 2;
        while (projectReferenceRepository.existsByProjectIdAndCitationKey(projectId, key)) key = base + suffix++;
        return key;
    }

    private String displayAuthor(ReferenceAuthor author) {
        if (author.getLiteralName() != null) return author.getLiteralName();
        return (author.getGivenName() == null ? "" : author.getGivenName() + " ") + (author.getFamilyName() == null ? "" : author.getFamilyName());
    }

    private String metadataWarnings(CreateReferenceRequest item) {
        List<String> warnings = new ArrayList<>();
        if (item.title() == null || item.title().isBlank()) warnings.add("Missing title");
        if (item.publicationYear() == null) warnings.add("Missing publication year");
        if (item.authors() == null || item.authors().isEmpty()) warnings.add("Missing author");
        return String.join("; ", warnings);
    }

    private String writeJson(CreateReferenceRequest request) { try { return objectMapper.writeValueAsString(request); } catch (Exception e) { throw new IllegalArgumentException("Unable to serialize import preview."); } }
    private CreateReferenceRequest readJson(String json) { try { return objectMapper.readValue(json, CreateReferenceRequest.class); } catch (Exception e) { throw new IllegalArgumentException("Unable to read import preview."); } }
    private String sha256(byte[] bytes) { try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes)); } catch (Exception e) { throw new IllegalStateException("SHA-256 unavailable."); } }
    private String blankToNull(String value) { return value == null || value.isBlank() ? null : value.trim(); }
    private String required(String value) { String v = blankToNull(value); if (v == null) throw new IllegalArgumentException("Reference title is required."); return v; }
    private String safeName(String value) { return blankToNull(value) == null ? "references.txt" : value.replaceAll("[^A-Za-z0-9._-]", "_"); }
    private boolean touchesBibliographicMetadata(UpdateReferenceRequest request) {
        return request.title() != null
                || request.containerTitle() != null
                || request.publicationYear() != null
                || request.volume() != null
                || request.issue() != null
                || request.pages() != null
                || request.publisher() != null
                || request.publisherPlace() != null
                || request.doi() != null
                || request.url() != null
                || request.authors() != null;
    }
    public record ExportedReferences(String filename, String contentType, String content) {}
}
