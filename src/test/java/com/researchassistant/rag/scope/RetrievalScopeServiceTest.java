package com.researchassistant.rag.scope;

import com.researchassistant.document.exception.InvalidDocumentOperationException;
import com.researchassistant.identity.entity.User;
import com.researchassistant.project.entity.ResearchProject;
import com.researchassistant.project.service.ProjectAuthorizationContext;
import com.researchassistant.project.service.ProjectAuthorizationService;
import com.researchassistant.workspace.entity.Workspace;
import com.researchassistant.workspace.entity.WorkspaceStatus;
import com.researchassistant.workspace.entity.WorkspaceType;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RetrievalScopeServiceTest {

    private ProjectAuthorizationService projectAuthorizationService;
    private RetrievalScopeRepository scopeRepository;
    private RetrievalScopeService service;
    private User user;
    private ResearchProject project;

    @BeforeEach
    void setUp() {
        projectAuthorizationService = mock(ProjectAuthorizationService.class);
        scopeRepository = mock(RetrievalScopeRepository.class);
        service = new RetrievalScopeService(projectAuthorizationService, scopeRepository);

        user = new User();
        user.setId(UUID.randomUUID());
        user.setEmail("researcher@example.com");

        Workspace workspace = new Workspace();
        workspace.setId(UUID.randomUUID());
        workspace.setName("Workspace");
        workspace.setOwner(user);
        workspace.setType(WorkspaceType.PERSONAL);
        workspace.setStatus(WorkspaceStatus.ACTIVE);

        project = new ResearchProject();
        project.setId(UUID.randomUUID());
        project.setWorkspace(workspace);
        project.setTitle("Farmer-to-Buyer Agricultural Marketplace");
        project.setCreatedBy(user);

        when(projectAuthorizationService.requireProjectViewer(project.getId(), user))
                .thenReturn(new ProjectAuthorizationContext(project, null, Optional.empty()));
    }

    @Test
    void resolvesSelectedReadyAuthorizedSourcesWithCurrentVersions() {
        UUID documentOne = UUID.randomUUID();
        UUID documentTwo = UUID.randomUUID();
        UUID versionOne = UUID.randomUUID();
        UUID versionTwo = UUID.randomUUID();
        when(scopeRepository.findEligibleSelectedDocuments(project.getId(), Set.of(documentOne, documentTwo)))
                .thenReturn(List.of(
                        new RetrievalScopeDocument(documentOne, versionOne),
                        new RetrievalScopeDocument(documentTwo, versionTwo)
                ));

        RetrievalScope scope = service.resolve(
                user,
                project.getId(),
                RetrievalScopeType.SELECTED_DOCUMENTS,
                Set.of(documentOne, documentTwo)
        );

        assertThat(scope.scopeType()).isEqualTo(RetrievalScopeType.SELECTED_DOCUMENTS);
        assertThat(scope.authorizedDocumentIds()).containsExactly(documentOne, documentTwo);
        assertThat(scope.authorizedDocumentVersionIds()).containsExactly(versionOne, versionTwo);
    }

    @Test
    void rejectsSelectedSourceIdsThatDoNotResolveAsEligibleProjectSources() {
        UUID eligibleDocument = UUID.randomUUID();
        UUID rejectedDocument = UUID.randomUUID();
        UUID version = UUID.randomUUID();
        when(scopeRepository.findEligibleSelectedDocuments(project.getId(), Set.of(eligibleDocument, rejectedDocument)))
                .thenReturn(List.of(new RetrievalScopeDocument(eligibleDocument, version)));

        assertThatThrownBy(() -> service.resolve(
                user,
                project.getId(),
                RetrievalScopeType.SELECTED_DOCUMENTS,
                Set.of(eligibleDocument, rejectedDocument)
        )).isInstanceOf(InvalidDocumentOperationException.class);
    }
}
