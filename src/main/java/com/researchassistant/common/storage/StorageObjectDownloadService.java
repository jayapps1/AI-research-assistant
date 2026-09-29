package com.researchassistant.common.storage;

import com.researchassistant.common.exception.ResourceNotFoundException;
import com.researchassistant.identity.entity.User;
import com.researchassistant.project.service.ProjectAuthorizationService;
import com.researchassistant.workspace.service.WorkspaceAuthorizationService;

import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.util.UUID;

@Service
public class StorageObjectDownloadService {

    private final StorageObjectRepository repository;
    private final ObjectStorageService objectStorageService;
    private final ProjectAuthorizationService projectAuthorizationService;
    private final WorkspaceAuthorizationService workspaceAuthorizationService;

    public StorageObjectDownloadService(
            StorageObjectRepository repository,
            ObjectStorageService objectStorageService,
            ProjectAuthorizationService projectAuthorizationService,
            WorkspaceAuthorizationService workspaceAuthorizationService
    ) {
        this.repository = repository;
        this.objectStorageService = objectStorageService;
        this.projectAuthorizationService = projectAuthorizationService;
        this.workspaceAuthorizationService = workspaceAuthorizationService;
    }

    @Transactional(readOnly = true)
    public ResponseEntity<org.springframework.core.io.Resource> download(
            UUID storageObjectId,
            User user
    ) {
        StorageObjectEntity object = repository.findById(storageObjectId)
                .orElseThrow(() -> new ResourceNotFoundException("Storage object not found."));
        authorizeRead(object, user);
        if (object.getStatus() != StorageObjectStatus.AVAILABLE) {
            throw new ResourceNotFoundException("Storage object not found.");
        }

        StorageObject storedObject = objectStorageService.open(object.getStorageKey());
        org.springframework.core.io.InputStreamResource resource =
                new org.springframework.core.io.InputStreamResource(storedObject.inputStream());

        String mediaType = object.getMediaType() == null || object.getMediaType().isBlank()
                ? MediaType.APPLICATION_OCTET_STREAM_VALUE
                : object.getMediaType();

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(mediaType))
                .contentLength(storedObject.sizeBytes())
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment()
                                .filename(sanitizeHeaderFilename(object.getOriginalFilename()))
                                .build()
                                .toString()
                )
                .body(resource);
    }

    private void authorizeRead(StorageObjectEntity object, User user) {
        if (object.getProjectId() != null) {
            projectAuthorizationService.requireProjectViewer(object.getProjectId(), user);
            return;
        }
        if (object.getWorkspaceId() != null) {
            workspaceAuthorizationService.requireActiveMembership(object.getWorkspaceId(), user);
            return;
        }
        if (!object.getOwnerUserId().equals(user.getId())) {
            throw new ResourceNotFoundException("Storage object not found.");
        }
    }

    private String sanitizeHeaderFilename(String filename) {
        String value = filename == null ? "download" : filename;
        value = Normalizer.normalize(value, Normalizer.Form.NFKC)
                .replace('\\', '_')
                .replace('/', '_')
                .replaceAll("[\\p{Cntrl}\\r\\n\"]", "_")
                .trim();
        return value.isEmpty() ? "download" : value;
    }
}
