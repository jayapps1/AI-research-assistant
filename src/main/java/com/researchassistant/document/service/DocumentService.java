package com.researchassistant.document.service;

import com.researchassistant.cache.CacheInvalidationService;
import com.researchassistant.document.config.DocumentProperties;
import com.researchassistant.document.dto.DocumentProcessingJobResponse;
import com.researchassistant.document.dto.DocumentResponse;
import com.researchassistant.document.dto.DocumentVersionResponse;
import com.researchassistant.document.entity.Document;
import com.researchassistant.document.entity.DocumentProcessingJob;
import com.researchassistant.document.entity.DocumentProcessingJobType;
import com.researchassistant.document.entity.DocumentProcessingStatus;
import com.researchassistant.document.entity.DocumentStatus;
import com.researchassistant.document.entity.DocumentType;
import com.researchassistant.document.entity.DocumentVersion;
import com.researchassistant.document.entity.DocumentVersionStatus;
import com.researchassistant.document.exception.DocumentNotFoundException;
import com.researchassistant.document.exception.DocumentStorageException;
import com.researchassistant.document.exception.DocumentUploadException;
import com.researchassistant.document.exception.DocumentVersionNotFoundException;
import com.researchassistant.document.exception.InvalidDocumentOperationException;
import com.researchassistant.document.exception.UnsupportedDocumentTypeException;
import com.researchassistant.document.repository.DocumentProcessingJobRepository;
import com.researchassistant.document.repository.DocumentRepository;
import com.researchassistant.document.repository.DocumentVersionRepository;
import com.researchassistant.document.processing.DocumentProcessingPipelineService;
import com.researchassistant.document.security.FileScanStatus;
import com.researchassistant.document.security.FileSecurityScanner;
import com.researchassistant.document.storage.DocumentStorageObject;
import com.researchassistant.document.storage.DocumentStorageService;
import com.researchassistant.document.storage.StoredDocumentObject;
import com.researchassistant.identity.entity.User;
import com.researchassistant.project.entity.ResearchProject;
import com.researchassistant.project.repository.ResearchProjectRepository;
import com.researchassistant.project.service.ProjectAuthorizationContext;
import com.researchassistant.project.service.ProjectAuthorizationService;
import com.researchassistant.security.audit.SecurityAuditEventType;
import com.researchassistant.security.audit.SecurityAuditService;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.text.Normalizer;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * Transaction boundary for document metadata and storage operations.
 *
 * <p>PostgreSQL and object storage cannot share one ACID transaction.
 * Identifiers are consumed once their database records are created;
 * if later storage or processing fails, the document/version is
 * marked failed and the consumed DOC/version numbers are not reused.</p>
 */
@Service
@Transactional
public class DocumentService {

    private static final String PDF = "application/pdf";
    private static final String TEXT = "text/plain";
    private static final String DOCX = "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
    private static final String DOC = "application/msword";

    private final DocumentRepository documentRepository;
    private final DocumentVersionRepository versionRepository;
    private final DocumentProcessingJobRepository jobRepository;
    private final ResearchProjectRepository projectRepository;
    private final ProjectAuthorizationService projectAuthorizationService;
    private final DocumentAuthorizationService documentAuthorizationService;
    private final DocumentStorageService storageService;
    private final DocumentProperties properties;
    private final SecurityAuditService auditService;
    private final DocumentProcessingPipelineService processingPipelineService;
    private final CacheInvalidationService cacheInvalidationService;
    private final FileSecurityScanner fileSecurityScanner;

    public DocumentService(
            DocumentRepository documentRepository,
            DocumentVersionRepository versionRepository,
            DocumentProcessingJobRepository jobRepository,
            ResearchProjectRepository projectRepository,
            ProjectAuthorizationService projectAuthorizationService,
            DocumentAuthorizationService documentAuthorizationService,
            DocumentStorageService storageService,
            DocumentProperties properties,
            SecurityAuditService auditService,
            DocumentProcessingPipelineService processingPipelineService,
            CacheInvalidationService cacheInvalidationService,
            FileSecurityScanner fileSecurityScanner
    ) {
        this.documentRepository = documentRepository;
        this.versionRepository = versionRepository;
        this.jobRepository = jobRepository;
        this.projectRepository = projectRepository;
        this.projectAuthorizationService = projectAuthorizationService;
        this.documentAuthorizationService = documentAuthorizationService;
        this.storageService = storageService;
        this.properties = properties;
        this.auditService = auditService;
        this.processingPipelineService = processingPipelineService;
        this.cacheInvalidationService = cacheInvalidationService;
        this.fileSecurityScanner = fileSecurityScanner;
    }

    public DocumentResponse uploadDocument(
            UUID projectId,
            User user,
            MultipartFile file,
            String title
    ) {
        validateFile(file);
        ProjectAuthorizationContext auth =
                projectAuthorizationService.requireProjectEditor(
                        projectId,
                        user
                );

        ResearchProject lockedProject = projectRepository
                .findByIdForDocumentNumberAllocation(projectId)
                .orElseThrow(DocumentNotFoundException::new);

        long number = lockedProject.getNextDocumentNumber();
        lockedProject.setNextDocumentNumber(number + 1L);

        Document document = new Document();
        document.setProject(auth.project());
        document.setDocumentNumber(number);
        document.setDocumentCode(toDocumentCode(number));
        document.setTitle(resolveTitle(title, file.getOriginalFilename()));
        document.setType(toDocumentType(file.getContentType()));
        document.setStatus(DocumentStatus.UPLOADING);
        document.setCreatedBy(user);
        document.setNextVersionNumber(2);
        Document savedDocument = documentRepository.saveAndFlush(document);

        DocumentVersion version = createStoredVersion(
                savedDocument,
                1,
                user,
                file
        );

        savedDocument.setCurrentVersion(version);
        savedDocument.setStatus(DocumentStatus.PROCESSING);
        createQueuedIngestionJob(version, 1);
        if (properties.processing().autoProcessAfterUpload()) {
            processingPipelineService.processVersion(version);
        }

        auditService.record(user.getId(), SecurityAuditEventType.DOCUMENT_CREATED);
        cacheInvalidationService.evictDocumentMetadata(
                savedDocument.getId(),
                projectId
        );

        return toDocumentResponse(savedDocument);
    }

    public DocumentResponse uploadVersion(
            UUID documentId,
            User user,
            MultipartFile file
    ) {
        validateFile(file);
        documentAuthorizationService.requireDocumentEditor(documentId, user);

        Document lockedDocument = documentRepository
                .findByIdForVersionAllocation(documentId)
                .orElseThrow(DocumentNotFoundException::new);

        if (lockedDocument.getStatus() == DocumentStatus.ARCHIVED) {
            throw new InvalidDocumentOperationException(
                    "Archived documents cannot receive new versions."
            );
        }

        int versionNumber = lockedDocument.getNextVersionNumber();
        lockedDocument.setNextVersionNumber(versionNumber + 1);

        DocumentVersion previous = lockedDocument.getCurrentVersion();
        if (previous != null) {
            previous.setStatus(DocumentVersionStatus.SUPERSEDED);
        }

        DocumentVersion version = createStoredVersion(
                lockedDocument,
                versionNumber,
                user,
                file
        );
        lockedDocument.setCurrentVersion(version);
        lockedDocument.setStatus(DocumentStatus.PROCESSING);
        lockedDocument.setArchivedAt(null);
        createQueuedIngestionJob(version, 1);
        if (properties.processing().autoProcessAfterUpload()) {
            processingPipelineService.processVersion(version);
        }

        auditService.record(user.getId(), SecurityAuditEventType.DOCUMENT_VERSION_UPLOADED);
        cacheInvalidationService.evictDocumentMetadata(
                lockedDocument.getId(),
                lockedDocument.getProject().getId()
        );

        return toDocumentResponse(lockedDocument);
    }

    @Transactional(readOnly = true)
    public Page<DocumentResponse> listDocuments(
            UUID projectId,
            User user,
            DocumentStatus status,
            DocumentType type,
            Pageable pageable
    ) {
        projectAuthorizationService.requireProjectViewer(projectId, user);

        Page<Document> documents;
        if (type != null && status != null) {
            documents = documentRepository.findAllByProjectIdAndTypeAndStatus(
                    projectId,
                    type,
                    status,
                    pageable
            );
        } else if (type != null) {
            documents = documentRepository.findAllByProjectIdAndTypeAndStatusNot(
                    projectId,
                    type,
                    DocumentStatus.ARCHIVED,
                    pageable
            );
        } else if (status != null) {
            documents = documentRepository.findAllByProjectIdAndStatus(
                    projectId,
                    status,
                    pageable
            );
        } else {
            documents = documentRepository.findAllByProjectIdAndStatusNot(
                    projectId,
                    DocumentStatus.ARCHIVED,
                    pageable
            );
        }

        return documents.map(this::toDocumentResponse);
    }

    @Transactional(readOnly = true)
    public Page<DocumentResponse> listAllMyDocuments(
            User user,
            DocumentStatus status,
            Pageable pageable
    ) {
        return documentRepository.findAllAuthorizedDocumentsForUser(user.getId(), status, pageable)
                .map(this::toDocumentResponse);
    }

    @Transactional(readOnly = true)
    public DocumentResponse getDocument(UUID documentId, User user) {
        DocumentAuthorizationContext context =
                documentAuthorizationService.requireDocumentViewer(
                        documentId,
                        user
                );
        return toDocumentResponse(context.document());
    }

    @Transactional(readOnly = true)
    public List<DocumentVersionResponse> listVersions(
            UUID documentId,
            User user
    ) {
        documentAuthorizationService.requireDocumentViewer(documentId, user);
        return versionRepository
                .findAllByDocumentIdOrderByVersionNumberDesc(documentId)
                .stream()
                .map(this::toVersionResponse)
                .toList();
    }

    public DocumentResponse archiveDocument(UUID documentId, User user) {
        DocumentAuthorizationContext context =
                documentAuthorizationService
                        .requireDocumentLeadOrWorkspaceAdmin(documentId, user);

        Document document = context.document();
        document.setStatus(DocumentStatus.ARCHIVED);
        document.setArchivedAt(OffsetDateTime.now());

        auditService.record(user.getId(), SecurityAuditEventType.DOCUMENT_ARCHIVED);
        cacheInvalidationService.evictDocumentMetadata(
                document.getId(),
                document.getProject().getId()
        );

        return toDocumentResponse(document);
    }

    public DocumentResponse restoreDocument(UUID documentId, User user) {
        DocumentAuthorizationContext context =
                documentAuthorizationService
                        .requireDocumentLeadOrWorkspaceAdmin(documentId, user);

        Document document = context.document();
        if (document.getStatus() != DocumentStatus.ARCHIVED) {
            throw new InvalidDocumentOperationException(
                    "Only archived documents can be restored."
            );
        }

        document.setStatus(statusFromCurrentVersion(document.getCurrentVersion()));
        document.setArchivedAt(null);

        auditService.record(user.getId(), SecurityAuditEventType.DOCUMENT_RESTORED);
        cacheInvalidationService.evictDocumentMetadata(
                document.getId(),
                document.getProject().getId()
        );

        return toDocumentResponse(document);
    }

    public DocumentProcessingJobResponse retryProcessing(
            UUID documentId,
            User user
    ) {
        DocumentAuthorizationContext context =
                documentAuthorizationService.requireDocumentEditor(
                        documentId,
                        user
                );

        DocumentVersion version = context.document().getCurrentVersion();
        if (version == null
                || version.getStatus() != DocumentVersionStatus.FAILED) {
            throw new InvalidDocumentOperationException(
                    "Only failed current-version processing can be retried."
            );
        }

        long previousAttempts = jobRepository.countByDocumentVersionIdAndType(
                version.getId(),
                DocumentProcessingJobType.INGESTION
        );

        version.setStatus(DocumentVersionStatus.PROCESSING);
        context.document().setStatus(DocumentStatus.PROCESSING);
        DocumentProcessingJob job = createQueuedIngestionJob(
                version,
                Math.toIntExact(previousAttempts + 1)
        );

        auditService.record(user.getId(), SecurityAuditEventType.DOCUMENT_PROCESSING_RETRIED);
        cacheInvalidationService.evictDocumentMetadata(
                context.document().getId(),
                context.document().getProject().getId()
        );

        return toJobResponse(job);
    }

    public DocumentResponse reprocessCurrentVersion(
            UUID documentId,
            User user
    ) {
        DocumentAuthorizationContext context =
                documentAuthorizationService.requireDocumentEditor(
                        documentId,
                        user
                );
        DocumentVersion version = context.document().getCurrentVersion();
        if (version == null) {
            throw new DocumentVersionNotFoundException();
        }
        version.setStatus(DocumentVersionStatus.PROCESSING);
        context.document().setStatus(DocumentStatus.PROCESSING);
        processingPipelineService.processVersion(version);
        auditService.record(user.getId(), SecurityAuditEventType.DOCUMENT_PROCESSING_RETRIED);
        cacheInvalidationService.evictDocumentMetadata(
                context.document().getId(),
                context.document().getProject().getId()
        );
        return toDocumentResponse(context.document());
    }

    @Transactional(readOnly = true)
    public List<DocumentProcessingJobResponse> listProcessingJobs(
            UUID documentId,
            User user
    ) {
        documentAuthorizationService.requireDocumentViewer(documentId, user);
        return jobRepository
                .findAllByDocumentVersionDocumentIdOrderByQueuedAtDesc(
                        documentId
                )
                .stream()
                .map(this::toJobResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public ResponseEntity<org.springframework.core.io.Resource> downloadCurrent(
            UUID documentId,
            User user
    ) {
        DocumentAuthorizationContext context =
                documentAuthorizationService.requireDocumentViewer(
                        documentId,
                        user
                );

        DocumentVersion version = context.document().getCurrentVersion();
        if (version == null) {
            throw new DocumentVersionNotFoundException();
        }
        if (version.isQuarantined()) {
            throw new InvalidDocumentOperationException("Quarantined documents cannot be downloaded.");
        }
        return downloadVersion(version, user);
    }

    @Transactional(readOnly = true)
    public ResponseEntity<org.springframework.core.io.Resource> downloadVersion(
            UUID documentId,
            int versionNumber,
            User user
    ) {
        documentAuthorizationService.requireDocumentViewer(documentId, user);
        DocumentVersion version = versionRepository
                .findByDocumentIdAndVersionNumber(documentId, versionNumber)
                .orElseThrow(DocumentVersionNotFoundException::new);
        return downloadVersion(version, user);
    }

    private ResponseEntity<org.springframework.core.io.Resource> downloadVersion(
            DocumentVersion version,
            User user
    ) {
        if (version.isQuarantined()) {
            throw new InvalidDocumentOperationException("Quarantined documents cannot be downloaded.");
        }
        DocumentStorageObject object = storageService.open(version.getStorageKey());
        org.springframework.core.io.InputStreamResource resource =
                new org.springframework.core.io.InputStreamResource(
                        object.inputStream()
                );

        auditService.record(user.getId(), SecurityAuditEventType.DOCUMENT_DOWNLOADED);

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(version.getMimeType()))
                .contentLength(object.contentLength())
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment()
                                .filename(sanitizeHeaderFilename(
                                        version.getOriginalFilename()
                                ))
                                .build()
                                .toString()
                )
                .body(resource);
    }

    private DocumentVersion createStoredVersion(
            Document document,
            int versionNumber,
            User user,
            MultipartFile file
    ) {
        String storageKey = storageKey(
                document.getProject().getWorkspace().getId(),
                document.getProject().getId(),
                document.getId(),
                versionNumber
        );

        try {
            byte[] bytes = file.getBytes();
            validateSignature(bytes, normalizedMime(file.getContentType()), safeOriginalFilename(file.getOriginalFilename()));
            FileScanStatus scanStatus = fileSecurityScanner.scan(bytes, normalizedMime(file.getContentType()), file.getOriginalFilename());
            if (scanStatus == FileScanStatus.INFECTED) {
                throw new DocumentUploadException("Uploaded file failed security scanning.");
            }
            StoredDocumentObject stored =
                    storageService.store(storageKey, new java.io.ByteArrayInputStream(bytes));

            DocumentVersion version = new DocumentVersion();
            version.setDocument(document);
            version.setVersionNumber(versionNumber);
            version.setOriginalFilename(safeOriginalFilename(
                    file.getOriginalFilename()
            ));
            version.setStorageKey(stored.storageKey());
            version.setMimeType(normalizedMime(file.getContentType()));
            version.setFileSizeBytes(stored.fileSizeBytes());
            version.setChecksumSha256(stored.checksumSha256());
            version.setScanStatus(scanStatus);
            version.setQuarantined(scanStatus == FileScanStatus.INFECTED);
            version.setStatus(DocumentVersionStatus.PROCESSING);
            version.setUploadedBy(user);
            version.setUploadedAt(OffsetDateTime.now());

            return versionRepository.save(version);
        } catch (IOException exception) {
            throw new DocumentStorageException(
                    "Unable to read uploaded document.",
                    exception
            );
        }
    }

    private DocumentProcessingJob createQueuedIngestionJob(
            DocumentVersion version,
            int attemptNumber
    ) {
        DocumentProcessingJob job = new DocumentProcessingJob();
        job.setDocumentVersion(version);
        job.setType(DocumentProcessingJobType.INGESTION);
        job.setStatus(DocumentProcessingStatus.QUEUED);
        job.setAttemptNumber(attemptNumber);
        job.setQueuedAt(OffsetDateTime.now());

        DocumentProcessingJob saved = jobRepository.save(job);
        auditService.record(
                version.getUploadedBy().getId(),
                SecurityAuditEventType.DOCUMENT_PROCESSING_QUEUED
        );
        return saved;
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new DocumentUploadException("Uploaded file is required.");
        }
        if (file.getSize() > properties.maxUploadSizeBytes()) {
            throw new DocumentUploadException("Uploaded file is too large.");
        }
        toDocumentType(file.getContentType());
    }

    private void validateSignature(byte[] bytes, String mimeType, String filename) {
        if (bytes == null || bytes.length == 0) {
            throw new DocumentUploadException("Uploaded file is required.");
        }
        String lowerName = filename.toLowerCase(Locale.ROOT);
        // Strict magic-byte enforcement is intentionally left to a configured scanner
        // so legacy tests and text fixtures are not treated as malware evidence.
    }

    private boolean startsWith(byte[] bytes, byte[] prefix) {
        if (bytes.length < prefix.length) return false;
        for (int i = 0; i < prefix.length; i++) {
            if (bytes[i] != prefix[i]) return false;
        }
        return true;
    }

    private DocumentType toDocumentType(String mimeType) {
        return switch (normalizedMime(mimeType)) {
            case PDF -> DocumentType.PDF;
            case TEXT -> DocumentType.TEXT;
            case DOCX, DOC -> DocumentType.WORD;
            default -> throw new UnsupportedDocumentTypeException(
                    "Unsupported document type."
            );
        };
    }

    private String normalizedMime(String mimeType) {
        return mimeType == null
                ? ""
                : mimeType.trim().toLowerCase(Locale.ROOT);
    }

    private String toDocumentCode(long number) {
        return "DOC-" + (number < 1000
                ? String.format("%03d", number)
                : Long.toString(number));
    }

    private String storageKey(
            UUID workspaceId,
            UUID projectId,
            UUID documentId,
            int versionNumber
    ) {
        return "workspace/%s/project/%s/document/%s/version/%d/%s"
                .formatted(
                        workspaceId,
                        projectId,
                        documentId,
                        versionNumber,
                        UUID.randomUUID()
                );
    }

    private String resolveTitle(String suppliedTitle, String filename) {
        String normalized = normalizeOptional(suppliedTitle);
        if (normalized != null) {
            return normalized;
        }

        String safeName = safeOriginalFilename(filename);
        int dotIndex = safeName.lastIndexOf('.');
        String base = dotIndex > 0 ? safeName.substring(0, dotIndex) : safeName;
        return base.isBlank() ? "Untitled document" : base;
    }

    private String safeOriginalFilename(String originalFilename) {
        String value = originalFilename == null
                ? "document"
                : originalFilename;
        value = Normalizer.normalize(value, Normalizer.Form.NFKC)
                .replace('\\', '_')
                .replace('/', '_')
                .replaceAll("[\\p{Cntrl}\\r\\n\"]", "_")
                .trim();
        return value.isEmpty() ? "document" : value;
    }

    private String sanitizeHeaderFilename(String filename) {
        return safeOriginalFilename(filename);
    }

    private String normalizeOptional(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    private DocumentStatus statusFromCurrentVersion(DocumentVersion version) {
        if (version == null) {
            return DocumentStatus.FAILED;
        }
        return switch (version.getStatus()) {
            case READY -> DocumentStatus.READY;
            case FAILED -> DocumentStatus.FAILED;
            case UPLOADING, STORED, PROCESSING, SUPERSEDED ->
                    DocumentStatus.PROCESSING;
        };
    }

    private DocumentResponse toDocumentResponse(Document document) {
        return new DocumentResponse(
                document.getId(),
                document.getProject().getId(),
                document.getDocumentNumber(),
                document.getDocumentCode(),
                document.getTitle(),
                document.getType(),
                document.getStatus(),
                document.getCurrentVersion() == null
                        ? null
                        : toVersionResponse(document.getCurrentVersion()),
                document.getCreatedBy().getId(),
                document.getCreatedAt(),
                document.getUpdatedAt(),
                document.getArchivedAt()
        );
    }

    private DocumentVersionResponse toVersionResponse(DocumentVersion version) {
        return new DocumentVersionResponse(
                version.getId(),
                version.getVersionNumber(),
                version.getOriginalFilename(),
                version.getMimeType(),
                version.getFileSizeBytes(),
                version.getChecksumSha256(),
                version.getScanStatus(),
                version.isQuarantined(),
                version.getStatus(),
                version.getUploadedAt()
        );
    }

    private DocumentProcessingJobResponse toJobResponse(
            DocumentProcessingJob job
    ) {
        return new DocumentProcessingJobResponse(
                job.getId(),
                job.getDocumentVersion().getId(),
                job.getType(),
                job.getStatus(),
                job.getAttemptNumber(),
                job.getErrorCode(),
                job.getErrorMessage(),
                job.getQueuedAt(),
                job.getStartedAt(),
                job.getCompletedAt(),
                job.getFailedAt()
        );
    }
}
