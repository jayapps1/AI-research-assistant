package com.researchassistant.conversation.controller;

import com.researchassistant.conversation.dto.ConversationAttachmentResponse;
import com.researchassistant.conversation.service.ConversationAttachmentService;
import com.researchassistant.identity.entity.User;
import com.researchassistant.identity.service.AuthenticatedUserResolver;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/conversations/{conversationId}/attachments")
public class ConversationAttachmentController {

    private final AuthenticatedUserResolver userResolver;
    private final ConversationAttachmentService attachmentService;

    public ConversationAttachmentController(
            AuthenticatedUserResolver userResolver,
            ConversationAttachmentService attachmentService
    ) {
        this.userResolver = userResolver;
        this.attachmentService = attachmentService;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ConversationAttachmentResponse upload(
            Authentication authentication,
            @PathVariable UUID conversationId,
            @RequestPart("file") MultipartFile file
    ) {
        User user = userResolver.requireActiveUser(authentication);
        return attachmentService.upload(conversationId, user, file);
    }

    @GetMapping
    public List<ConversationAttachmentResponse> list(
            Authentication authentication,
            @PathVariable UUID conversationId
    ) {
        User user = userResolver.requireActiveUser(authentication);
        return attachmentService.list(conversationId, user);
    }

    @GetMapping("/{attachmentId}/download")
    public ResponseEntity<org.springframework.core.io.Resource> download(
            Authentication authentication,
            @PathVariable UUID conversationId,
            @PathVariable UUID attachmentId
    ) {
        User user = userResolver.requireActiveUser(authentication);
        return attachmentService.download(conversationId, attachmentId, user);
    }
}
