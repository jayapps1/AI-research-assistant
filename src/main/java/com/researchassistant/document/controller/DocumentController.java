package com.researchassistant.document.controller;

import com.researchassistant.document.dto.DocumentProcessingJobResponse;
import com.researchassistant.document.dto.DocumentResponse;
import com.researchassistant.document.dto.DocumentVersionResponse;
import com.researchassistant.document.dto.PageResponse;
import com.researchassistant.document.dto.UpdateDocumentMetadataRequest;
import com.researchassistant.document.entity.DocumentStatus;
import com.researchassistant.document.entity.DocumentType;
import com.researchassistant.document.service.DocumentService;
import com.researchassistant.identity.entity.User;
import com.researchassistant.identity.service.AuthenticatedUserResolver;

import jakarta.validation.Valid;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
public class DocumentController {

    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int MAX_PAGE_SIZE = 100;

    private final DocumentService documentService;
    private final AuthenticatedUserResolver authenticatedUserResolver;

    public DocumentController(
            DocumentService documentService,
            AuthenticatedUserResolver authenticatedUserResolver
    ) {
        this.documentService = documentService;
        this.authenticatedUserResolver = authenticatedUserResolver;
    }

    @PostMapping(
            value = "/projects/{projectId}/documents",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    @ResponseStatus(HttpStatus.CREATED)
    public DocumentResponse uploadDocument(
            Authentication authentication,
            @PathVariable UUID projectId,
            @RequestPart("file") MultipartFile file,
            @RequestParam(required = false) String title
    ) {
        User user = authenticatedUserResolver.requireActiveUser(authentication);
        return documentService.uploadDocument(projectId, user, file, title);
    }

    @GetMapping("/projects/{projectId}/documents")
    public PageResponse<DocumentResponse> listDocuments(
            Authentication authentication,
            @PathVariable UUID projectId,
            @RequestParam(required = false) DocumentStatus status,
            @RequestParam(required = false) DocumentType type,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        User user = authenticatedUserResolver.requireActiveUser(authentication);
        return PageResponse.from(documentService.listDocuments(
                projectId,
                user,
                status,
                type,
                pageable(page, size)
        ));
    }

    @GetMapping("/documents/mine")
    public PageResponse<DocumentResponse> listMyDocuments(
            Authentication authentication,
            @RequestParam(required = false) DocumentStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        User user = authenticatedUserResolver.requireActiveUser(authentication);
        return PageResponse.from(documentService.listAllMyDocuments(
                user,
                status,
                pageable(page, size)
        ));
    }

    @GetMapping("/documents/{documentId}")
    public DocumentResponse getDocument(
            Authentication authentication,
            @PathVariable UUID documentId
    ) {
        User user = authenticatedUserResolver.requireActiveUser(authentication);
        return documentService.getDocument(documentId, user);
    }

    @PatchMapping("/documents/{documentId}")
    public DocumentResponse updateDocumentMetadata(
            Authentication authentication,
            @PathVariable UUID documentId,
            @Valid @RequestBody UpdateDocumentMetadataRequest request
    ) {
        User user = authenticatedUserResolver.requireActiveUser(authentication);
        return documentService.updateMetadata(documentId, user, request);
    }

    @PostMapping(
            value = "/documents/{documentId}/versions",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    @ResponseStatus(HttpStatus.CREATED)
    public DocumentResponse uploadVersion(
            Authentication authentication,
            @PathVariable UUID documentId,
            @RequestPart("file") MultipartFile file
    ) {
        User user = authenticatedUserResolver.requireActiveUser(authentication);
        return documentService.uploadVersion(documentId, user, file);
    }

    @GetMapping("/documents/{documentId}/versions")
    public List<DocumentVersionResponse> listVersions(
            Authentication authentication,
            @PathVariable UUID documentId
    ) {
        User user = authenticatedUserResolver.requireActiveUser(authentication);
        return documentService.listVersions(documentId, user);
    }

    @PostMapping("/documents/{documentId}/archive")
    public DocumentResponse archiveDocument(
            Authentication authentication,
            @PathVariable UUID documentId
    ) {
        User user = authenticatedUserResolver.requireActiveUser(authentication);
        return documentService.archiveDocument(documentId, user);
    }

    @DeleteMapping("/documents/{documentId}")
    public DocumentResponse moveDocumentToTrash(
            Authentication authentication,
            @PathVariable UUID documentId
    ) {
        User user = authenticatedUserResolver.requireActiveUser(authentication);
        return documentService.archiveDocument(documentId, user);
    }

    @DeleteMapping("/documents/{documentId}/permanent")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void permanentlyDeleteDocument(
            Authentication authentication,
            @PathVariable UUID documentId
    ) {
        User user = authenticatedUserResolver.requireActiveUser(authentication);
        documentService.permanentlyDeleteDocument(documentId, user);
    }

    @PostMapping("/documents/{documentId}/restore")
    public DocumentResponse restoreDocument(
            Authentication authentication,
            @PathVariable UUID documentId
    ) {
        User user = authenticatedUserResolver.requireActiveUser(authentication);
        return documentService.restoreDocument(documentId, user);
    }

    @GetMapping("/documents/{documentId}/download")
    public ResponseEntity<org.springframework.core.io.Resource> downloadCurrent(
            Authentication authentication,
            @PathVariable UUID documentId
    ) {
        User user = authenticatedUserResolver.requireActiveUser(authentication);
        return documentService.downloadCurrent(documentId, user);
    }

    @GetMapping("/documents/{documentId}/versions/{versionNumber}/download")
    public ResponseEntity<org.springframework.core.io.Resource> downloadVersion(
            Authentication authentication,
            @PathVariable UUID documentId,
            @PathVariable int versionNumber
    ) {
        User user = authenticatedUserResolver.requireActiveUser(authentication);
        return documentService.downloadVersion(
                documentId,
                versionNumber,
                user
        );
    }

    @GetMapping("/documents/{documentId}/processing")
    public List<DocumentProcessingJobResponse> listProcessing(
            Authentication authentication,
            @PathVariable UUID documentId
    ) {
        User user = authenticatedUserResolver.requireActiveUser(authentication);
        return documentService.listProcessingJobs(documentId, user);
    }

    @PostMapping("/documents/{documentId}/processing/retry")
    public DocumentProcessingJobResponse retryProcessing(
            Authentication authentication,
            @PathVariable UUID documentId
    ) {
        User user = authenticatedUserResolver.requireActiveUser(authentication);
        return documentService.retryProcessing(documentId, user);
    }

    @PostMapping("/documents/{documentId}/processing/reprocess")
    public DocumentResponse reprocessCurrentVersion(
            Authentication authentication,
            @PathVariable UUID documentId
    ) {
        User user = authenticatedUserResolver.requireActiveUser(authentication);
        return documentService.reprocessCurrentVersion(documentId, user);
    }

    private Pageable pageable(int page, int size) {
        int safePage = Math.max(0, page);
        int safeSize = size <= 0
                ? DEFAULT_PAGE_SIZE
                : Math.min(size, MAX_PAGE_SIZE);
        return PageRequest.of(safePage, safeSize);
    }
}
