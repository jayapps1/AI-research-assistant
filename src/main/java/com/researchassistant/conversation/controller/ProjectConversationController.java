package com.researchassistant.conversation.controller;

import com.researchassistant.conversation.dto.ConversationSummaryResponse;
import com.researchassistant.conversation.dto.ConversationDetailResponse;
import com.researchassistant.conversation.dto.CreateConversationRequest;
import com.researchassistant.conversation.dto.SubmitConversationMessageRequest;
import com.researchassistant.conversation.dto.SubmitConversationResponse;
import com.researchassistant.conversation.entity.ConversationStatus;
import com.researchassistant.conversation.service.ConversationService;
import com.researchassistant.identity.entity.User;
import com.researchassistant.identity.service.AuthenticatedUserResolver;

import jakarta.validation.Valid;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/projects/{projectId}/conversations")
public class ProjectConversationController {

    private final AuthenticatedUserResolver authenticatedUserResolver;
    private final ConversationService conversationService;

    public ProjectConversationController(
            AuthenticatedUserResolver authenticatedUserResolver,
            ConversationService conversationService
    ) {
        this.authenticatedUserResolver = authenticatedUserResolver;
        this.conversationService = conversationService;
    }

    @GetMapping
    public Page<ConversationSummaryResponse> list(
            Authentication authentication,
            @PathVariable UUID projectId,
            @RequestParam(defaultValue = "ACTIVE") ConversationStatus status,
            @RequestParam(name = "q", required = false) String query,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        User user = authenticatedUserResolver.requireActiveUser(authentication);
        return conversationService.listProject(projectId, user, status, query, pageable);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SubmitConversationResponse create(
            Authentication authentication,
            @PathVariable UUID projectId,
            @Valid @RequestBody CreateConversationRequest request
    ) {
        User user = authenticatedUserResolver.requireActiveUser(authentication);
        return conversationService.createAndSubmitProject(
                projectId,
                user,
                request.content(),
                request.scopeType(),
                request.documentIds(),
                request.evidenceLimit()
        );
    }

    @GetMapping("/{conversationId}")
    public ConversationDetailResponse detail(
            Authentication authentication,
            @PathVariable UUID projectId,
            @PathVariable UUID conversationId
    ) {
        User user = authenticatedUserResolver.requireActiveUser(authentication);
        return conversationService.detailProject(projectId, conversationId, user);
    }

    @PostMapping("/{conversationId}/messages")
    public SubmitConversationResponse submitMessage(
            Authentication authentication,
            @PathVariable UUID projectId,
            @PathVariable UUID conversationId,
            @Valid @RequestBody SubmitConversationMessageRequest request
    ) {
        User user = authenticatedUserResolver.requireActiveUser(authentication);
        return conversationService.submitProject(
                projectId,
                conversationId,
                user,
                request.content(),
                request.scopeType(),
                request.documentIds(),
                request.evidenceLimit()
        );
    }

    @PostMapping("/{conversationId}/messages/{messageId}/retry")
    public SubmitConversationResponse retryMessage(
            Authentication authentication,
            @PathVariable UUID projectId,
            @PathVariable UUID conversationId,
            @PathVariable UUID messageId
    ) {
        User user = authenticatedUserResolver.requireActiveUser(authentication);
        return conversationService.retryProject(projectId, conversationId, messageId, user);
    }
}
