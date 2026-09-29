package com.researchassistant.conversation.controller;

import com.researchassistant.conversation.dto.ConversationDetailResponse;
import com.researchassistant.conversation.dto.ConversationSummaryResponse;
import com.researchassistant.conversation.dto.CreateConversationRequest;
import com.researchassistant.conversation.dto.MoveConversationToProjectRequest;
import com.researchassistant.conversation.dto.SubmitConversationMessageRequest;
import com.researchassistant.conversation.dto.SubmitConversationResponse;
import com.researchassistant.conversation.dto.UpdateConversationRequest;
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
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/conversations")
public class ConversationController {

    private final AuthenticatedUserResolver authenticatedUserResolver;
    private final ConversationService conversationService;

    public ConversationController(
            AuthenticatedUserResolver authenticatedUserResolver,
            ConversationService conversationService
    ) {
        this.authenticatedUserResolver = authenticatedUserResolver;
        this.conversationService = conversationService;
    }

    @GetMapping
    public Page<ConversationSummaryResponse> list(
            Authentication authentication,
            @RequestParam(defaultValue = "ACTIVE") ConversationStatus status,
            @RequestParam(name = "q", required = false) String query,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        User user = authenticatedUserResolver.requireActiveUser(authentication);
        return conversationService.list(user, status, query, pageable);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SubmitConversationResponse create(
            Authentication authentication,
            @Valid @RequestBody CreateConversationRequest request
    ) {
        User user = authenticatedUserResolver.requireActiveUser(authentication);
        return conversationService.createAndSubmit(user, request.content());
    }

    @GetMapping("/{conversationId}")
    public ConversationDetailResponse detail(
            Authentication authentication,
            @PathVariable UUID conversationId
    ) {
        User user = authenticatedUserResolver.requireActiveUser(authentication);
        return conversationService.detail(conversationId, user);
    }

    @PostMapping("/{conversationId}/messages")
    public SubmitConversationResponse submitMessage(
            Authentication authentication,
            @PathVariable UUID conversationId,
            @Valid @RequestBody SubmitConversationMessageRequest request
    ) {
        User user = authenticatedUserResolver.requireActiveUser(authentication);
        return conversationService.submit(
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
            @PathVariable UUID conversationId,
            @PathVariable UUID messageId
    ) {
        User user = authenticatedUserResolver.requireActiveUser(authentication);
        return conversationService.retry(conversationId, messageId, user);
    }

    @PatchMapping("/{conversationId}")
    public ConversationSummaryResponse rename(
            Authentication authentication,
            @PathVariable UUID conversationId,
            @Valid @RequestBody UpdateConversationRequest request
    ) {
        User user = authenticatedUserResolver.requireActiveUser(authentication);
        return conversationService.rename(conversationId, user, request.title());
    }

    @PostMapping("/{conversationId}/archive")
    public ConversationSummaryResponse archive(
            Authentication authentication,
            @PathVariable UUID conversationId
    ) {
        User user = authenticatedUserResolver.requireActiveUser(authentication);
        return conversationService.archive(conversationId, user);
    }

    @PostMapping("/{conversationId}/restore")
    public ConversationSummaryResponse restore(
            Authentication authentication,
            @PathVariable UUID conversationId
    ) {
        User user = authenticatedUserResolver.requireActiveUser(authentication);
        return conversationService.restore(conversationId, user);
    }

    @PostMapping("/{conversationId}/trash")
    public ConversationSummaryResponse trash(
            Authentication authentication,
            @PathVariable UUID conversationId
    ) {
        User user = authenticatedUserResolver.requireActiveUser(authentication);
        return conversationService.trash(conversationId, user);
    }

    @PostMapping("/{conversationId}/project")
    public ConversationSummaryResponse moveToProject(
            Authentication authentication,
            @PathVariable UUID conversationId,
            @Valid @RequestBody MoveConversationToProjectRequest request
    ) {
        User user = authenticatedUserResolver.requireActiveUser(authentication);
        return conversationService.moveToProject(conversationId, user, request.projectId());
    }
}
