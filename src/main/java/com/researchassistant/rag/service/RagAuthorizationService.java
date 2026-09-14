package com.researchassistant.rag.service;

import com.researchassistant.identity.entity.User;
import com.researchassistant.project.service.ProjectAuthorizationService;
import com.researchassistant.rag.entity.RagConversation;
import com.researchassistant.rag.entity.RagQuery;
import com.researchassistant.rag.exception.RagAccessDeniedException;
import com.researchassistant.rag.repository.RagConversationRepository;
import com.researchassistant.rag.repository.RagQueryRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class RagAuthorizationService {

    private final RagConversationRepository conversationRepository;
    private final RagQueryRepository queryRepository;
    private final ProjectAuthorizationService projectAuthorizationService;

    public RagAuthorizationService(
            RagConversationRepository conversationRepository,
            RagQueryRepository queryRepository,
            ProjectAuthorizationService projectAuthorizationService
    ) {
        this.conversationRepository = conversationRepository;
        this.queryRepository = queryRepository;
        this.projectAuthorizationService = projectAuthorizationService;
    }

    public RagConversation requireOwnConversation(UUID conversationId, User user) {
        RagConversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new RagAccessDeniedException("RAG conversation not found."));
        projectAuthorizationService.requireProjectViewer(
                conversation.getProject().getId(),
                user
        );
        if (!conversation.getCreatedBy().getId().equals(user.getId())) {
            throw new RagAccessDeniedException("RAG conversation not found.");
        }
        return conversation;
    }

    public RagQuery requireOwnQuery(UUID queryId, User user) {
        RagQuery query = queryRepository.findWithConversationById(queryId)
                .orElseThrow(() -> new RagAccessDeniedException("RAG query not found."));
        projectAuthorizationService.requireProjectViewer(
                query.getConversation().getProject().getId(),
                user
        );
        if (!query.getConversation().getCreatedBy().getId().equals(user.getId())) {
            throw new RagAccessDeniedException("RAG query not found.");
        }
        return query;
    }
}
