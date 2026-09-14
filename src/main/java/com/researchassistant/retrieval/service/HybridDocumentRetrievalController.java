package com.researchassistant.retrieval.service;

import com.researchassistant.identity.entity.User;
import com.researchassistant.identity.service.AuthenticatedUserResolver;
import com.researchassistant.retrieval.dto.RetrievalSearchRequest;
import com.researchassistant.retrieval.dto.RetrievalSearchResponse;

import jakarta.validation.Valid;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/projects/{projectId}/retrieval")
public class HybridDocumentRetrievalController {

    private final HybridDocumentRetrievalService retrievalService;
    private final RetrievalRequestFactory requestFactory;
    private final AuthenticatedUserResolver authenticatedUserResolver;

    public HybridDocumentRetrievalController(
            HybridDocumentRetrievalService retrievalService,
            RetrievalRequestFactory requestFactory,
            AuthenticatedUserResolver authenticatedUserResolver
    ) {
        this.retrievalService = retrievalService;
        this.requestFactory = requestFactory;
        this.authenticatedUserResolver = authenticatedUserResolver;
    }

    @PostMapping("/search")
    public RetrievalSearchResponse search(
            Authentication authentication,
            @org.springframework.web.bind.annotation.PathVariable UUID projectId,
            @Valid @RequestBody RetrievalSearchRequest request
    ) {
        User user = authenticatedUserResolver.requireActiveUser(authentication);
        return retrievalService.retrieve(
                user,
                requestFactory.fromApi(projectId, request)
        );
    }
}
