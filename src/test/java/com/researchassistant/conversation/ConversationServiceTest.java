package com.researchassistant.conversation;

import com.researchassistant.ai.config.AiProperties;
import com.researchassistant.ai.orchestration.AiTaskResult;
import com.researchassistant.ai.orchestration.AiTaskType;
import com.researchassistant.ai.orchestration.ResearchAiOrchestrator;
import com.researchassistant.ai.provider.AiGenerationProvider;
import com.researchassistant.ai.provider.AiProviderType;
import com.researchassistant.ai.usage.AiRequestRepository;
import com.researchassistant.conversation.dto.ConversationDetailResponse;
import com.researchassistant.conversation.dto.SubmitConversationResponse;
import com.researchassistant.conversation.entity.Conversation;
import com.researchassistant.conversation.entity.ConversationMessage;
import com.researchassistant.conversation.entity.ConversationMessageRole;
import com.researchassistant.conversation.entity.ConversationRun;
import com.researchassistant.conversation.entity.ConversationSource;
import com.researchassistant.conversation.entity.ConversationStatus;
import com.researchassistant.conversation.entity.ConversationType;
import com.researchassistant.conversation.entity.MessageCitation;
import com.researchassistant.conversation.exception.ConversationAccessDeniedException;
import com.researchassistant.conversation.repository.ConversationMessageRepository;
import com.researchassistant.conversation.repository.ConversationRepository;
import com.researchassistant.conversation.repository.ConversationRunRepository;
import com.researchassistant.conversation.repository.ConversationSourceRepository;
import com.researchassistant.conversation.repository.MessageCitationRepository;
import com.researchassistant.conversation.service.ConversationAttachmentService;
import com.researchassistant.conversation.service.ConversationMapper;
import com.researchassistant.conversation.service.ConversationService;
import com.researchassistant.conversation.service.WebSourceProjectSaveService;
import com.researchassistant.document.repository.DocumentRepository;
import com.researchassistant.document.repository.DocumentVersionRepository;
import com.researchassistant.document.exception.InvalidDocumentOperationException;
import com.researchassistant.identity.entity.User;
import com.researchassistant.project.entity.ResearchProject;
import com.researchassistant.project.service.ProjectAuthorizationContext;
import com.researchassistant.project.service.ProjectAuthorizationService;
import com.researchassistant.rag.dto.response.CitationResponse;
import com.researchassistant.rag.dto.response.GroundedAnswerResponse;
import com.researchassistant.rag.dto.response.RetrievalSummaryResponse;
import com.researchassistant.rag.entity.RagQuery;
import com.researchassistant.rag.entity.RagQueryStatus;
import com.researchassistant.rag.repository.AnswerCitationRepository;
import com.researchassistant.rag.repository.GroundedAnswerRepository;
import com.researchassistant.rag.repository.RagQueryDocumentRepository;
import com.researchassistant.rag.repository.RagQueryRepository;
import com.researchassistant.rag.scope.RetrievalScopeType;
import com.researchassistant.rag.service.RagQueryService;
import com.researchassistant.rag.service.RagResponseMapper;
import com.researchassistant.rag.service.RagContextBudgetService;
import com.researchassistant.reference.repository.ProjectReferenceRepository;
import com.researchassistant.reference.repository.ReferenceSourceLinkRepository;
import com.researchassistant.websearch.WebSearchProvider;
import com.researchassistant.workspace.entity.Workspace;
import com.researchassistant.workspace.entity.WorkspaceStatus;
import com.researchassistant.workspace.entity.WorkspaceType;
import com.researchassistant.workspace.service.PersonalWorkspaceService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ConversationServiceTest {

    private final Map<UUID, Conversation> conversations = new HashMap<>();
    private final Map<UUID, ConversationMessage> messages = new HashMap<>();
    private final Map<UUID, ConversationRun> runs = new HashMap<>();
    private final Map<UUID, ConversationSource> sources = new HashMap<>();
    private final Map<UUID, MessageCitation> citations = new HashMap<>();

    private ConversationRepository conversationRepository;
    private ConversationMessageRepository messageRepository;
    private ConversationRunRepository runRepository;
    private ConversationSourceRepository sourceRepository;
    private MessageCitationRepository messageCitationRepository;
    private ResearchAiOrchestrator orchestrator;
    private ProjectAuthorizationService projectAuthorizationService;
    private RagQueryService ragQueryService;
    private RagQueryRepository ragQueryRepository;
    private ConversationService service;
    private User user;
    private User otherUser;
    private Workspace workspace;

    @BeforeEach
    void setUp() {
        conversationRepository = mock(ConversationRepository.class);
        messageRepository = mock(ConversationMessageRepository.class);
        runRepository = mock(ConversationRunRepository.class);
        sourceRepository = mock(ConversationSourceRepository.class);
        messageCitationRepository = mock(MessageCitationRepository.class);
        orchestrator = mock(ResearchAiOrchestrator.class);
        projectAuthorizationService = mock(ProjectAuthorizationService.class);
        ragQueryService = mock(RagQueryService.class);
        ragQueryRepository = mock(RagQueryRepository.class);

        user = user("user@example.com");
        otherUser = user("other@example.com");
        workspace = workspace(user);

        wireConversationRepository();
        wireMessageRepository();
        wireRunRepository();
        wireSourceRepository();
        wireMessageCitationRepository();

        PersonalWorkspaceService personalWorkspaceService = mock(PersonalWorkspaceService.class);
        when(personalWorkspaceService.ensurePersonalWorkspace(user)).thenReturn(workspace);

        AiGenerationProvider generationProvider = mock(AiGenerationProvider.class);
        when(generationProvider.available()).thenReturn(true);
        when(generationProvider.providerName()).thenReturn("OPENAI");
        when(generationProvider.modelName()).thenReturn("gpt-4o-mini");

        RagContextBudgetService budgetService = mock(RagContextBudgetService.class);
        when(budgetService.estimatePromptTokens(any())).thenReturn(100);
        AiRequestRepository aiRequestRepository = mock(AiRequestRepository.class);
        when(aiRequestRepository.findById(any())).thenReturn(Optional.empty());
        when(ragQueryRepository.getReferenceById(any())).thenAnswer(invocation -> {
            RagQuery query = new RagQuery();
            query.setId(invocation.getArgument(0));
            query.setScopeType(RetrievalScopeType.PROJECT_ALL_DOCUMENTS);
            return query;
        });

        service = new ConversationService(
                conversationRepository,
                messageRepository,
                runRepository,
                sourceRepository,
                messageCitationRepository,
                orchestrator,
                personalWorkspaceService,
                projectAuthorizationService,
                ragQueryService,
                ragQueryRepository,
                mock(RagQueryDocumentRepository.class),
                mock(GroundedAnswerRepository.class),
                mock(AnswerCitationRepository.class),
                mock(DocumentRepository.class),
                mock(DocumentVersionRepository.class),
                mock(ReferenceSourceLinkRepository.class),
                mock(ProjectReferenceRepository.class),
                aiRequestRepository,
                aiProperties(),
                budgetService,
                emptyProvider(),
                emptyProvider(),
                provider(generationProvider),
                mock(WebSearchProvider.class),
                mock(WebSourceProjectSaveService.class),
                new com.fasterxml.jackson.databind.ObjectMapper(),
                new ConversationMapper(),
                mock(RagResponseMapper.class),
                mock(ConversationAttachmentService.class)
        );
    }

    @Test
    void createsConversationAndPersistsUserAndAssistantMessages() {
        when(orchestrator.executeTask(eq(user), any(), eq(String.class)))
                .thenReturn(AiTaskResult.success(
                        UUID.randomUUID(),
                        AiTaskType.GENERAL_CONVERSATION,
                        AiProviderType.OPENAI,
                        "gpt-4o-mini",
                        "Stratified sampling divides a population into strata.",
                        120,
                        40,
                        160,
                        500L,
                        OffsetDateTime.now(),
                        OffsetDateTime.now()
                ));

        SubmitConversationResponse response = service.createAndSubmit(user, "What is stratified sampling?");

        assertThat(response.conversation().type()).isEqualTo(ConversationType.GENERAL);
        assertThat(response.conversation().status()).isEqualTo(ConversationStatus.ACTIVE);
        assertThat(response.conversation().title()).isEqualTo("What is stratified sampling");
        assertThat(response.userMessage().content()).isEqualTo("What is stratified sampling?");
        assertThat(response.assistantMessage().content()).contains("divides a population");
        assertThat(savedMessages()).extracting(ConversationMessage::getRole)
                .containsExactly(ConversationMessageRole.USER, ConversationMessageRole.ASSISTANT);
        assertThat(response.run().status().name()).isEqualTo("COMPLETED");
    }

    @Test
    void reloadsConversationWithoutSystemMessagesAndContinuesFollowUp() {
        Conversation conversation = existingConversation(user, "Sampling");
        addMessage(conversation, ConversationMessageRole.USER, "Question", 1);
        addMessage(conversation, ConversationMessageRole.SYSTEM, "Hidden system prompt", 2);
        addMessage(conversation, ConversationMessageRole.ASSISTANT, "Answer", 3);

        ConversationDetailResponse detail = service.detail(conversation.getId(), user);
        assertThat(detail.messages()).extracting(m -> m.content()).containsExactly("Question", "Answer");

        when(orchestrator.executeTask(eq(user), any(), eq(String.class)))
                .thenReturn(AiTaskResult.success(
                        UUID.randomUUID(),
                        AiTaskType.GENERAL_CONVERSATION,
                        AiProviderType.OPENAI,
                        "gpt-4o-mini",
                        "Follow-up answer",
                        50,
                        20,
                        70,
                        100L,
                        OffsetDateTime.now(),
                        OffsetDateTime.now()
                ));

        SubmitConversationResponse followUp = service.submit(conversation.getId(), user, "Follow up?");
        assertThat(followUp.assistantMessage().content()).isEqualTo("Follow-up answer");
        assertThat(followUp.userMessage().sequenceNumber()).isEqualTo(4);
        assertThat(followUp.assistantMessage().sequenceNumber()).isEqualTo(5);
    }

    @Test
    void renamesArchivesRestoresAndTrashesConversation() {
        Conversation conversation = existingConversation(user, "Original");

        assertThat(service.rename(conversation.getId(), user, "New Title").title()).isEqualTo("New Title");
        assertThat(service.archive(conversation.getId(), user).status()).isEqualTo(ConversationStatus.ARCHIVED);
        assertThat(service.restore(conversation.getId(), user).status()).isEqualTo(ConversationStatus.ACTIVE);
        assertThat(service.trash(conversation.getId(), user).status()).isEqualTo(ConversationStatus.TRASHED);
        assertThat(conversation.getDeletedAt()).isNotNull();
    }

    @Test
    void delegatesSearchToPostgresBackedRepositoryWithCurrentUserOnly() {
        when(conversationRepository.searchForUser(
                eq(user.getId()),
                eq(ConversationStatus.ACTIVE),
                eq(ConversationType.GENERAL),
                eq("sampling"),
                eq(ConversationMessageRole.SYSTEM),
                any()
        )).thenReturn(new PageImpl<>(List.of(existingConversation(user, "Sampling")), PageRequest.of(0, 20), 1));

        assertThat(service.list(user, ConversationStatus.ACTIVE, " sampling ", PageRequest.of(0, 20)).getTotalElements()).isEqualTo(1);
    }

    @Test
    void rejectsCrossUserConversationAccess() {
        Conversation conversation = existingConversation(user, "Private");

        assertThatThrownBy(() -> service.detail(conversation.getId(), otherUser))
                .isInstanceOf(ConversationAccessDeniedException.class);
    }

    @Test
    void failedAiGenerationKeepsUserMessageAndCreatesNoAssistantMessage() {
        when(orchestrator.executeTask(eq(user), any(), eq(String.class)))
                .thenReturn(AiTaskResult.failure(
                        UUID.randomUUID(),
                        AiTaskType.GENERAL_CONVERSATION,
                        AiProviderType.OPENAI,
                        "gpt-4o-mini",
                        "AI_PROVIDER_TIMEOUT",
                        "AI_PROVIDER_TIMEOUT",
                        100L,
                        OffsetDateTime.now(),
                        OffsetDateTime.now()
                ));

        SubmitConversationResponse response = service.createAndSubmit(user, "Will this fail?");

        assertThat(response.userMessage().content()).isEqualTo("Will this fail?");
        assertThat(response.assistantMessage()).isNull();
        assertThat(response.run().status().name()).isEqualTo("FAILED");
        assertThat(savedMessages()).extracting(ConversationMessage::getRole).containsExactly(ConversationMessageRole.USER);
    }

    @Test
    void createsProjectConversationThroughAuthorizedProjectRag() {
        ResearchProject project = project(workspace, "Farmer-to-Buyer Agricultural Marketplace");
        UUID queryId = UUID.randomUUID();
        when(projectAuthorizationService.requireProjectViewer(project.getId(), user))
                .thenReturn(new ProjectAuthorizationContext(project, null, Optional.empty()));
        when(ragQueryService.submitProjectConversationTurn(eq(project.getId()), eq(user), any(), any()))
                .thenReturn(new GroundedAnswerResponse(
                        queryId,
                        UUID.randomUUID(),
                        "Uploaded papers point to market-access and trust gaps (Morepje et al., 2024).",
                        RagQueryStatus.COMPLETED,
                        List.of(new CitationResponse(
                                1,
                                UUID.randomUUID(),
                                "DOC-001",
                                "Agricultural Marketplace Paper",
                                UUID.randomUUID(),
                                1,
                                4,
                                2,
                                "Market access remains constrained by trust and infrastructure.",
                                "(Morepje et al., 2024)",
                                true,
                                null
                        )),
                        new RetrievalSummaryResponse(
                                RetrievalScopeType.PROJECT_ALL_DOCUMENTS,
                                3,
                                5,
                                "LEXICAL",
                                null,
                                null,
                                8,
                                1200,
                                1000,
                                120,
                                30000,
                                0,
                                "DIRECT_RAG",
                                "gpt-4o-mini"
                        ),
                        OffsetDateTime.now()
                ));

        SubmitConversationResponse response = service.createAndSubmitProject(
                project.getId(),
                user,
                "What research gaps are supported by my uploaded papers?",
                RetrievalScopeType.PROJECT_ALL_DOCUMENTS,
                null,
                null
        );

        assertThat(response.conversation().type()).isEqualTo(ConversationType.PROJECT);
        assertThat(response.conversation().projectId()).isEqualTo(project.getId());
        assertThat(response.run().searchScope()).isEqualTo("PROJECT");
        assertThat(response.run().ragQueryId()).isEqualTo(queryId);
        assertThat(response.assistantMessage().content()).contains("market-access");
        assertThat(response.assistantMessage().citations()).hasSize(1);
    }

    @Test
    void selectedSourceProjectQuestionRejectsUnauthorizedDocumentIds() {
        ResearchProject project = project(workspace, "Project");
        UUID unauthorizedDocumentId = UUID.randomUUID();
        when(projectAuthorizationService.requireProjectViewer(project.getId(), user))
                .thenReturn(new ProjectAuthorizationContext(project, null, Optional.empty()));
        when(ragQueryService.submitProjectConversationTurn(eq(project.getId()), eq(user), any(), any()))
                .thenThrow(new InvalidDocumentOperationException("One or more selected documents are not available for retrieval."));

        assertThatThrownBy(() -> service.createAndSubmitProject(
                project.getId(),
                user,
                "Use this source only.",
                RetrievalScopeType.SELECTED_DOCUMENTS,
                Set.of(unauthorizedDocumentId),
                null
        )).isInstanceOf(InvalidDocumentOperationException.class);
    }

    @Test
    void movesGeneralConversationToAuthorizedProjectWithoutDuplicatingMessages() {
        Conversation conversation = existingConversation(user, "General question");
        addMessage(conversation, ConversationMessageRole.USER, "Original question", 1);
        ResearchProject project = project(workspace, "Moved Project");
        when(projectAuthorizationService.requireProjectViewer(project.getId(), user))
                .thenReturn(new ProjectAuthorizationContext(project, null, Optional.empty()));

        var moved = service.moveToProject(conversation.getId(), user, project.getId());

        assertThat(moved.type()).isEqualTo(ConversationType.PROJECT);
        assertThat(moved.projectId()).isEqualTo(project.getId());
        assertThat(savedMessages()).hasSize(1);
        assertThat(savedMessages().getFirst().getContent()).isEqualTo("Original question");
    }

    @Test
    void projectScopedDetailRejectsConversationFromAnotherProject() {
        ResearchProject projectA = project(workspace, "Project A");
        ResearchProject projectB = project(workspace, "Project B");
        Conversation conversation = existingProjectConversation(user, projectA, "Project A chat");

        assertThatThrownBy(() -> service.detailProject(projectB.getId(), conversation.getId(), user))
                .isInstanceOf(ConversationAccessDeniedException.class);
    }

    @Test
    void projectScopedSubmitRejectsConversationFromAnotherProjectBeforeAppendingMessage() {
        ResearchProject projectA = project(workspace, "Project A");
        ResearchProject projectB = project(workspace, "Project B");
        Conversation conversation = existingProjectConversation(user, projectA, "Project A chat");
        addMessage(conversation, ConversationMessageRole.USER, "Existing question", 1);

        assertThatThrownBy(() -> service.submitProject(
                projectB.getId(),
                conversation.getId(),
                user,
                "This must not be appended.",
                RetrievalScopeType.PROJECT_ALL_DOCUMENTS,
                null,
                null
        )).isInstanceOf(ConversationAccessDeniedException.class);

        assertThat(savedMessages()).extracting(ConversationMessage::getContent)
                .containsExactly("Existing question");
    }

    private void wireConversationRepository() {
        when(conversationRepository.save(any(Conversation.class))).thenAnswer(invocation -> {
            Conversation conversation = invocation.getArgument(0);
            if (conversation.getId() == null) {
                conversation.setId(UUID.randomUUID());
            }
            if (conversation.getCreatedAt() == null) {
                conversation.setCreatedAt(OffsetDateTime.now());
            }
            if (conversation.getUpdatedAt() == null) {
                conversation.setUpdatedAt(OffsetDateTime.now());
            }
            conversations.put(conversation.getId(), conversation);
            return conversation;
        });
        when(conversationRepository.findByIdAndUserId(any(), any())).thenAnswer(invocation -> {
            UUID id = invocation.getArgument(0);
            UUID userId = invocation.getArgument(1);
            Conversation conversation = conversations.get(id);
            return conversation != null && conversation.getUser().getId().equals(userId)
                    ? Optional.of(conversation)
                    : Optional.empty();
        });
    }

    private void wireMessageRepository() {
        when(messageRepository.save(any(ConversationMessage.class))).thenAnswer(invocation -> {
            ConversationMessage message = invocation.getArgument(0);
            if (message.getId() == null) {
                message.setId(UUID.randomUUID());
            }
            if (message.getCreatedAt() == null) {
                message.setCreatedAt(OffsetDateTime.now());
            }
            messages.put(message.getId(), message);
            return message;
        });
        when(messageRepository.maxSequenceForConversation(any())).thenAnswer(invocation -> {
            UUID conversationId = invocation.getArgument(0);
            return messages.values().stream()
                    .filter(message -> message.getConversation().getId().equals(conversationId))
                    .mapToInt(ConversationMessage::getSequenceNumber)
                    .max()
                    .orElse(0);
        });
        when(messageRepository.findAllByConversationIdAndRoleNotOrderBySequenceNumberAsc(any(), any())).thenAnswer(invocation -> {
            UUID conversationId = invocation.getArgument(0);
            ConversationMessageRole excluded = invocation.getArgument(1);
            return messagesFor(conversationId, excluded, true);
        });
        when(messageRepository.findAllByConversationIdAndRoleNotOrderBySequenceNumberDesc(any(), any(), any())).thenAnswer(invocation -> {
            UUID conversationId = invocation.getArgument(0);
            ConversationMessageRole excluded = invocation.getArgument(1);
            return messagesFor(conversationId, excluded, false);
        });
        when(messageRepository.findByIdAndConversationIdAndRole(any(), any(), any())).thenAnswer(invocation -> {
            UUID id = invocation.getArgument(0);
            UUID conversationId = invocation.getArgument(1);
            ConversationMessageRole role = invocation.getArgument(2);
            ConversationMessage message = messages.get(id);
            return message != null
                    && message.getConversation().getId().equals(conversationId)
                    && message.getRole() == role
                    ? Optional.of(message)
                    : Optional.empty();
        });
    }

    private void wireRunRepository() {
        when(runRepository.save(any(ConversationRun.class))).thenAnswer(invocation -> {
            ConversationRun run = invocation.getArgument(0);
            if (run.getId() == null) {
                run.setId(UUID.randomUUID());
            }
            if (run.getCreatedAt() == null) {
                run.setCreatedAt(OffsetDateTime.now());
            }
            if (run.getUpdatedAt() == null) {
                run.setUpdatedAt(OffsetDateTime.now());
            }
            runs.put(run.getId(), run);
            return run;
        });
        when(runRepository.findFirstByConversationIdOrderByCreatedAtDesc(any())).thenAnswer(invocation -> {
            UUID conversationId = invocation.getArgument(0);
            return runs.values().stream()
                    .filter(run -> run.getConversation().getId().equals(conversationId))
                    .max(Comparator.comparing(ConversationRun::getCreatedAt));
        });
        when(runRepository.findAllByConversationIdAndAssistantMessageIsNotNull(any())).thenAnswer(invocation -> {
            UUID conversationId = invocation.getArgument(0);
            return runs.values().stream()
                    .filter(run -> run.getConversation().getId().equals(conversationId))
                    .filter(run -> run.getAssistantMessage() != null)
                    .toList();
        });
        when(runRepository.findFirstByConversationIdAndUserMessageIdOrderByCreatedAtDesc(any(), any())).thenAnswer(invocation -> {
            UUID conversationId = invocation.getArgument(0);
            UUID userMessageId = invocation.getArgument(1);
            return runs.values().stream()
                    .filter(run -> run.getConversation().getId().equals(conversationId))
                    .filter(run -> run.getUserMessage().getId().equals(userMessageId))
                    .max(Comparator.comparing(ConversationRun::getCreatedAt));
        });
    }

    private void wireSourceRepository() {
        when(sourceRepository.save(any(ConversationSource.class))).thenAnswer(invocation -> {
            ConversationSource source = invocation.getArgument(0);
            if (source.getId() == null) {
                source.setId(UUID.randomUUID());
            }
            if (source.getCreatedAt() == null) {
                source.setCreatedAt(OffsetDateTime.now());
            }
            if (source.getRetrievedAt() == null) {
                source.setRetrievedAt(OffsetDateTime.now());
            }
            sources.put(source.getId(), source);
            return source;
        });
        when(sourceRepository.findAllByMessageIdOrderBySourceOrdinalAscCreatedAtAsc(any())).thenAnswer(invocation -> {
            UUID messageId = invocation.getArgument(0);
            return sources.values().stream()
                    .filter(source -> source.getMessage().getId().equals(messageId))
                    .sorted(Comparator
                            .comparing((ConversationSource source) -> source.getSourceOrdinal() == null ? Integer.MAX_VALUE : source.getSourceOrdinal())
                            .thenComparing(source -> source.getCreatedAt() == null ? OffsetDateTime.MIN : source.getCreatedAt()))
                    .toList();
        });
    }

    private void wireMessageCitationRepository() {
        when(messageCitationRepository.save(any(MessageCitation.class))).thenAnswer(invocation -> {
            MessageCitation citation = invocation.getArgument(0);
            if (citation.getId() == null) {
                citation.setId(UUID.randomUUID());
            }
            if (citation.getCreatedAt() == null) {
                citation.setCreatedAt(OffsetDateTime.now());
            }
            citations.put(citation.getId(), citation);
            return citation;
        });
        when(messageCitationRepository.findAllByMessageIdOrderByCitationOrdinalAsc(any())).thenAnswer(invocation -> {
            UUID messageId = invocation.getArgument(0);
            return citations.values().stream()
                    .filter(citation -> citation.getMessage().getId().equals(messageId))
                    .sorted(Comparator.comparingInt(MessageCitation::getCitationOrdinal))
                    .toList();
        });
    }

    private List<ConversationMessage> messagesFor(UUID conversationId, ConversationMessageRole excluded, boolean ascending) {
        Comparator<ConversationMessage> comparator = Comparator.comparingInt(ConversationMessage::getSequenceNumber);
        if (!ascending) {
            comparator = comparator.reversed();
        }
        return messages.values().stream()
                .filter(message -> message.getConversation().getId().equals(conversationId))
                .filter(message -> message.getRole() != excluded)
                .sorted(comparator)
                .toList();
    }

    private List<ConversationMessage> savedMessages() {
        return messages.values().stream()
                .sorted(Comparator.comparingInt(ConversationMessage::getSequenceNumber))
                .toList();
    }

    private Conversation existingConversation(User owner, String title) {
        Conversation conversation = new Conversation();
        conversation.setId(UUID.randomUUID());
        conversation.setUser(owner);
        conversation.setWorkspace(workspace);
        conversation.setTitle(title);
        conversation.setType(ConversationType.GENERAL);
        conversation.setStatus(ConversationStatus.ACTIVE);
        conversation.setCreatedAt(OffsetDateTime.now());
        conversation.setUpdatedAt(OffsetDateTime.now());
        conversations.put(conversation.getId(), conversation);
        return conversation;
    }

    private Conversation existingProjectConversation(User owner, ResearchProject project, String title) {
        Conversation conversation = existingConversation(owner, title);
        conversation.setWorkspace(project.getWorkspace());
        conversation.setProject(project);
        conversation.setType(ConversationType.PROJECT);
        return conversation;
    }

    private ConversationMessage addMessage(Conversation conversation, ConversationMessageRole role, String content, int sequence) {
        ConversationMessage message = new ConversationMessage();
        message.setId(UUID.randomUUID());
        message.setConversation(conversation);
        message.setRole(role);
        message.setContent(content);
        message.setSequenceNumber(sequence);
        message.setCreatedAt(OffsetDateTime.now());
        messages.put(message.getId(), message);
        return message;
    }

    private User user(String email) {
        User u = new User();
        u.setId(UUID.randomUUID());
        u.setEmail(email);
        return u;
    }

    private Workspace workspace(User owner) {
        Workspace ws = new Workspace();
        ws.setId(UUID.randomUUID());
        ws.setName("My Workspace");
        ws.setOwner(owner);
        ws.setType(WorkspaceType.PERSONAL);
        ws.setStatus(WorkspaceStatus.ACTIVE);
        return ws;
    }

    private ResearchProject project(Workspace workspace, String title) {
        ResearchProject project = new ResearchProject();
        project.setId(UUID.randomUUID());
        project.setWorkspace(workspace);
        project.setTitle(title);
        project.setCreatedBy(user);
        return project;
    }

    private AiProperties aiProperties() {
        return new AiProperties(
                new AiProperties.Generation(true, AiProviderType.OPENAI, "gpt-4o-mini", 0.2, 1024, Duration.ofSeconds(60)),
                new AiProperties.Embedding(false, AiProviderType.NONE, "", null, 64, Duration.ofSeconds(60)),
                new AiProperties.Retrieval(true, false),
                new AiProperties.Privacy(true),
                new AiProperties.Context(12, 30_000, 6, 1024),
                new AiProperties.RateLimit(30, 120, 2, 10)
        );
    }

    @SuppressWarnings("unchecked")
    private <T> ObjectProvider<T> emptyProvider() {
        ObjectProvider<T> provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(null);
        return provider;
    }

    @SuppressWarnings("unchecked")
    private <T> ObjectProvider<T> provider(T value) {
        ObjectProvider<T> provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(value);
        return provider;
    }
}
