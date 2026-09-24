package com.researchassistant.rag.service;

import com.researchassistant.identity.entity.User;
import com.researchassistant.project.service.ProjectAuthorizationContext;
import com.researchassistant.project.service.ProjectAuthorizationService;
import com.researchassistant.rag.dto.request.CreateRagConversationRequest;
import com.researchassistant.rag.dto.request.UpdateRagConversationRequest;
import com.researchassistant.rag.dto.response.RagConversationDetailResponse;
import com.researchassistant.rag.dto.response.RagConversationResponse;
import com.researchassistant.rag.entity.RagConversation;
import com.researchassistant.rag.entity.RagConversationStatus;
import com.researchassistant.rag.repository.RagConversationRepository;
import com.researchassistant.rag.repository.RagQueryRepository;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.time.OffsetDateTime;

@Service
public class RagConversationService {

    private final ProjectAuthorizationService projectAuthorizationService;
    private final RagAuthorizationService authorizationService;
    private final RagConversationRepository conversationRepository;
    private final RagQueryRepository queryRepository;
    private final RagResponseMapper mapper;

    public RagConversationService(
            ProjectAuthorizationService projectAuthorizationService,
            RagAuthorizationService authorizationService,
            RagConversationRepository conversationRepository,
            RagQueryRepository queryRepository,
            RagResponseMapper mapper
    ) {
        this.projectAuthorizationService = projectAuthorizationService;
        this.authorizationService = authorizationService;
        this.conversationRepository = conversationRepository;
        this.queryRepository = queryRepository;
        this.mapper = mapper;
    }

    @Transactional
    public RagConversationResponse create(
            UUID projectId,
            User user,
            CreateRagConversationRequest request
    ) {
        ProjectAuthorizationContext context =
                projectAuthorizationService.requireProjectViewer(projectId, user);
        RagConversation conversation = new RagConversation();
        conversation.setProject(context.project());
        conversation.setCreatedBy(user);
        conversation.setTitle(cleanTitle(request == null ? null : request.title()));
        conversation.setStatus(RagConversationStatus.ACTIVE);
        return mapper.conversation(conversationRepository.save(conversation));
    }

    @Transactional(readOnly = true)
    public List<RagConversationResponse> list(UUID projectId, User user) {
        projectAuthorizationService.requireProjectViewer(projectId, user);
        return conversationRepository
                .findAllByProjectIdAndCreatedByIdAndStatusOrderByUpdatedAtDesc(
                        projectId,
                        user.getId(),
                        RagConversationStatus.ACTIVE
                )
                .stream()
                .map(mapper::conversation)
                .toList();
    }

    @Transactional(readOnly = true)
    public RagConversationDetailResponse detail(UUID conversationId, User user) {
        RagConversation conversation =
                authorizationService.requireOwnConversation(conversationId, user);
        return new RagConversationDetailResponse(
                mapper.conversation(conversation),
                queryRepository.findAllByConversationIdOrderByCreatedAtAsc(
                                conversationId,
                                PageRequest.of(0, 100)
                        )
                        .map(mapper::querySummary)
                        .toList()
        );
    }

    @Transactional
    public RagConversationResponse update(UUID conversationId, User user, UpdateRagConversationRequest request) {
        RagConversation conversation = authorizationService.requireOwnConversation(conversationId, user);
        if (request != null && request.title() != null) {
            conversation.setTitle(cleanTitle(request.title()));
        }
        return mapper.conversation(conversation);
    }

    @Transactional
    public RagConversationResponse archive(UUID conversationId, User user) {
        RagConversation conversation = authorizationService.requireOwnConversation(conversationId, user);
        conversation.setStatus(RagConversationStatus.ARCHIVED);
        conversation.setArchivedAt(OffsetDateTime.now());
        return mapper.conversation(conversation);
    }

    @Transactional
    public void delete(UUID conversationId, User user) {
        RagConversation conversation = authorizationService.requireOwnConversation(conversationId, user);
        conversation.setStatus(RagConversationStatus.ARCHIVED);
        conversation.setArchivedAt(OffsetDateTime.now());
    }

    private String cleanTitle(String title) {
        if (title == null || title.isBlank()) {
            return null;
        }
        String trimmed = title.trim();
        return trimmed.length() <= 255 ? trimmed : trimmed.substring(0, 255);
    }
}
