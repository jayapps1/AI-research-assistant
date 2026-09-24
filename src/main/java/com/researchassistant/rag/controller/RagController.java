package com.researchassistant.rag.controller;

import com.researchassistant.identity.entity.User;
import com.researchassistant.identity.service.AuthenticatedUserResolver;
import com.researchassistant.rag.dto.request.CreateRagConversationRequest;
import com.researchassistant.rag.dto.request.SubmitRagQueryRequest;
import com.researchassistant.rag.dto.request.UpdateRagConversationRequest;
import com.researchassistant.rag.dto.response.GroundedAnswerResponse;
import com.researchassistant.rag.dto.response.RagConversationDetailResponse;
import com.researchassistant.rag.dto.response.RagConversationResponse;
import com.researchassistant.rag.dto.response.RagEvidenceResponse;
import com.researchassistant.rag.service.RagConversationService;
import com.researchassistant.rag.service.RagQueryService;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
public class RagController {

    private final AuthenticatedUserResolver authenticatedUserResolver;
    private final RagConversationService conversationService;
    private final RagQueryService queryService;

    public RagController(
            AuthenticatedUserResolver authenticatedUserResolver,
            RagConversationService conversationService,
            RagQueryService queryService
    ) {
        this.authenticatedUserResolver = authenticatedUserResolver;
        this.conversationService = conversationService;
        this.queryService = queryService;
    }

    @PostMapping("/projects/{projectId}/rag/conversations")
    @ResponseStatus(HttpStatus.CREATED)
    public RagConversationResponse createConversation(
            Authentication authentication,
            @PathVariable UUID projectId,
            @Valid @RequestBody(required = false) CreateRagConversationRequest request
    ) {
        User user = authenticatedUserResolver.requireActiveUser(authentication);
        return conversationService.create(projectId, user, request);
    }

    @GetMapping("/projects/{projectId}/rag/conversations")
    public List<RagConversationResponse> listConversations(
            Authentication authentication,
            @PathVariable UUID projectId
    ) {
        User user = authenticatedUserResolver.requireActiveUser(authentication);
        return conversationService.list(projectId, user);
    }

    @GetMapping("/rag/conversations/{conversationId}")
    public RagConversationDetailResponse conversationDetail(
            Authentication authentication,
            @PathVariable UUID conversationId
    ) {
        User user = authenticatedUserResolver.requireActiveUser(authentication);
        return conversationService.detail(conversationId, user);
    }

    @PatchMapping("/rag/conversations/{conversationId}")
    public RagConversationResponse updateConversation(
            Authentication authentication,
            @PathVariable UUID conversationId,
            @Valid @RequestBody UpdateRagConversationRequest request
    ) {
        User user = authenticatedUserResolver.requireActiveUser(authentication);
        return conversationService.update(conversationId, user, request);
    }

    @PostMapping("/rag/conversations/{conversationId}/archive")
    public RagConversationResponse archiveConversation(
            Authentication authentication,
            @PathVariable UUID conversationId
    ) {
        User user = authenticatedUserResolver.requireActiveUser(authentication);
        return conversationService.archive(conversationId, user);
    }

    @DeleteMapping("/rag/conversations/{conversationId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteConversation(
            Authentication authentication,
            @PathVariable UUID conversationId
    ) {
        User user = authenticatedUserResolver.requireActiveUser(authentication);
        conversationService.delete(conversationId, user);
    }

    @PostMapping("/rag/conversations/{conversationId}/queries")
    public GroundedAnswerResponse submitQuery(
            Authentication authentication,
            @PathVariable UUID conversationId,
            @Valid @RequestBody SubmitRagQueryRequest request
    ) {
        User user = authenticatedUserResolver.requireActiveUser(authentication);
        return queryService.submit(conversationId, user, request);
    }

    @GetMapping("/rag/queries/{queryId}")
    public GroundedAnswerResponse queryDetail(
            Authentication authentication,
            @PathVariable UUID queryId
    ) {
        User user = authenticatedUserResolver.requireActiveUser(authentication);
        return queryService.get(queryId, user);
    }

    @GetMapping("/rag/queries/{queryId}/evidence")
    public List<RagEvidenceResponse> queryEvidence(
            Authentication authentication,
            @PathVariable UUID queryId
    ) {
        User user = authenticatedUserResolver.requireActiveUser(authentication);
        return queryService.evidence(queryId, user);
    }
}
