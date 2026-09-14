package com.researchassistant.retrieval;

import com.researchassistant.document.embedding.DisabledDocumentEmbeddingProvider;
import com.researchassistant.document.embedding.EmbeddingProperties;
import com.researchassistant.document.entity.Document;
import com.researchassistant.document.entity.DocumentStatus;
import com.researchassistant.document.repository.DocumentRepository;
import com.researchassistant.identity.entity.User;
import com.researchassistant.project.entity.ResearchProject;
import com.researchassistant.project.service.ProjectAuthorizationContext;
import com.researchassistant.project.service.ProjectAuthorizationService;
import com.researchassistant.retrieval.dto.DocumentRetrievalMode;
import com.researchassistant.retrieval.dto.DocumentRetrievalRequest;
import com.researchassistant.retrieval.dto.RetrievalSearchResponse;
import com.researchassistant.retrieval.repository.DocumentRetrievalRepository;
import com.researchassistant.retrieval.repository.RetrievalCandidateRow;
import com.researchassistant.retrieval.rerank.NoOpEvidenceReranker;
import com.researchassistant.retrieval.service.HybridDocumentRetrievalService;
import com.researchassistant.retrieval.service.RetrievalProperties;
import com.researchassistant.workspace.entity.Workspace;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class HybridDocumentRetrievalServiceTests {

    @Test
    void authorizesProjectBeforeRetrievalRepositoryIsCalled() {
        ProjectAuthorizationService authorizationService =
                mock(ProjectAuthorizationService.class);
        DocumentRetrievalRepository retrievalRepository =
                mock(DocumentRetrievalRepository.class);
        HybridDocumentRetrievalService service =
                service(authorizationService, mock(DocumentRepository.class), retrievalRepository);
        UUID projectId = UUID.randomUUID();
        User user = new User();

        assertThatThrownBy(() -> service.retrieve(
                user,
                request(projectId, null, DocumentRetrievalMode.PROJECT_ALL_DOCUMENTS)
        )).isInstanceOf(RuntimeException.class);

        verify(authorizationService).requireProjectViewer(projectId, user);
        verify(retrievalRepository, never()).lexical(any(), any(), any(), anyInt());
    }

    @Test
    void lexicalOnlyFallbackReturnsCandidatesWhenEmbeddingsDisabled() {
        ProjectAuthorizationService authorizationService =
                mock(ProjectAuthorizationService.class);
        DocumentRetrievalRepository retrievalRepository =
                mock(DocumentRetrievalRepository.class);
        UUID workspaceId = UUID.randomUUID();
        UUID projectId = UUID.randomUUID();
        when(authorizationService.requireProjectViewer(any(), any()))
                .thenReturn(projectContext(workspaceId, projectId));
        when(retrievalRepository.lexical(any(), any(), any(), anyInt()))
                .thenReturn(List.of(row(1.0)));

        RetrievalSearchResponse response = service(
                authorizationService,
                mock(DocumentRepository.class),
                retrievalRepository
        ).retrieve(
                new User(),
                request(projectId, null, DocumentRetrievalMode.PROJECT_ALL_DOCUMENTS)
        );

        assertThat(response.lexicalUsed()).isTrue();
        assertThat(response.semanticUsed()).isFalse();
        assertThat(response.candidates()).singleElement().satisfies(candidate -> {
            assertThat(candidate.documentCode()).isEqualTo("DOC-001");
            assertThat(candidate.pageNumber()).isEqualTo(1);
            assertThat(candidate.lexicalScore()).isEqualTo(1.0);
            assertThat(candidate.semanticScore()).isNull();
        });
    }

    @Test
    void selectedDocumentsMustBelongToAuthorizedProjectAndNotBeArchived() {
        ProjectAuthorizationService authorizationService =
                mock(ProjectAuthorizationService.class);
        DocumentRepository documentRepository = mock(DocumentRepository.class);
        UUID workspaceId = UUID.randomUUID();
        UUID projectId = UUID.randomUUID();
        UUID otherProjectId = UUID.randomUUID();
        UUID selectedId = UUID.randomUUID();
        when(authorizationService.requireProjectViewer(any(), any()))
                .thenReturn(projectContext(workspaceId, projectId));
        Document selected = document(selectedId, otherProjectId, DocumentStatus.READY);
        when(documentRepository.findAllById(Set.of(selectedId)))
                .thenReturn(List.of(selected));

        assertThatThrownBy(() -> service(
                authorizationService,
                documentRepository,
                mock(DocumentRetrievalRepository.class)
        ).retrieve(
                new User(),
                request(projectId, Set.of(selectedId), DocumentRetrievalMode.SELECTED_DOCUMENTS)
        )).isInstanceOf(RuntimeException.class);
    }

    private HybridDocumentRetrievalService service(
            ProjectAuthorizationService authorizationService,
            DocumentRepository documentRepository,
            DocumentRetrievalRepository retrievalRepository
    ) {
        EmbeddingProperties embeddingProperties =
                new EmbeddingProperties(false, "none", "", "", 32, java.time.Duration.ofSeconds(60));
        return new HybridDocumentRetrievalService(
                authorizationService,
                documentRepository,
                retrievalRepository,
                new DisabledDocumentEmbeddingProvider(embeddingProperties),
                embeddingProperties,
                new RetrievalProperties(40, 40, 60, 30, 10, 50, 60),
                new NoOpEvidenceReranker()
        );
    }

    private DocumentRetrievalRequest request(
            UUID projectId,
            Set<UUID> documentIds,
            DocumentRetrievalMode mode
    ) {
        return new DocumentRetrievalRequest(
                null,
                projectId,
                documentIds,
                "alpha",
                10,
                true,
                true,
                mode
        );
    }

    private ProjectAuthorizationContext projectContext(
            UUID workspaceId,
            UUID projectId
    ) {
        Workspace workspace = new Workspace();
        workspace.setId(workspaceId);
        ResearchProject project = new ResearchProject();
        project.setId(projectId);
        project.setWorkspace(workspace);
        return new ProjectAuthorizationContext(project, null, Optional.empty());
    }

    private Document document(
            UUID documentId,
            UUID projectId,
            DocumentStatus status
    ) {
        ResearchProject project = new ResearchProject();
        project.setId(projectId);
        Document document = new Document();
        document.setId(documentId);
        document.setProject(project);
        document.setStatus(status);
        return document;
    }

    private RetrievalCandidateRow row(double score) {
        return new RetrievalCandidateRow(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                "DOC-001",
                UUID.randomUUID(),
                1,
                1,
                1,
                "alpha evidence",
                score,
                "Document"
        );
    }
}
