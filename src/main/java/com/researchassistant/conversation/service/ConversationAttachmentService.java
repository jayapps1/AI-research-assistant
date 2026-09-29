package com.researchassistant.conversation.service;

import com.researchassistant.common.exception.ResourceNotFoundException;
import com.researchassistant.common.storage.ObjectStorageService;
import com.researchassistant.common.storage.StorageObject;
import com.researchassistant.common.storage.StorageObjectCategory;
import com.researchassistant.common.storage.StorageObjectEntity;
import com.researchassistant.common.storage.StorageObjectMetadataService;
import com.researchassistant.common.storage.StoredObject;
import com.researchassistant.conversation.dto.ConversationAttachmentResponse;
import com.researchassistant.conversation.entity.Conversation;
import com.researchassistant.conversation.entity.ConversationAttachment;
import com.researchassistant.conversation.entity.ConversationAttachmentStatus;
import com.researchassistant.conversation.entity.ConversationStatus;
import com.researchassistant.conversation.repository.ConversationAttachmentRepository;
import com.researchassistant.conversation.repository.ConversationRepository;
import com.researchassistant.document.config.DocumentProperties;
import com.researchassistant.document.exception.DocumentUploadException;
import com.researchassistant.identity.entity.User;
import com.researchassistant.project.entity.ResearchProject;
import com.researchassistant.project.service.ProjectAuthorizationService;

import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.text.Normalizer;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
public class ConversationAttachmentService {

    private final ConversationRepository conversationRepository;
    private final ConversationAttachmentRepository attachmentRepository;
    private final ObjectStorageService objectStorageService;
    private final StorageObjectMetadataService storageObjectMetadataService;
    private final ProjectAuthorizationService projectAuthorizationService;
    private final DocumentProperties documentProperties;

    public ConversationAttachmentService(
            ConversationRepository conversationRepository,
            ConversationAttachmentRepository attachmentRepository,
            ObjectStorageService objectStorageService,
            StorageObjectMetadataService storageObjectMetadataService,
            ProjectAuthorizationService projectAuthorizationService,
            DocumentProperties documentProperties
    ) {
        this.conversationRepository = conversationRepository;
        this.attachmentRepository = attachmentRepository;
        this.objectStorageService = objectStorageService;
        this.storageObjectMetadataService = storageObjectMetadataService;
        this.projectAuthorizationService = projectAuthorizationService;
        this.documentProperties = documentProperties;
    }

    @Transactional
    public ConversationAttachmentResponse upload(UUID conversationId, User user, MultipartFile file) {
        Conversation conversation = requireWritableConversation(conversationId, user);
        validateFile(file);
        try {
            byte[] bytes = file.getBytes();
            String originalFilename = safeOriginalFilename(file.getOriginalFilename());
            String mediaType = normalizedMime(file.getContentType());
            String storedFilename = UUID.randomUUID() + extension(originalFilename);
            String storageKey = storageKey(conversation, user, storedFilename);
            UUID workspaceId = conversation.getWorkspace() == null ? null : conversation.getWorkspace().getId();
            UUID projectId = conversation.getProject() == null ? null : conversation.getProject().getId();

            StorageObjectEntity pendingStorageObject = storageObjectMetadataService.createPending(
                    user.getId(),
                    workspaceId,
                    projectId,
                    StorageObjectCategory.CONVERSATION_ATTACHMENT,
                    storageKey,
                    originalFilename,
                    storedFilename,
                    mediaType,
                    bytes.length,
                    null
            );

            StoredObject stored;
            StorageObjectEntity availableStorageObject;
            try {
                stored = objectStorageService.store(storageKey, new ByteArrayInputStream(bytes), mediaType);
                availableStorageObject = storageObjectMetadataService.markAvailable(pendingStorageObject.getId(), stored);
            } catch (RuntimeException exception) {
                storageObjectMetadataService.markFailed(
                        pendingStorageObject.getId(),
                        storageFailureCode(exception),
                        exception.getMessage()
                );
                throw exception;
            }

            ConversationAttachment attachment = new ConversationAttachment();
            attachment.setConversation(conversation);
            attachment.setStorageObject(availableStorageObject);
            attachment.setUploadedBy(user);
            attachment.setOriginalFilename(originalFilename);
            attachment.setMediaType(mediaType);
            attachment.setSizeBytes(stored.sizeBytes());
            attachment.setChecksumSha256(stored.checksumSha256());
            attachment.setStatus(ConversationAttachmentStatus.ACTIVE);
            return ConversationAttachmentResponse.from(attachmentRepository.save(attachment));
        } catch (IOException exception) {
            throw new DocumentUploadException("Unable to read uploaded attachment.");
        }
    }

    @Transactional(readOnly = true)
    public List<ConversationAttachmentResponse> list(UUID conversationId, User user) {
        requireReadableConversation(conversationId, user);
        return attachmentRepository
                .findAllByConversationIdAndStatusOrderByCreatedAtAsc(
                        conversationId,
                        ConversationAttachmentStatus.ACTIVE
                )
                .stream()
                .map(ConversationAttachmentResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public ResponseEntity<org.springframework.core.io.Resource> download(
            UUID conversationId,
            UUID attachmentId,
            User user
    ) {
        requireReadableConversation(conversationId, user);
        ConversationAttachment attachment = attachmentRepository
                .findByIdAndConversationId(attachmentId, conversationId)
                .orElseThrow(() -> new ResourceNotFoundException("Conversation attachment not found."));
        if (attachment.getStatus() != ConversationAttachmentStatus.ACTIVE) {
            throw new ResourceNotFoundException("Conversation attachment not found.");
        }

        StorageObject storageObject = objectStorageService.open(attachment.getStorageObject().getStorageKey());
        org.springframework.core.io.InputStreamResource resource =
                new org.springframework.core.io.InputStreamResource(storageObject.inputStream());

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(attachment.getMediaType()))
                .contentLength(storageObject.sizeBytes())
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment()
                                .filename(safeOriginalFilename(attachment.getOriginalFilename()))
                                .build()
                                .toString()
                )
                .body(resource);
    }

    @Transactional
    public void associateConversationWithProject(Conversation conversation, ResearchProject project) {
        UUID workspaceId = project.getWorkspace().getId();
        UUID projectId = project.getId();
        for (ConversationAttachment attachment : attachmentRepository.findAllByConversationId(conversation.getId())) {
            StorageObjectEntity storageObject = attachment.getStorageObject();
            if (storageObject != null) {
                storageObject.setWorkspaceId(workspaceId);
                storageObject.setProjectId(projectId);
            }
        }
    }

    private Conversation requireReadableConversation(UUID conversationId, User user) {
        Conversation conversation = conversationRepository.findByIdAndUserId(conversationId, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Conversation not found."));
        if (conversation.getProject() != null) {
            projectAuthorizationService.requireProjectViewer(conversation.getProject().getId(), user);
        }
        return conversation;
    }

    private Conversation requireWritableConversation(UUID conversationId, User user) {
        Conversation conversation = requireReadableConversation(conversationId, user);
        if (conversation.getStatus() != ConversationStatus.ACTIVE) {
            throw new IllegalStateException("Restore this conversation before adding attachments.");
        }
        return conversation;
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new DocumentUploadException("Uploaded attachment is required.");
        }
        if (file.getSize() > documentProperties.maxUploadSizeBytes()) {
            throw new DocumentUploadException("DOCUMENT_FILE_TOO_LARGE", "The maximum file size is 50 MB per file.");
        }
    }

    private String storageKey(Conversation conversation, User user, String storedFilename) {
        if (conversation.getProject() != null) {
            return "projects/%s/attachments/conversations/%s/%s"
                    .formatted(conversation.getProject().getId(), conversation.getId(), storedFilename);
        }
        return "users/%s/attachments/conversations/%s/%s"
                .formatted(user.getId(), conversation.getId(), storedFilename);
    }

    private String normalizedMime(String mimeType) {
        return mimeType == null || mimeType.isBlank()
                ? MediaType.APPLICATION_OCTET_STREAM_VALUE
                : mimeType.trim().toLowerCase(Locale.ROOT);
    }

    private String safeOriginalFilename(String originalFilename) {
        String value = originalFilename == null ? "attachment" : originalFilename;
        value = Normalizer.normalize(value, Normalizer.Form.NFKC)
                .replace('\\', '_')
                .replace('/', '_')
                .replaceAll("[\\p{Cntrl}\\r\\n\"]", "_")
                .trim();
        return value.isEmpty() ? "attachment" : value;
    }

    private String extension(String filename) {
        int dot = filename.lastIndexOf('.');
        if (dot < 0 || dot == filename.length() - 1) {
            return "";
        }
        String value = filename.substring(dot).toLowerCase(Locale.ROOT);
        return value.length() <= 20 && value.matches("\\.[a-z0-9]+") ? value : "";
    }

    private String storageFailureCode(RuntimeException exception) {
        if (exception instanceof com.researchassistant.common.storage.StorageException storageException) {
            return storageException.getErrorCode();
        }
        return exception.getClass().getSimpleName();
    }
}
